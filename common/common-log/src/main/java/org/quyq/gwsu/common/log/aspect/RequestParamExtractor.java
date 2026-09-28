package org.quyq.gwsu.common.log.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请求参数提取器，使用 Jackson 3 实现参数序列化与过滤
 * <p>
 * 替代原 Fastjson 的 PropertyFilter + JSONObject 实现，
 * 通过 ValueSerializerModifier 在序列化时过滤掉 MultipartFile、HttpServletRequest 等不需要记录的属性类型
 * </p>
 *
 * @author Quyq
 */
public class RequestParamExtractor {

    private static final String QUERY_KEY = "query";

    private static final String PATH_KEY = "path";

    private static final String BODY_KEY = "body";

    private final ObjectMapper filteringMapper;

    public RequestParamExtractor() {
        SimpleModule filterModule = new SimpleModule("requestParamFilter");
        filterModule.setSerializerModifier(new FilterValueSerializerModifier());

        this.filteringMapper = JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .addModule(filterModule)
                .build();
    }

    /**
     * 按 HTTP 参数来源提取请求参数。
     *
     * @param request    HTTP请求
     * @param invocation 方法调用信息
     * @return 固定包含 query、path、body 的 JsonNode
     */
    public JsonNode getRequestParam(HttpServletRequest request, MethodInvocation invocation) {
        ObjectNode allParams = filteringMapper.createObjectNode();
        allParams.set(QUERY_KEY, extractQueryParams(request));
        allParams.set(PATH_KEY, extractPathParams(request));
        allParams.set(BODY_KEY, extractRequestBody(invocation));

        return allParams;
    }

    private ObjectNode extractQueryParams(HttpServletRequest request) {
        ObjectNode query = filteringMapper.createObjectNode();
        request.getParameterMap().forEach((name, values) -> {
            if (values == null || values.length == 0) {
                query.putNull(name);
            } else if (values.length == 1) {
                query.put(name, values[0]);
            } else {
                query.set(name, filteringMapper.valueToTree(values));
            }
        });
        return query;
    }

    private ObjectNode extractPathParams(HttpServletRequest request) {
        ObjectNode path = filteringMapper.createObjectNode();
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attribute instanceof Map<?, ?> variables) {
            variables.forEach((name, value) -> path.set(
                    String.valueOf(name),
                    serializeValue(value)));
        }
        return path;
    }

    private JsonNode extractRequestBody(MethodInvocation invocation) {
        Parameter[] parameters = invocation.getMethod().getParameters();
        Object[] arguments = invocation.getArguments();
        for (int i = 0; i < parameters.length && i < arguments.length; i++) {
            if (!parameters[i].isAnnotationPresent(RequestBody.class)) {
                continue;
            }
            Object argument = arguments[i];
            if (argument == null || isFilterObject(argument)) {
                return filteringMapper.createObjectNode();
            }
            return serializeValue(argument);
        }
        return filteringMapper.createObjectNode();
    }

    private JsonNode serializeValue(Object value) {
        if (value == null) {
            return filteringMapper.nullNode();
        }
        try {
            return filteringMapper.valueToTree(value);
        } catch (Exception ex) {
            return filteringMapper.valueToTree(String.valueOf(value));
        }
    }

    public boolean isEmpty(JsonNode requestParam) {
        if (requestParam == null || requestParam.isNull()) {
            return true;
        }
        Iterator<JsonNode> values = requestParam.iterator();
        while (values.hasNext()) {
            JsonNode value = values.next();
            if (value != null && !value.isNull() && !value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断对象是否为需要过滤的类型（不参与参数序列化）
     *
     * @param o 待检查对象
     * @return 是否需要过滤
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) o;
            Iterator<?> iter = collection.iterator();
            return !collection.isEmpty() && iter.next() instanceof MultipartFile;
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) o;
            if (map.isEmpty()) {
                return false;
            }
            Iterator<?> iter = map.entrySet().iterator();
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            return entry.getValue() instanceof MultipartFile;
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }

    /**
     * 值序列化修改器，在序列化时过滤掉不需要的属性类型
     * <p>
     * 等效于 Fastjson 的 PropertyFilter 功能，通过检查属性的声明类型，
     * 移除 MultipartFile、HttpServletRequest、HttpServletResponse、BindingResult 及其集合/数组/Map形式
     * </p>
     */
    private static class FilterValueSerializerModifier extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()
                    .filter(pw -> !isFilterType(pw.getType()))
                    .collect(Collectors.toList());
        }

        /**
         * 判断属性类型是否需要过滤
         */
        private boolean isFilterType(JavaType type) {
            Class<?> rawClass = type.getRawClass();

            // 直接匹配过滤类型
            if (MultipartFile.class.isAssignableFrom(rawClass)
                    || HttpServletRequest.class.isAssignableFrom(rawClass)
                    || HttpServletResponse.class.isAssignableFrom(rawClass)
                    || BindingResult.class.isAssignableFrom(rawClass)) {
                return true;
            }

            // 数组元素为过滤类型（如 MultipartFile[]）
            if (type.isArrayType()) {
                JavaType contentType = type.getContentType();
                return contentType != null && MultipartFile.class.isAssignableFrom(contentType.getRawClass());
            }

            // 集合元素为过滤类型（如 List<MultipartFile>）
            if (type.isCollectionLikeType()) {
                JavaType contentType = type.getContentType();
                return contentType != null && MultipartFile.class.isAssignableFrom(contentType.getRawClass());
            }

            // Map值为过滤类型（如 Map<String, MultipartFile>）
            if (type.isMapLikeType()) {
                JavaType valueType = type.getContentType();
                return valueType != null && MultipartFile.class.isAssignableFrom(valueType.getRawClass());
            }

            return false;
        }
    }

}

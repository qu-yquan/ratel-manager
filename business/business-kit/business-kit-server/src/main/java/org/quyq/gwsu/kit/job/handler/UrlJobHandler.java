package org.quyq.gwsu.kit.job.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.func.LambdaUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.handler.annotation.XxlJob;
import org.quyq.gwsu.kit.api.job.vo.UrlHandlerParam;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * URL调用模式定时任务Handler
 * <p>
 * 分布式模式：通过模块前缀查找服务名 → DiscoveryClient获取实例 → WebClient HTTP调用
 * 单应用模式：通过 RequestMappingHandlerMapping 查找HandlerMethod → 直接反射调用Controller方法
 * <p>
 * 两种模式均不触发权限校验，行为一致
 *
 * @author Quyq
 */
@Slf4j
@Component
public class UrlJobHandler implements ApplicationRunner {

    private static final String HANDLER_NAME = "urlJobHandler";

    private static final Duration READ_TIMEOUT = Duration.ofHours(6);

    private final RequestMappingHandlerMapping handlerMapping;
    private final ProjectUtils projectUtils;
    private final CacheUtils cacheUtils;
    private final WebClient webClient;
    private final Gson gson;

    /**
     * 通用类型转换器（路径变量值 String → 目标类型）
     */
    private static final SimpleTypeConverter TYPE_CONVERTER = new SimpleTypeConverter();

    /**
     * 单应用模式下的URL映射表
     * key: 完整请求路径（如 "/kit/job/api/callback"）
     * value: HandlerMethodWrapper
     */
    private volatile Map<String, HandlerMethodWrapper> localMethodRegistry = Map.of();

    /**
     * url调用路由选择标记前缀
     */
    private static final String ROUTER_KEY_PREFIX = "job:router-select-index:";

    public UrlJobHandler(RequestMappingHandlerMapping requestMappingHandlerMapping,
                         ProjectUtils projectUtils,
                         CacheUtils cacheUtils,
                         WebClient.Builder webClientBuilder,
                         Gson gson) {
        this.handlerMapping = requestMappingHandlerMapping;
        this.projectUtils = projectUtils;
        this.cacheUtils = cacheUtils;
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.gson = gson;
    }

    // ==================== 启动时构建映射表 ====================

    @Override
    public void run(ApplicationArguments args) {
        if (!DeployUtils.isSingle()) {
            log.info("URL Job Handler: 分布式部署模式，跳过本地方法映射表构建");
            return;
        }

        // 构建映射表
        Map<String, HandlerMethodWrapper> built = new HashMap<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // 跳过非项目内的接口
            String packageName = handlerMethod.getBeanType().getPackageName();
            if (!packageName.startsWith(CoreConstants.Project.COMMON_PACKAGE)) {
                continue;
            }

            // 获取路径模式
            Set<PathPattern> patterns = Optional.ofNullable(mappingInfo.getPathPatternsCondition())
                    .map(cond -> cond.getPatterns())
                    .orElseGet(Collections::emptySet);
            if (patterns.isEmpty()) {
                continue;
            }

            // 获取请求方法（仅收录POST和无限制的接口）
            Set<org.springframework.web.bind.annotation.RequestMethod> methods =
                    mappingInfo.getMethodsCondition().getMethods();
            if (!methods.isEmpty() && !methods.contains(org.springframework.web.bind.annotation.RequestMethod.POST)) {
                continue;
            }

            // 注册到映射表，key 就是完整路径
            HandlerMethod resolved = handlerMethod.createWithResolvedBean();
            for (PathPattern pattern : patterns) {
                built.put(pattern.getPatternString(), new HandlerMethodWrapper(resolved, pattern));
            }
        }

        this.localMethodRegistry = Collections.unmodifiableMap(built);
        log.info("URL Job Handler: 单应用模式，本地方法映射表构建完成，共 {} 条记录", localMethodRegistry.size());
    }

    // ==================== Handler 入口 ====================

    @XxlJob(HANDLER_NAME)
    public void urlJobHandler() {
        String param = XxlJobHelper.getJobParam();
        if (!StringUtils.hasText(param)) {
            XxlJobHelper.handleFail("参数为空，处理失败");
            return;
        }

        String jobId = XxlJobHelper.getJobId();
        log.info("URL调用模式 定时任务触发, 任务ID:{}, 参数：{}", jobId, param);

        try {
            UrlHandlerParam paramObj = gson.fromJson(param, UrlHandlerParam.class);
            if (!StringUtils.hasText(paramObj.prefix())) {
                XxlJobHelper.handleFail("参数配置错误：模块前缀(prefix)不能为空");
                return;
            }
            if (!StringUtils.hasText(paramObj.url())) {
                XxlJobHelper.handleFail("参数配置错误：接口路径(url)不能为空");
                return;
            }

            if (DeployUtils.isSingle()) {
                invokeLocal(paramObj);
            } else {
                invokeRemote(paramObj);
            }

        } catch (Exception e) {
            log.error("URL调用模式 定时任务执行异常, 任务ID:{}", jobId, e);
            XxlJobHelper.log(e);
            XxlJobHelper.handleFail(e.getMessage());
            return;
        }

        XxlJobHelper.handleSuccess();
    }

    // ==================== 构造任务上下文 ====================

    /**
     * 构建任务执行参数
     */
    private BaseDTO.JobParams buildJobParams() {
        return new BaseDTO.JobParams(
                XxlJobHelper.getShardTotal(),
                XxlJobHelper.getShardIndex(),
                XxlJobHelper.getJobId(),
                XxlJobHelper.getLogId()
        );
    }

    /**
     * 构造请求体 JSON（合并业务参数 + 任务上下文）
     */
    private JsonObject buildRequestBody(UrlHandlerParam paramObj) {
        JsonObject body = new JsonObject();
        if (StringUtils.hasText(paramObj.bodyJson())) {
            try {
                JsonObject parsed = JsonParser.parseString(paramObj.bodyJson()).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : parsed.entrySet()) {
                    body.add(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("bodyJson 格式错误，请检查参数配置: " + e.getMessage(), e);
            }
        }
        // 注入任务上下文到 jobParams 字段
        body.add(LambdaUtil.getFieldName(BaseDTO::getJobParams), gson.toJsonTree(buildJobParams()));
        return body;
    }

    // ==================== 分布式模式：WebClient HTTP 调用 ====================

    private void invokeRemote(UrlHandlerParam paramObj) throws Exception {
        // 1. 通过模块前缀查找服务名
        var moduleMapping = DeployUtils.getDistributedServerModuleMapping();
        var serverInfo = moduleMapping.get(paramObj.prefix());
        String serviceName = serverInfo == null ? null : serverInfo.applicationName();
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalStateException("找不到模块前缀对应的服务: " + paramObj.prefix());
        }

        // 2. 获取服务实例（延迟获取：单应用模式下无 DiscoveryClient Bean，不可构造器注入）
        DiscoveryClient discoveryClient = SpringUtils.getBean(DiscoveryClient.class);
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (CollUtil.isEmpty(instances)) {
            throw new IllegalStateException("调用服务实例为空，请检查 " + serviceName + " 服务状态");
        }

        // 3. Redis 轮询选择实例
        String redisKey = projectUtils.getServerPrefix() + ROUTER_KEY_PREFIX + serviceName;
        Long incr = cacheUtils.increment(redisKey);
        // 仅首次创建时设置过期时间
        if (incr != null && incr == 1) {
            cacheUtils.expire(redisKey, 12, TimeUnit.HOURS);
        }
        int index = Math.floorMod(incr, instances.size());
        String domain = instances.get(index).getUri().toString();

        // 4. 构造请求体
        JsonObject body = buildRequestBody(paramObj);

        // 5. WebClient 发起 HTTP 请求
        String fullUrl = domain + paramObj.url();
        log.info("URL调用模式 分布式调用, 目标:{}", fullUrl);

        String result = webClient.post()
                .uri(fullUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(respStr -> new IllegalStateException(
                                        "响应状态异常：" + response.statusCode() + "，响应体：" + respStr))
                )
                .bodyToMono(String.class)
                .timeout(READ_TIMEOUT)
                .block(READ_TIMEOUT.plus(Duration.ofSeconds(30)));

        XxlJobHelper.log("执行结果：\n {}", result);
        checkBusinessResult(result);
    }

    // ==================== 单应用模式：本地方法调用 ====================

    private void invokeLocal(UrlHandlerParam paramObj) throws Exception {
        // 拼接完整路径作为查找key: /{prefix}{url}
        String lookupPath = "/" + paramObj.prefix() + paramObj.url();
        PathContainer pathContainer = PathContainer.parsePath(lookupPath);

        // 1. 精确匹配
        HandlerMethodWrapper wrapper = localMethodRegistry.get(lookupPath);

        // 2. 路径变量匹配（如 /job/log/{id}）
        Map<String, String> pathVariables = Map.of();
        if (wrapper == null) {
            for (HandlerMethodWrapper candidate : localMethodRegistry.values()) {
                if (candidate.pathPattern.matches(pathContainer)) {
                    wrapper = candidate;
                    PathPattern.PathMatchInfo matchInfo = candidate.pathPattern.matchAndExtract(pathContainer);
                    if (matchInfo != null) {
                        pathVariables = matchInfo.getUriVariables();
                    }
                    break;
                }
            }
        }

        if (wrapper == null) {
            throw new IllegalStateException("未找到接口注册: " + lookupPath);
        }

        HandlerMethod handlerMethod = wrapper.handlerMethod;
        Object bean = handlerMethod.getBean();
        Method method = handlerMethod.getMethod();

        // 构造请求体 JSON
        JsonObject body = buildRequestBody(paramObj);

        // 解析方法参数
        Object[] args = resolveMethodArguments(method, body, pathVariables);

        log.info("URL调用模式 单应用调用, 目标:{}.{}", bean.getClass().getSimpleName(), method.getName());

        // 反射调用，解包 InvocationTargetException 以获取真实异常信息
        Object result;
        try {
            result = method.invoke(bean, args);
        } catch (InvocationTargetException e) {
            throw (Exception) (e.getCause() != null ? e.getCause() : e);
        }

        // 处理返回值
        handleLocalResult(result);
    }

    // ==================== 参数解析 ====================

    /**
     * 解析方法参数，将 JSON body 和路径变量映射到方法参数
     */
    private Object[] resolveMethodArguments(Method method, JsonObject body, Map<String, String> pathVariables) {
        int paramCount = method.getParameterCount();
        if (paramCount == 0) {
            return new Object[0];
        }

        java.lang.reflect.Parameter[] parameters = method.getParameters();

        Object[] args = new Object[paramCount];
        for (int i = 0; i < paramCount; i++) {
            java.lang.reflect.Parameter param = parameters[i];

            // @PathVariable 参数：从路径变量中取值
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String varName = StringUtils.hasText(pathVariable.value()) ? pathVariable.value() : param.getName();
                String varValue = pathVariables.get(varName);
                args[i] = convertType(varValue, param.getType());
                continue;
            }

            // @RequestBody 参数：将整个 body 反序列化为参数类型
            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                args[i] = gson.fromJson(body, param.getType());
                continue;
            }

            // 其他参数（@RequestParam 等）：从 body 中按参数名取值
            JsonElement element = body.get(param.getName());
            if (element != null && !element.isJsonNull()) {
                args[i] = gson.fromJson(element, param.getType());
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * 类型转换（路径变量值 String → 目标类型），使用 Spring 的通用类型转换器
     */
    private Object convertType(String value, Class<?> targetType) {
        if (value == null) return null;
        return TYPE_CONVERTER.convertIfNecessary(value, targetType);
    }

    // ==================== 返回值处理 ====================

    /**
     * 检查分布式模式下的 HTTP 响应业务状态码
     */
    private void checkBusinessResult(String resultStr) {
        try {
            JsonElement element = JsonParser.parseString(resultStr);
            if (element.isJsonObject()) {
                R<?> r = gson.fromJson(element, R.class);
                if (!r.isSuccess()) {
                    throw new IllegalStateException("业务状态码响应失败: " + r.msg());
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 非 R 格式响应，忽略
        }
    }

    /**
     * 处理单应用模式下的方法调用返回值
     */
    private void handleLocalResult(Object result) {
        if (result == null) {
            XxlJobHelper.log("执行结果：无返回值");
            return;
        }
        XxlJobHelper.log("执行结果：\n {}", result);

        if (result instanceof R<?> r) {
            if (!r.isSuccess()) {
                throw new IllegalStateException("业务状态码响应失败: " + r.msg());
            }
        }
    }

    // ==================== 内部数据结构 ====================

    /**
     * HandlerMethod 包装类
     */
    private record HandlerMethodWrapper(HandlerMethod handlerMethod, PathPattern pathPattern) {}
}

package org.quyq.gwsu.common.security.filter;


import cn.hutool.jwt.JWTException;
import cn.hutool.core.net.NetUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.filter.RequestResponseContext;
import org.quyq.gwsu.common.core.utils.filter.RequestResponseProcessor;
import org.quyq.gwsu.common.security.api.IApiKeyClientApi;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginRequest;
import org.quyq.gwsu.common.security.api.vo.ApiKeyLoginUserVO;
import org.quyq.gwsu.common.security.casbin.field.FieldEnforcer;
import org.quyq.gwsu.common.security.config.properties.SecurityProperties;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.FieldRule;
import org.quyq.gwsu.common.security.domain.RequestContext;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.AuthenticationTokenUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description 鉴权过滤器 判断接口是否有访问权限 ， 和响应的字段权限判断
 */
@RequiredArgsConstructor
@Order(10)
@Slf4j
public class AuthenticationFilter implements RequestResponseProcessor {

    private final Enforcer enforcer;

    private final FieldEnforcer fieldEnforcer;

    private final SecurityUtils securityUtils;

    private final SecurityProperties securityProperties;

    private final IApiKeyClientApi apiKeyClientApi;

    private final Gson gson = new Gson();

    private static final String ATTRIBUTE_REQUEST_CONTEXT_KEY = "CURR_REQUEST_CONTEXT";
    private static final String ATTRIBUTE_FIELD_RULE_KEY = "FIELD_RULE";

    @Override
    public Mono<Boolean> preHandle(RequestResponseContext context) {
        return Mono.defer(() -> {
            context.getHeaders().put(CoreConstants.Headers.SERVER_FROM_APP , List.of("gateway"));
            // 检查是否为无需认证的接口
            if (shouldIgnoreAuthentication(context.getPath())) {
                return Mono.just(true);
            }

            Optional<Subject<Visitor>> subject;
            String token = getToken(context);
            try {
                subject = securityUtils.getSubject(token);
            }catch (JWTException e){
                throw new SecurityException(CommonErrorCode.E03001);
            }
            if (subject.isEmpty() && isApiKeyToken(token)) {
                return authenticateApiKeyAndAuthorize(context, token);
            }
            return authorize(context, subject);
        });
    }

    private Mono<Boolean> authenticateApiKeyAndAuthorize(RequestResponseContext context, String token) {
        ApiKeyLoginRequest loginRequest = new ApiKeyLoginRequest();
        loginRequest.setApiKey(token);
        loginRequest.setIp(resolveClientIp(context));

        Mono<Optional<Subject<Visitor>>> loginMono = Mono.fromCallable(() -> {
            FeignUtils.data(apiKeyClientApi.loginByApiKey(loginRequest));
            return securityUtils.getSubject(token);
        });

        if (!DeployUtils.isSingle()) {
            loginMono = loginMono.subscribeOn(Schedulers.boundedElastic());
        }

        return loginMono
                .onErrorMap(NullPointerException.class, e -> {
                    log.info("", e);
                    return new SecurityException(CommonErrorCode.E03007);
                })
                .flatMap(subject -> authorize(context, subject));
    }

    private Mono<Boolean> authorize(RequestResponseContext context, Optional<Subject<Visitor>> subject) {
        if (subject.isEmpty()) {
            return Mono.error(new SecurityException(CommonErrorCode.E03001));
        }

        if (shouldAuthAllow(context.getPath())) {
            return Mono.just(true);
        }

        RequestContext rc = buildContext(subject.get(), context);
        boolean allowed = enforcer.enforce(rc.subject(), rc.resType(), rc.action(), rc.resUrl(), rc.env());
        if (!allowed) {
            return Mono.error(new SecurityException(CommonErrorCode.E03002));
        }

        return Mono.just(true);
    }

    /**
     * 判断当前请求路径是否需要忽略认证
     *
     * @param path 请求路径
     * @return true 表示无需认证，直接放行
     */
    private boolean shouldIgnoreAuthentication(String path) {
        return securityProperties != null && securityProperties.shouldIgnore(path);
    }


    private boolean shouldAuthAllow(String path) {
        return securityProperties != null && securityProperties.shouldAuthAllow(path);
    }

    private boolean isApiKeyToken(String token) {
        return StringUtils.hasText(token) && token.startsWith(SecurityConstants.Authentication.API_KEY_PREFIX);
    }

    private String resolveClientIp(RequestResponseContext context) {
        String[] headerNames = new String[]{
                "x-forwarded-for",
                "x-real-ip",
                "proxy-client-ip",
                "wl-proxy-client-ip",
                "http_client_ip",
                "http_x_forwarded_for"
        };
        for (String headerName : headerNames) {
            String ip = context.getHeader(headerName);
            if (!NetUtil.isUnknown(ip)) {
                return NetUtil.getMultistageReverseProxyIp(ip);
            }
        }
        return null;
    }

    @Override
    public Mono<Void> postHandle(RequestResponseContext context) {
        return Mono.defer(() -> {

            List<FieldRule> rules = context.getAttribute(ATTRIBUTE_FIELD_RULE_KEY);
            if (CollectionUtils.isEmpty(rules)) {
                return Mono.empty();
            }
            Object newBody = changeResponseBody(rules, context.getOriginalResponseBody());
            if (Objects.nonNull(newBody)) {
                context.setModifiedResponseBody(newBody);
            }

            return Mono.empty();
        });


    }

    /**
     * 判断是否需要处理响应内容
     *
     * @param context 请求上下文（包含路径、头、参数等）
     * @return
     */
    @Override
    public boolean needsResponseBody(RequestResponseContext context) {
        // 无需认证的接口不处理响应体
        if (shouldIgnoreAuthentication(context.getPath())) {
            return false;
        }


        Optional<Subject<Visitor>> subject = securityUtils.getSubject(getToken(context));
        if (subject.isEmpty()) {
            return false;
        }
        List<FieldRule> matchingRules = fieldEnforcer.getMatchingRules(buildContext(subject.get(), context));
        if (!CollectionUtils.isEmpty(matchingRules)) {
            context.setAttribute(ATTRIBUTE_FIELD_RULE_KEY, matchingRules);
        }
        return !CollectionUtils.isEmpty(matchingRules);
    }


    /**
     * 通过字段过滤规则，将响应体中没有权限的字段过滤掉
     * 1.如果是数组，需要遍历数组处理
     * 2.匹配到的字段规则可能会有多条，其中需要通过 effect 字段确定是禁止还是允许字段，禁止优先级高于允许
     * 3.如果是 R<T> 统一返回类型，只处理 data 字段
     *
     * @param rules                字段过滤规则列表
     * @param originalResponseBody 原始响应体
     * @return 处理后的响应体对象
     */
    private Object changeResponseBody(List<FieldRule> rules, String originalResponseBody) {
        if (originalResponseBody == null || originalResponseBody.isBlank()) {
            return null;
        }

        JsonElement jsonElement = gson.fromJson(originalResponseBody, JsonElement.class);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }

        // 计算允许展示的字段集合和禁止展示的字段集合
        Set<String> allowFields = new HashSet<>();
        Set<String> denyFields = new HashSet<>();

        for (FieldRule rule : rules) {
            if ("allow".equalsIgnoreCase(rule.effect())) {
                allowFields.addAll(rule.fields());
            } else if ("deny".equalsIgnoreCase(rule.effect())) {
                denyFields.addAll(rule.fields());
            }
        }

        // deny优先级高于allow：如果字段在deny中，即使allow中有也要移除
        // 最终允许的字段 = allow字段 - deny字段
        Set<String> finalAllowFields = new HashSet<>(allowFields);
        finalAllowFields.removeAll(denyFields);
        boolean hasAllowRule = !allowFields.isEmpty();

        // 判断是否是 R<T> 统一返回类型
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (isRTypeResponse(jsonObject)) {
                // 只处理 data 字段
                JsonElement dataElement = jsonObject.get("data");
                if (dataElement != null && !dataElement.isJsonNull()) {
                    if (dataElement.isJsonArray()) {
                        processJsonArray(dataElement.getAsJsonArray(), finalAllowFields, denyFields, hasAllowRule);
                    } else if (dataElement.isJsonObject()) {
                        processJsonObject(dataElement.getAsJsonObject(), finalAllowFields, denyFields, hasAllowRule);
                    }
                }
                return jsonElement;
            }
        }

        // 非 R 类型，直接处理整个响应
        if (jsonElement.isJsonArray()) {
            processJsonArray(jsonElement.getAsJsonArray(), finalAllowFields, denyFields, hasAllowRule);
        } else if (jsonElement.isJsonObject()) {
            processJsonObject(jsonElement.getAsJsonObject(), finalAllowFields, denyFields, hasAllowRule);
        }

        return jsonElement;
    }

    /**
     * 判断是否是 R<T> 统一返回类型
     * R 类型包含 code, msg, data, errCode 字段
     *
     * @param jsonObject JSON对象
     * @return 是否是 R 类型
     */
    private boolean isRTypeResponse(JsonObject jsonObject) {
        return jsonObject.has("code")
                && jsonObject.has("msg")
                && jsonObject.has("data");
    }

    /**
     * 处理JSON数组，遍历每个元素进行处理
     *
     * @param jsonArray        JSON数组
     * @param finalAllowFields 最终允许展示的字段集合
     * @param denyFields       禁止展示的字段集合
     * @param hasAllowRule     是否存在allow规则
     */
    private void processJsonArray(JsonArray jsonArray, Set<String> finalAllowFields, Set<String> denyFields, boolean hasAllowRule) {
        for (JsonElement element : jsonArray) {
            if (element.isJsonObject()) {
                processJsonObject(element.getAsJsonObject(), finalAllowFields, denyFields, hasAllowRule);
            } else if (element.isJsonArray()) {
                processJsonArray(element.getAsJsonArray(), finalAllowFields, denyFields, hasAllowRule);
            }
        }
    }

    /**
     * 处理JSON对象，移除指定字段
     *
     * @param jsonObject       JSON对象
     * @param finalAllowFields 最终允许展示的字段集合
     * @param denyFields       禁止展示的字段集合
     * @param hasAllowRule     是否存在allow规则
     */
    private void processJsonObject(JsonObject jsonObject, Set<String> finalAllowFields, Set<String> denyFields, boolean hasAllowRule) {
        // 收集需要移除的字段
        Set<String> fieldsToRemove = new HashSet<>();

        for (String key : jsonObject.keySet()) {
            // deny字段必须移除
            if (denyFields.contains(key)) {
                fieldsToRemove.add(key);
            }
            // 如果存在allow规则，不在allow列表中的字段也要移除
            else if (hasAllowRule && !finalAllowFields.contains(key)) {
                fieldsToRemove.add(key);
            }
        }

        // 移除字段
        for (String field : fieldsToRemove) {
            jsonObject.remove(field);
        }

        // 递归处理嵌套对象和数组
        for (String key : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(key);
            if (value.isJsonObject()) {
                processJsonObject(value.getAsJsonObject(), finalAllowFields, denyFields, hasAllowRule);
            } else if (value.isJsonArray()) {
                processJsonArray(value.getAsJsonArray(), finalAllowFields, denyFields, hasAllowRule);
            }
        }
    }


    /**
     * 构建请求上下文
     *
     * @param context
     * @return
     */
    private RequestContext buildContext(Subject<Visitor> subject, RequestResponseContext context) {
        RequestContext rc = context.getAttribute(ATTRIBUTE_REQUEST_CONTEXT_KEY);
        if (Objects.nonNull(rc)) {
            return rc;
        }
        //资源
        String resource = extractResourceType(context);
        //行为
        String act = context.getMethod().toUpperCase(Locale.ROOT);
        //路径
        String url = getUrl(context);


        context.setAttribute(ATTRIBUTE_REQUEST_CONTEXT_KEY, new RequestContext(subject, resource, url, act, buildEnv()));
        return context.getAttribute(ATTRIBUTE_REQUEST_CONTEXT_KEY);
    }

    private String getToken(RequestResponseContext request) {
        return AuthenticationTokenUtils.resolve(request.getHeader(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY));
    }


    private String extractResourceType(RequestResponseContext context) {
        String cleanedPath = cleanedPath(context.getPath());
        int slashIndex = cleanedPath.indexOf('/');
        return slashIndex == -1 ? cleanedPath : cleanedPath.substring(0, slashIndex);
    }

    private String getUrl(RequestResponseContext context) {
        String cleanedPath = cleanedPath(context.getPath());
        int slashIndex = cleanedPath.indexOf('/');
        return slashIndex == -1 ? "" : cleanedPath.substring(slashIndex);
    }

    private String cleanedPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }


    /**
     * 构建环境上下文
     *
     * @return
     */
    private Map<String, Object> buildEnv() {
        LocalDateTime now = LocalDateTime.now();
        return Map.of(
                "datatime",now,
                "data", now.toLocalDate(),
                "time", now.toLocalTime());
    }

}

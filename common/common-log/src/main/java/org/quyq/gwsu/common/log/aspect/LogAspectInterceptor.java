package org.quyq.gwsu.common.log.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.ProxyUtil;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.log.config.properties.LogInfoConfigProperties;
import org.quyq.gwsu.common.log.constants.LogInfoConstants;
import org.quyq.gwsu.common.log.enums.ViewOperationSubject;
import org.quyq.gwsu.common.log.service.AccessLogHandlerService;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.AuthenticationTokenUtils;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class LogAspectInterceptor implements MethodInterceptor {

    private static final String ARGS_KEY = "args";

    private final LogInfoConfigProperties.AccessLogProperties logProperties;

    private final AccessLogHandlerService logService;

    private final ObjectMapper objectMapper;

    private final ObjectProvider<List<BusinessModuleInfoProvider>> providers;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final RequestParamExtractor extractor = new RequestParamExtractor();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Method method = invocation.getMethod();
        //如果日志功能没有启用或不是对外接口不记录
        if (!logProperties.enabled() ||
                needSkip(method)) {
            return invocation.proceed();
        }

        //如果是指定忽略的服务或者没有请求体不记录
        HttpServletRequest request = ServletUtils.getRequest();
        if (Objects.isNull(request)) {
            return invocation.proceed();
        }
        if (isNestedControllerInvocation(invocation, request)) {
            return invocation.proceed();
        }
        String requestURI = request.getRequestURI();
        String modulePrefix = getModulePrefix(requestURI);
        String uri = Objects.isNull(modulePrefix) ? requestURI : getUrl(requestURI);


        if (!CollectionUtils.isEmpty(logProperties.ignoreList())) {
            //如果是指定忽略的接口，则跳过
            if (logProperties.ignoreList().stream().anyMatch(i -> {
                if (CharSequenceUtil.isBlank(i)) {
                    return false;
                }

                String[] urls = i.split(" ");
                return matchIgnore(urls, uri, request.getMethod());
            })) {
                return invocation.proceed();
            }

        }


        return startRecordLog(invocation, request , modulePrefix , uri);
    }

    private Object startRecordLog(MethodInvocation invocation, HttpServletRequest request , String modulePrefix , String uri) throws Throwable {
        LogOperationVO log = createLog(invocation, request , modulePrefix , uri);
        //推送请求日志
        this.put(log);
        Object result;
        try {
            result = invocation.proceed();
            setResponse(log, result, null);
        } catch (Throwable ex) {
            setResponse(log, null, ex);
            throw ex;
        } finally {
            //推送响应日志
            this.put(log);
        }
        return result;
    }


    private boolean matchIgnore(String[] ignoreUrl, String uri, String method) {
        String configMethod = null;
        String configUri;
        if (ignoreUrl.length < 2) {
            configUri = ignoreUrl[0];
        } else {
            configMethod = ignoreUrl[0];
            configUri = ignoreUrl[1];
        }

        if (Objects.nonNull(configMethod)) {
            boolean match = !configMethod.contains("*") && !configMethod.equalsIgnoreCase(method);
            if (match) {
                return false;
            }
        }

        return pathMatcher.match(configUri, uri);
    }


    /**
     * 生成返回数据
     *
     * @param loger
     * @param result
     * @param ex
     */
    private void setResponse(LogOperationVO loger, Object result, Throwable ex) {
        try {
            LocalDateTime nowDate = LocalDateTime.now();
            loger.setResponseTime(nowDate).setModifyTime(nowDate);
            loger.setConsumeMill(Duration.between(loger.getRequestTime(), nowDate).toMillis())
                    .setModifyOp(ServletUtils.getHeaders().get(CoreConstants.Headers.AUTHORIZATION_USER_NAME));

            if (Objects.nonNull(ex)) {
                loger.setErrorMsg(ExceptionUtil.stacktraceToString(ex, 5000));
                return;
            }
            loger.setStatus(true);
            result = result instanceof ResponseEntity ? ((ResponseEntity<?>) result).getBody() : result;
            if (Objects.nonNull(result)) {
                if(isSSE(result)){
                    loger.setResponseData("SSE data stream...");
                }else {
                    loger.setResponseData(objectMapper.writeValueAsString(result));
                }

            }
        } catch (Exception e) {
            //捕获日志记录操作异常 ， 防止日志记录功能影响正常业务接口
            log.error("日志记录异常（不影响业务响应）", e);
        } finally {
            if (Objects.nonNull(loger))
                log.info("[接口响应]-\n┌───接口响应─────────────" +
                                "\n├── 响应结果： {} \n├── {} \n├── 接口耗时： {}ms " +
                                "\n└───────────────────────", Objects.isNull(ex) ? "成功" : "异常",
                        Objects.isNull(ex) ?
                                "响应数据： %s".formatted(loger.getResponseData())
                                :
                                "错误信息： %s".formatted(ex.getMessage())
                        , loger.getConsumeMill()
                );
        }


    }



    private boolean isSSE(Object result){
        if(result instanceof SseEmitter || result instanceof Flux<?> || result instanceof Mono<?>){
            return true;
        }
        return false;
    }

    /**
     * 生成日志文件
     *
     * @param invocation
     * @param request
     * @return
     */
    private LogOperationVO createLog(MethodInvocation invocation, HttpServletRequest request , String modulePrefix , String uri) {
        LogOperationVO accessLog = new LogOperationVO();
        LocalDateTime now = LocalDateTime.now();

        JsonNode requestParam = extractor.getRequestParam(request, invocation);

        accessLog.setOperId(IdUtil.getSnowflakeNextIdStr());
        accessLog.setRequestTime(now)
                //默认失败状态
                .setStatus(false)
                .setMethod(getMethodName(invocation))
                .setRequestMethod(request.getMethod().toUpperCase())
                .setRequestParam(objectMapper.writeValueAsString(requestParam))
                .setModulePrefix(modulePrefix)
                .setRequestUrl(uri)
                .setCreateTime(now);

        Map<String, String> headers = ServletUtils.getHeaders();

        String parentId = Optional.ofNullable(headers.get(LogInfoConstants.HEADER_TRACE_INFO))
                        .map(v ->v.split("-")[2]).orElse(null);


        accessLog
                .setTid(MDC.get(LogInfoConstants.TRACE_ID))
                .setParentId(parentId)
                .setFromApp(headers.get(CoreConstants.Headers.SERVER_FROM_APP))
                .setMenuId(headers.get(CoreConstants.Headers.VIEW_FROM_PAGE_MENU))
                .setOperSubject(ViewOperationSubject.getViewOperationSubject(
                        Optional.ofNullable(headers.get(CoreConstants.Headers.VIEW_OPERATION_SUBJECT))
                                .map(v -> {
                                    if(NumberUtil.isNumber(v)){
                                        return Integer.parseInt(v);
                                    }
                                    return 0;
                                })
                                .orElse(0)
                ))
                .setTokenId(getTokenId(headers))
                .setOperName(headers.get(CoreConstants.Headers.AUTHORIZATION_USER_NAME))
                .setTerminalDetail(headers.get("user-agent"))
                .setCreateOp(headers.get(CoreConstants.Headers.AUTHORIZATION_USER_NAME));

        setApiDescription(invocation, accessLog);

        //终端赋值
        if (ProxyUtil.hasClass("org.quyq.gwsu.common.security.utils.SecurityUtils")) {
            SpringUtils.getBean(SecurityUtils.class)
                    .getSubject().ifPresent(subject -> accessLog.setTerminal(subject.getTerminalType()));
        }
        if (Objects.isNull(accessLog.getTerminal())) {

            UserAgent userAgent = UserAgentUtil.parse(accessLog.getTerminalDetail());
            if (Objects.nonNull(userAgent)) {
                accessLog.setTerminal(userAgent.isMobile() ? TerminalType.MOBILE : TerminalType.PC);
            }
        }

        log.info("[接口调用]-\n┌───接口调用─────────────" +
                        "\n├── 路径： {}:{} \n├── 来源： {} \n├── 入参： {} " +
                        "\n└───────────────────────", request.getMethod().toUpperCase(), request.getRequestURI(),
                "%s端 - %s".formatted(accessLog.getTerminal(), accessLog.getOperName()),
                requestParam.get(ARGS_KEY).isEmpty() ? "无" : objectMapper.writeValueAsString(requestParam.get(ARGS_KEY)));

        return accessLog;

    }


    private String getModulePrefix(String url) {
        List<BusinessModuleInfoProvider> p = providers.getIfAvailable();
        if (CollectionUtils.isEmpty(p)) {
            return null;
        }

        if (p.size() == 1) {
            return p.getFirst().module().prefix();
        }

        String modulePrefix = extractModulePrefix(url);
        if (p.stream().anyMatch(p1 -> p1.module().prefix().equals(modulePrefix))) {
            return modulePrefix;
        }

        return null;

    }


    private String extractModulePrefix(String path) {
        String cleanedPath = cleanedPath(path);
        int slashIndex = cleanedPath.indexOf('/');
        return slashIndex == -1 ? cleanedPath : cleanedPath.substring(0, slashIndex);
    }

    private String getUrl(String path) {
        if (!DeployUtils.isSingle()) {
            return path;
        }
        String cleanedPath = cleanedPath(path);
        int slashIndex = cleanedPath.indexOf('/');
        return slashIndex == -1 ? "" : cleanedPath.substring(slashIndex);
    }

    private String cleanedPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private void setApiDescription(MethodInvocation invocation, LogOperationVO log) {
        Operation operation = invocation.getMethod().getAnnotation(Operation.class);
        Tag apiAnn = invocation.getMethod().getDeclaringClass().getAnnotation(Tag.class);
        if (Objects.nonNull(operation)) {
            log.setApiDescription(operation.summary());
        }
        if (Objects.nonNull(apiAnn)) {
            log.setApiModule(apiAnn.name());
        }

    }


    private boolean needSkip(Method method) {
        boolean noApi = !method.isAnnotationPresent(RequestMapping.class)
                && !method.isAnnotationPresent(GetMapping.class)
                && !method.isAnnotationPresent(PostMapping.class)
                && !method.isAnnotationPresent(DeleteMapping.class)
                && !method.isAnnotationPresent(PutMapping.class)
                && !method.isAnnotationPresent(PatchMapping.class);
        return method.isAnnotationPresent(LogIgnore.class)
                || noApi;
    }

    private boolean isNestedControllerInvocation(MethodInvocation invocation, HttpServletRequest request) {
        if (!DeployUtils.isSingle()) {
            return false;
        }
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }

        Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
        Method currentMethod = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        Method requestHandlerMethod = handlerMethod.getMethod();
        return !sameMethod(currentMethod, requestHandlerMethod);
    }

    private boolean sameMethod(Method currentMethod, Method requestHandlerMethod) {
        if (currentMethod.equals(requestHandlerMethod)) {
            return true;
        }
        return currentMethod.getName().equals(requestHandlerMethod.getName())
                && currentMethod.getParameterCount() == requestHandlerMethod.getParameterCount()
                && Arrays.equals(currentMethod.getParameterTypes(), requestHandlerMethod.getParameterTypes());
    }

    private String getMethodName(MethodInvocation invocation) {
        return CharSequenceUtil.format("{}.{}()", invocation.getThis().getClass().getName(), invocation.getMethod().getName());
    }

    private String getTokenId(Map<String, String> headers) {
        return AuthenticationTokenUtils.resolve(headers.get(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY));

    }


    private void put(LogOperationVO log) {
        logService.save(log);
    }

}

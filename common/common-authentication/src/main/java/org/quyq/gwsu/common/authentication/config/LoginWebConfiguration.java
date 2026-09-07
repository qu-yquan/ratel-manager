package org.quyq.gwsu.common.authentication.config;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.func.LambdaUtil;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceScopeManager;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.login.LoginManager;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformLoginDTO;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.quyq.gwsu.common.authentication.oauth.path.AuthenticationEndpointPathResolver;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.core.exception.ArgumentException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.exception.handler.GlobalExceptionFunctionHandler;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetRequest;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceFacade;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.*;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description 认证相关接口暴露配置
 */
@AutoConfiguration
@ConditionalOnClass({RouterFunctions.class})
@Slf4j
public class LoginWebConfiguration {

    private static final String USER_TYPE = "accountType";

    @Resource
    private LoginManager loginManager;

    @Resource
    private IDGenerationUtils idGenerationUtils;

    @Resource
    private SecurityUtils securityUtils;

    @Resource
    private SessionUtils sessionUtils;

    @Resource
    private Gson gson;

    @Resource
    private CaptchaServiceFacade captchaServiceFacade;

    @Resource
    private AuthenticationEndpointPathResolver endpointPathResolver;

    @Resource
    private OAuthFrontendEndpointResolver oauthFrontendEndpointResolver;

    @Resource
    private OAuthAuthorizationViewProviderManager oauthAuthorizationViewProviderManager;


    @Bean
    public RouterFunction<ServerResponse> loginRouters() {
        return RouterFunctions
                /**
                 * 用户认证
                 * accountType 账号类型 {@ling org.quyq.gwsu.common.security.enums.AccountType}
                 */
                .route(RequestPredicates.POST(buildPath("/auth/login/{accountType}")), this::login)

                /**
                 * 用户退出登录
                 */
                .andRoute(RequestPredicates.POST(buildPath("/auth/logout")), this::logout)

                /**
                 * 三方平台认证回调接口
                 * accountType 账号类型 {@ling org.quyq.gwsu.common.security.enums.AccountType}
                 * loginType 登录类型
                 */
                .andRoute(RequestPredicates.GET(buildPath("/auth/callback/{accountType}/{loginType}")), this::callback)

                /**
                 * 获取前端调用的三方认证地址
                 * accountType 账号类型 {@ling org.quyq.gwsu.common.security.enums.AccountType}
                 * loginType 登录类型
                 */
                .andRoute(RequestPredicates.GET(buildPath("/auth/url/{accountType}/{loginType}")), this::authUrl)

                /**
                 * 获取验证码
                 */
                .andRoute(RequestPredicates.GET(buildPath("/auth/captcha/get")), this::getCaptcha)

                /**
                 * OAuth 授权确认页面跳转入口。
                 */
                .andRoute(RequestPredicates.GET(buildPath("/oauth2/loginConsent")), this::oauthConsent)

                /**
                 * 一次校验验证码
                 */
                .andRoute(RequestPredicates.POST(buildPath("/auth/captcha/check")), this::checkCaptcha)

                /**
                 * 获取工作区列表和当前所属工作区
                 */
                .andRoute(RequestPredicates.POST(buildPath("/workspace/list")), this::workspaceList)

                /**
                 * 切换指定工作区
                 */
                .andRoute(RequestPredicates.POST(buildPath("/workspace/switch/{workspaceId}")), this::switchWorkspace)
                .filter(new GlobalExceptionFunctionHandler());
    }

    /**
     * 获取验证码
     *
     * @param request
     * @return
     */
    private ServerResponse getCaptcha(ServerRequest request) {
        CaptchaType type = request.param("type")
                .filter(StringUtils::hasText)
                .map(CaptchaType::from)
                .orElse(null);
        String scene = request.param("scene").orElse("login");
        String clientUid = request.param("clientUid").orElse(null);
        return ServerResponse.ok()
                .body(R.ok(captchaServiceFacade.get(new CaptchaGetRequest(type, scene, clientUid))));
    }

    private ServerResponse oauthConsent(ServerRequest request) {
        String authorizeUri = oauthFrontendEndpointResolver.apiUrl(buildPath("/oauth2/authorize"));
        String frontendConsentUrl = oauthAuthorizationViewProviderManager
                .resolve(request.param("client_id").orElse(null))
                .consentPageUrl(request.params(), authorizeUri);
        return ServerResponse.temporaryRedirect(URI.create(frontendConsentUrl)).build();
    }

    /**
     * 一次校验验证码
     *
     * @param request
     * @return
     */
    private ServerResponse checkCaptcha(ServerRequest request) throws ServletException, IOException {
        CaptchaCheckRequest form = request.body(CaptchaCheckRequest.class);
        return ServerResponse.ok()
                .body(R.ok(captchaServiceFacade.check(form)));
    }

    /**
     * 获取第三方认证地址，用于前端跳转
     *
     * @param request
     * @return
     */
    private ServerResponse authUrl(ServerRequest request) {
        AccountType accountType = AccountType.fromString(request.pathVariable(USER_TYPE));

        String loginType = request.pathVariable("loginType");

        String state = idGenerationUtils.generateNextIdStr();

        return ServerResponse.ok()
                .body(R.ok(loginManager.webAuthUrl(state, accountType, loginType)));

    }

    /**
     * 登录逻辑
     *
     * @param request
     * @return
     * @throws Exception
     */
    private ServerResponse login(ServerRequest request) throws ServletException, IOException {

        AccountType accountType = AccountType.fromString(request.pathVariable(USER_TYPE));
        JsonNode body = request.body(JsonNode.class);
        return response(loginManager.login(body, accountType));
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    private ServerResponse logout(ServerRequest request) {
        StpUtil.logout();
        return ServerResponse.ok()
                .body(R.ok("退出成功"));
    }


    /**
     * 获取工作区列表
     *
     * @param request
     * @return
     */
    private ServerResponse workspaceList(ServerRequest request) {

        List<WorkspaceInfo> workspaceList = securityUtils.userInfo()
                .map(DataResourceScopeManager::workspaceList)
                .orElse(Collections.emptyList());
        Optional<WorkspaceInfo> currWorkspace = sessionUtils.getValue(SecurityConstants.Session.SESSION_CURR_WORKSPACE);

        return ServerResponse.ok()
                .body(R.ok(
                        //工作区列表
                        Map.of("list", workspaceList,
                                //当前所属工作区
                                "currWorkspace", currWorkspace.orElse(null))
                ));
    }


    /**
     * 切换工作区
     *
     * @param request
     * @return
     */
    private ServerResponse switchWorkspace(ServerRequest request) {
        //工作区ID
        String workspaceId = AssertUtils.hasText(request.pathVariable("workspaceId"), CommonErrorCode.E04006);
        Optional<Subject<Visitor>> subjectOpt = securityUtils.getSubject();
        AssertUtils.isTrue(subjectOpt.isPresent(), CommonErrorCode.E03002);
        Subject<Visitor> subject = subjectOpt.get();
        Optional<UserInfo> userInfo = subject.userInfo();
        AssertUtils.isTrue(userInfo.isPresent(), CommonErrorCode.E03002);

        Optional<WorkspaceInfo> workspaceInfo = DataResourceScopeManager.workspaceList(userInfo.get())
                .stream().filter(v -> workspaceId.equals(v.id())).findFirst();

        if (workspaceInfo.isEmpty()) {
            throw new ArgumentException(CommonErrorCode.E04007);
        }

        sessionUtils.putValue(SecurityConstants.Session.SESSION_CURR_WORKSPACE, workspaceInfo.get());
        Map<String, List<?>> dataedResource = DataResourceScopeManager.dataResource(workspaceInfo.get(), userInfo.get() ,subject.getDataScope());
        sessionUtils.putValue(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE, dataedResource);
        log.info("====>工作区【{}】切换成功 ,当前资源数据：{}", workspaceInfo.get().name(), gson.toJson(dataedResource));

        return ServerResponse
                .ok()
                .body(R.ok("切换成功"));

    }


    /**
     * 三方平台回调接口
     *
     * @param request
     * @return
     */
    private ServerResponse callback(ServerRequest request) {
        ThreePlatformLoginDTO form = new ThreePlatformLoginDTO();
        form.setCode(request.param(LambdaUtil.getFieldName(ThreePlatformLoginDTO::getCode)).orElse(null));
        form.setExtraParam(request.params());
        form.setType(request.pathVariable("loginType"));
        form.setTerminal(TerminalType.WEB);

        AccountType accountType = AccountType.fromString(request.pathVariable(USER_TYPE));

        return response(loginManager.login(form, accountType, VisitorType.USER));
    }

    private ServerResponse response(LoginManager.LoginResult result) {
        if (result.isRedirect()) {
            return ServerResponse
                    .temporaryRedirect(URI.create(result.redirectUrl()))
                    .build();

        }

        return ServerResponse.ok()
                .body(result.token());
    }


    private String buildPath(String path) {
        return endpointPathResolver.resolve(path);
    }

}

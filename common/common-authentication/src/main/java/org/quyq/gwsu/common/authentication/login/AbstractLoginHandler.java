package org.quyq.gwsu.common.authentication.login;


import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceScopeManager;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorContext;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.api.IRoleInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.UserRoleInfo;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaVerifyRequest;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceFacade;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@Slf4j
public abstract class AbstractLoginHandler<T extends AbstractLoginDTO, U extends UserInfo> implements LoginHandler<T> {

    /**
     * 认证逻辑
     *
     * @param loginVO
     * @param properties 其他配置属性，用于不同实现修改行为
     * @return
     */
    protected abstract U toAuth(T loginVO, CoreProperties properties);

    @Override
    public LoginVO authenticate(T loginDTO, @NonNull VisitorType visitorType) {
        CoreProperties properties = new CoreProperties();
        LoginVO loginVO = new LoginVO();
        String type = loginType();
        String authorizationId = SpringUtils.getBean(IDGenerationUtils.class).generateNextIdStr();

        return ScopedValue.where(AuthenticationConstants.ACCOUNT_TYPE, accountType())
                .call(() -> {
                    LoginInterceptorContext<U> context = new LoginInterceptorContext<>(
                            loginDTO, loginVO, authorizationId, accountType(), visitorType, type);

                    try {
                        verifyCaptchaIfNecessary(loginDTO);
                        U auth = toAuth(loginDTO, properties);

                        Subject<U> subject = buildSubject(auth);
                        context.setSubject(subject);

                        loginVO.setUserId(auth.getUserId());
                        loginVO.setNeedRedirect(properties.redirect);
                        loginVO.setRedirectUrl(properties.redirectUrl);

                        // 阶段1: 用户认证成功后，判断是否继续
                        if (!LoginInterceptorUtils.fireAfterAuthenticated(type, context)) {
                            return loginVO;
                        }

                        // 登录
                        SaLoginParameter loginParameter = Optional.ofNullable(properties.getSaLoginParameter())
                                .orElseGet(SaLoginParameter::new);
                        StpUtil.login("%s:%s".formatted(visitorType.name(), loginVO.getUserId()), loginParameter);
                        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

                        loginVO.setToken(tokenInfo.tokenValue);
                        loginVO.setExpires(tokenInfo.tokenTimeout);

                        // 记录token格式，JWT格式包含两个"."分隔符
                        log.info("[Login] token生成: loginType={}, userId={}, tokenFormat={}",
                                type, loginVO.getUserId(),
                                tokenInfo.tokenValue != null && tokenInfo.tokenValue.chars().filter(c -> c == '.').count() == 2 ? "JWT" : "UUID");

                        putSessionData(auth, subject, visitorType, authorizationId);


                        // 阶段2: 登录成功后
                        LoginInterceptorUtils.fireAfterLoginSuccess(type, context);

                    } catch (Exception e) {
                        // 阶段3: 登录失败后
                        LoginInterceptorUtils.fireAfterLoginFailure(type, context, e);

                        if (!properties.isRedirect()) {
                            throw e;
                        }
                        // 如果需要重定向，则不能抛出异常，将错误消息收集，重定向到对应接口上
                        ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(e);
                        loginVO.setErrMsg(errorInfo.result().msg());
                        loginVO.setErrCode(errorInfo.result().errCode());
                    }

                    return loginVO;
                });


    }


    private void verifyCaptchaIfNecessary(T loginDTO) {
        if (!StringUtils.hasText(loginDTO.getCaptchaId())) {
            return;
        }
        AssertUtils.hasText(loginDTO.getCaptchaCode(), CommonErrorCode.E04009);
        CaptchaServiceFacade facade = SpringUtils.getBean(CaptchaServiceFacade.class);
        facade.verifyForLogin(new CaptchaVerifyRequest(
                loginDTO.getCaptchaId(),
                loginDTO.getCaptchaCode(),
                loginDTO.getType(),
                "login"
        ));
    }


    private void putSessionData(U auth, Subject<U> subject, VisitorType visitorType, String authorizationId) {
        // 加载数据资源信息
        List<WorkspaceInfo> workspaceList = DataResourceScopeManager.workspaceList(auth);
        //账号session存储用户信息
        StpUtil.getSession()
                .set(SecurityConstants.Session.SESSION_SUBJECT_INFO_KEY, subject);

        // 创建TokenSession
        SaSession tokenSession = StpUtil.getTokenSession();
        // tokensession 存储当前工作空间和数据资源信息
        tokenSession.set(SecurityConstants.Session.SESSION_CURR_WORKSPACE, workspaceList.getFirst());
        tokenSession.set(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE,
                DataResourceScopeManager.dataResource(workspaceList.getFirst(), auth, subject.getDataScope()));
        // 访问者类型
        tokenSession.set(SecurityConstants.Session.SESSION_USER_VISITOR_TYPE , visitorType);
        //登录类型
        tokenSession.set(SecurityConstants.Session.SESSION_USER_LOGIN_TYPE , loginType());
        tokenSession.set(SecurityConstants.Session.SESSION_AUTHORIZATION_ID, authorizationId);
        tokenSession.set(SecurityConstants.Session.SESSION_ACCOUNT_TYPE, accountType());
    }


    private Subject<U> buildSubject(U user) {
        Subject<U> subject = new Subject<>(user);

        // 加载角色、作用域信息
        List<IRoleInfoClientApi> roleClientApi = SpringUtils.getBeansOfType(IRoleInfoClientApi.class);
        if (!CollectionUtils.isEmpty(roleClientApi) && StringUtils.hasText(user.getUserId())) {
            UserRoleInfo roleInfo = FeignUtils.data(roleClientApi.getFirst().getRoleListBySubject(user.getUserId()));
            if (Objects.nonNull(roleInfo)) {
                if (!CollectionUtils.isEmpty(roleInfo.roles())) {
                    subject.setRoles(roleInfo.roles());
                }
                subject.setDataScope(Optional.ofNullable(roleInfo.dataScope()).orElse(DataScope.SELF_ONLY));
            }

        }

        return subject;
    }

    @Data
    protected static class CoreProperties {

        /**
         * 是否重定向跳转
         */
        private boolean redirect = false;

        /**
         * 重定向跳转地址
         */
        private String redirectUrl;

        /**
         * Sa-Token 登录参数
         */
        private SaLoginParameter saLoginParameter;

    }

}

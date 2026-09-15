package org.quyq.gwsu.common.authentication.login.interceptor.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.login.impl.password.PasswordLoginDTO;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptor;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorContext;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.ExceptionMsgHandler;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.log.enums.LoginEventType;
import org.quyq.gwsu.common.log.enums.LoginSessionStatus;
import org.quyq.gwsu.common.log.security.TokenFingerprint;
import org.quyq.gwsu.common.log.security.TokenFingerprintService;
import org.quyq.gwsu.common.log.service.LoginLogHandlerService;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.core.Ordered;

import java.time.LocalDateTime;

/**
 * 基于登录生命周期记录普通认证日志。
 */
@Slf4j
@RequiredArgsConstructor
public class LoginLogInterceptor implements LoginInterceptor<UserInfo> {

    private final LoginLogHandlerService loginLogHandlerService;

    private final TokenFingerprintService tokenFingerprintService;

    @Override
    public void afterLoginSuccess(LoginInterceptorContext<UserInfo> context) {
        if (VisitorType.CLIENT == context.getVisitorType()) {
            return;
        }
        UserInfo user = context.getSubject().userInfo().orElse(null);
        TokenFingerprint fingerprint = tokenFingerprintService.generate(context.getLoginVO().getToken());
        LogLoginVO loginLog = baseLog(context)
                .setStatus(true)
                .setSessionStatus(LoginSessionStatus.ACTIVE)
                .setTokenFingerprint(fingerprint.value())
                .setTokenKeyVersion(fingerprint.keyVersion());
        if (user != null) {
            loginLog.setUserId(user.getUserId()).setUserName(user.getUserName());
        }
        safelySave(loginLog);
    }

    @Override
    public void afterLoginFailure(LoginInterceptorContext<UserInfo> context, Throwable exception) {
        ExceptionMsgHandler.ErrorInfo errorInfo = ExceptionMsgHandler.determineErrorInfo(exception);
        R<Void> result = errorInfo.result();
        LogLoginVO loginLog = baseLog(context)
                .setStatus(false)
                .setSessionStatus(LoginSessionStatus.INACTIVE)
                .setFailureCode(result.errCode())
                .setFailureMessage(result.msg());
        if (context.getSubject() != null) {
            context.getSubject().userInfo().ifPresent(user -> loginLog
                    .setUserId(user.getUserId())
                    .setUserName(user.getUserName()));
        }
        safelySave(loginLog);
    }

    private LogLoginVO baseLog(LoginInterceptorContext<UserInfo> context) {
        LogLoginVO loginLog = new LogLoginVO()
                .setAuthorizationId(context.getAuthorizationId())
                .setEventType(LoginEventType.LOGIN)
                .setAccountType(context.getAccountType())
                .setVisitorType(context.getVisitorType())
                .setLoginType(context.getLoginType())
                .setLoginAccount(loginAccount(context))
                .setTerminal(context.getLoginDTO().getTerminal())
                .setTerminalDetail(ServletUtils.getHeaders().get("user-agent"))
                .setClientIp(ServletUtils.getClientIP())
                .setEventTime(LocalDateTime.now());
        loginLog.setCreateTime(LocalDateTime.now());
        return loginLog;
    }

    private String loginAccount(LoginInterceptorContext<UserInfo> context) {
        if (context.getLoginDTO() instanceof PasswordLoginDTO passwordLoginDTO) {
            return passwordLoginDTO.getUsername();
        }
        return null;
    }

    private void safelySave(LogLoginVO loginLog) {
        try {
            loginLogHandlerService.save(loginLog);
        } catch (Exception exception) {
            log.warn("认证日志提交异常，不影响认证结果：{}", exception.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

package org.quyq.gwsu.common.authentication.oauth.token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.quyq.gwsu.common.authentication.oauth.store.CustomOAuth2AuthorizationService;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.log.enums.LoginEventType;
import org.quyq.gwsu.common.log.enums.LoginSessionStatus;
import org.quyq.gwsu.common.log.security.TokenFingerprint;
import org.quyq.gwsu.common.log.security.TokenFingerprintService;
import org.quyq.gwsu.common.log.service.LoginLogHandlerService;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth2 Access Token 签发完成后的认证日志记录器。
 */
@Slf4j
@RequiredArgsConstructor
public class OAuthLoginLogRecorder {

    private final LoginLogHandlerService loginLogHandlerService;

    private final TokenFingerprintService tokenFingerprintService;

    private final OAuthClientAccountTypeResolver accountTypeResolver;

    public void recordSuccess(HttpServletRequest request, RegisteredClient registeredClient,
                              OAuth2AccessToken accessToken, String requestedGrantType,
                              OAuth2Authorization authorization) {
        try {
            TokenFingerprint fingerprint = tokenFingerprintService.generate(accessToken.getTokenValue());
            String loginType = CustomOAuthSubjectWriter.oauthLoginType(requestedGrantType, authorization);
            LogLoginVO loginLog = new LogLoginVO()
                    .setAuthorizationId(authorization.getId())
                    .setEventType(AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(requestedGrantType)
                            ? LoginEventType.TOKEN_REFRESH : LoginEventType.LOGIN)
                    .setAccountType(accountTypeResolver.resolve(registeredClient))
                    .setVisitorType(VisitorType.CLIENT)
                    .setLoginType(loginType)
                    .setGrantType(requestedGrantType)
                    .setClientId(registeredClient.getClientId())
                    .setTokenFingerprint(fingerprint.value())
                    .setTokenKeyVersion(fingerprint.keyVersion())
                    .setSessionStatus(LoginSessionStatus.ACTIVE)
                    .setTerminalDetail(request.getHeader("user-agent"))
                    .setClientIp(ServletUtils.getClientIP(request))
                    .setStatus(true)
                    .setEventTime(LocalDateTime.now());
            loginLog.setCreateTime(LocalDateTime.now());
            authUser(authorization).ifPresent(user -> loginLog
                    .setUserId(user.getUserId())
                    .setUserName(user.getUserName()));
            loginLogHandlerService.save(loginLog);
        } catch (Exception exception) {
            log.warn("OAuth2 认证日志提交异常，不影响 Token 响应：{}", exception.getMessage());
        }
    }

    private Optional<UserInfo> authUser(OAuth2Authorization authorization) {
        Object value = authorization.getAttribute(CustomOAuth2AuthorizationService.RATEL_SUBJECT_ATTRIBUTE);
        if (value instanceof Subject<?> subject) {
            return subject.userInfo();
        }
        return Optional.empty();
    }
}

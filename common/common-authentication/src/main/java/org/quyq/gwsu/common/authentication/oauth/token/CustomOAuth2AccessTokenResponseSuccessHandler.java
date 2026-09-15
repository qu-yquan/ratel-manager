package org.quyq.gwsu.common.authentication.oauth.token;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2UnifiedSuccessResponseHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * OAuth token 响应前写入 Ratel 登录态。
 *
 * @author Quyq
 */
public class CustomOAuth2AccessTokenResponseSuccessHandler implements AuthenticationSuccessHandler {

    private final CustomOAuthSubjectWriter subjectWriter;

    private final OAuth2UnifiedSuccessResponseHandler delegate;

    private final OAuthLoginLogRecorder loginLogRecorder;

    public CustomOAuth2AccessTokenResponseSuccessHandler(
            CustomOAuthSubjectWriter subjectWriter,
            OAuth2UnifiedSuccessResponseHandler delegate,
            OAuthLoginLogRecorder loginLogRecorder) {
        this.subjectWriter = subjectWriter;
        this.delegate = delegate;
        this.loginLogRecorder = loginLogRecorder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuthentication) {
            String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
            OAuth2Authorization authorization = subjectWriter.write(
                    tokenAuthentication.getRegisteredClient(),
                    tokenAuthentication.getAccessToken(),
                    grantType);
            loginLogRecorder.recordSuccess(
                    request,
                    tokenAuthentication.getRegisteredClient(),
                    tokenAuthentication.getAccessToken(),
                    grantType,
                    authorization);
        }
        delegate.onAuthenticationSuccess(request, response, authentication);
    }

}

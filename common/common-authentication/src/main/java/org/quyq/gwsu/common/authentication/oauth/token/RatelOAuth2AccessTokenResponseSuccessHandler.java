package org.quyq.gwsu.common.authentication.oauth.token;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AccessTokenResponseAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * OAuth token 响应前写入 Ratel 登录态。
 *
 * @author Quyq
 */
public class RatelOAuth2AccessTokenResponseSuccessHandler implements AuthenticationSuccessHandler {

    private final RatelOAuthSubjectWriter subjectWriter;

    private final OAuth2AccessTokenResponseAuthenticationSuccessHandler delegate =
            new OAuth2AccessTokenResponseAuthenticationSuccessHandler();

    public RatelOAuth2AccessTokenResponseSuccessHandler(RatelOAuthSubjectWriter subjectWriter) {
        this.subjectWriter = subjectWriter;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuthentication) {
            subjectWriter.write(tokenAuthentication.getRegisteredClient(), tokenAuthentication.getAccessToken());
        }
        delegate.onAuthenticationSuccess(request, response, authentication);
    }

}

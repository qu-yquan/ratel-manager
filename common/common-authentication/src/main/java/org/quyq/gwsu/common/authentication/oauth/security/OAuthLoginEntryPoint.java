package org.quyq.gwsu.common.authentication.oauth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * OAuth 需要用户登录时跳转到现有登录入口。
 *
 * @author Quyq
 */
public class OAuthLoginEntryPoint implements AuthenticationEntryPoint {

    private final OAuthFrontendEndpointResolver frontendEndpointResolver;

    private final OAuthAuthorizationViewProviderManager viewProviderManager;

    public OAuthLoginEntryPoint(
            OAuthFrontendEndpointResolver frontendEndpointResolver,
            OAuthAuthorizationViewProviderManager viewProviderManager) {
        this.frontendEndpointResolver = frontendEndpointResolver;
        this.viewProviderManager = viewProviderManager;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        String target = frontendEndpointResolver.apiUrl(request.getRequestURI());
        if (request.getQueryString() != null) {
            target = target + "?" + request.getQueryString();
        }
        response.sendRedirect(viewProviderManager
                .resolve(request.getParameter(OAuth2ParameterNames.CLIENT_ID))
                .loginPageUrl(target));
    }

}

package org.quyq.gwsu.common.authentication.oauth.response;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2DeviceVerificationAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** 将设备验证结果返回到设备码交互页面。 */
public class OAuth2DeviceVerificationResultHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final String RESULT_PARAMETER = "result";
    private static final String ERROR_PARAMETER = "error";

    private final OAuthAuthorizationViewProviderManager viewProviderManager;
    private final OAuthFrontendEndpointResolver endpointResolver;
    private final String deviceVerificationPath;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OAuth2DeviceVerificationResultHandler(
            OAuthAuthorizationViewProviderManager viewProviderManager,
            OAuthFrontendEndpointResolver endpointResolver,
            String deviceVerificationPath) {
        this.viewProviderManager = viewProviderManager;
        this.endpointResolver = endpointResolver;
        this.deviceVerificationPath = deviceVerificationPath;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String clientId = authentication instanceof OAuth2DeviceVerificationAuthenticationToken token
                ? token.getClientId()
                : request.getParameter("client_id");
        redirect(request, response, clientId, Map.of(RESULT_PARAMETER, List.of("success")));
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String error = exception instanceof OAuth2AuthenticationException oauth2Exception
                ? oauth2Exception.getError().getErrorCode()
                : "invalid_request";
        redirect(request, response, request.getParameter("client_id"), Map.of(
                RESULT_PARAMETER, List.of("error"),
                ERROR_PARAMETER, List.of(error)
        ));
    }

    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String clientId,
            Map<String, List<String>> params) throws IOException {
        String verificationUri = endpointResolver.apiUrl(deviceVerificationPath);
        String target = viewProviderManager.resolve(clientId)
                .deviceVerificationPageUrl(params, verificationUri);
        redirectStrategy.sendRedirect(request, response, target);
    }
}

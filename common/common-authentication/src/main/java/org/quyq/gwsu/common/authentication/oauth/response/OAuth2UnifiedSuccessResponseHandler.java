package org.quyq.gwsu.common.authentication.oauth.response;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2DeviceAuthorizationRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenIntrospectionAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** OAuth2 JSON 端点成功响应处理器。 */
public class OAuth2UnifiedSuccessResponseHandler implements AuthenticationSuccessHandler {

    private final OAuth2ResponseWriter responseWriter;

    public OAuth2UnifiedSuccessResponseHandler(OAuth2ResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        responseWriter.writeSuccess(response, responseData(authentication));
    }

    public Object responseData(Authentication authentication) {
        if (authentication instanceof OAuth2AccessTokenAuthenticationToken accessToken) {
            return accessTokenData(accessToken);
        }
        if (authentication instanceof OAuth2DeviceAuthorizationRequestAuthenticationToken deviceAuthorization) {
            return deviceAuthorizationData(deviceAuthorization);
        }
        if (authentication instanceof OAuth2TokenIntrospectionAuthenticationToken introspection) {
            return new LinkedHashMap<>(introspection.getTokenClaims().getClaims());
        }
        if (authentication instanceof OAuth2TokenRevocationAuthenticationToken) {
            return null;
        }
        throw new IllegalArgumentException("Unsupported OAuth2 success authentication: " + authentication.getClass());
    }

    private Map<String, Object> accessTokenData(OAuth2AccessTokenAuthenticationToken authentication) {
        OAuth2AccessToken accessToken = authentication.getAccessToken();
        Map<String, Object> data = new LinkedHashMap<>(authentication.getAdditionalParameters());
        data.put(OAuth2ParameterNames.ACCESS_TOKEN, accessToken.getTokenValue());
        data.put(OAuth2ParameterNames.TOKEN_TYPE, accessToken.getTokenType().getValue());
        putExpiresIn(data, accessToken);
        if (!accessToken.getScopes().isEmpty()) {
            data.put(OAuth2ParameterNames.SCOPE, String.join(" ", accessToken.getScopes()));
        }
        if (authentication.getRefreshToken() != null) {
            data.put(OAuth2ParameterNames.REFRESH_TOKEN, authentication.getRefreshToken().getTokenValue());
        }
        return data;
    }

    private Map<String, Object> deviceAuthorizationData(
            OAuth2DeviceAuthorizationRequestAuthenticationToken authentication) {
        Map<String, Object> data = new LinkedHashMap<>(authentication.getAdditionalParameters());
        data.put(OAuth2ParameterNames.DEVICE_CODE, authentication.getDeviceCode().getTokenValue());
        data.put(OAuth2ParameterNames.USER_CODE, authentication.getUserCode().getTokenValue());
        data.put(OAuth2ParameterNames.VERIFICATION_URI, authentication.getAuthorizationUri());
        putExpiresIn(data, authentication.getDeviceCode());
        return data;
    }

    private void putExpiresIn(Map<String, Object> data, AbstractOAuth2Token token) {
        if (token.getIssuedAt() != null && token.getExpiresAt() != null) {
            data.put(OAuth2ParameterNames.EXPIRES_IN,
                    Math.max(0, Duration.between(token.getIssuedAt(), token.getExpiresAt()).toSeconds()));
        }
    }
}

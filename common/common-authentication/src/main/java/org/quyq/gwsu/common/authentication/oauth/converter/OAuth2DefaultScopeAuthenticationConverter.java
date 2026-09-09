package org.quyq.gwsu.common.authentication.oauth.converter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2DeviceAuthorizationRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.LinkedHashSet;

/** 在首次授权请求未指定 scope 时使用客户端配置的全部 scope。 */
public class OAuth2DefaultScopeAuthenticationConverter implements AuthenticationConverter {

    private final AuthenticationConverter delegate;
    private final RegisteredClientRepository registeredClientRepository;

    public OAuth2DefaultScopeAuthenticationConverter(
            AuthenticationConverter delegate,
            RegisteredClientRepository registeredClientRepository) {
        this.delegate = delegate;
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication authentication = delegate.convert(request);
        if (authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCode
                && authorizationCode.getScopes().isEmpty()) {
            RegisteredClient client = registeredClientRepository.findByClientId(authorizationCode.getClientId());
            if (hasScopes(client)) {
                return new OAuth2AuthorizationCodeRequestAuthenticationToken(
                        authorizationCode.getAuthorizationUri(),
                        authorizationCode.getClientId(),
                        (Authentication) authorizationCode.getPrincipal(),
                        authorizationCode.getRedirectUri(),
                        authorizationCode.getState(),
                        new LinkedHashSet<>(client.getScopes()),
                        authorizationCode.getAdditionalParameters());
            }
        }
        if (authentication instanceof OAuth2ClientCredentialsAuthenticationToken clientCredentials
                && clientCredentials.getScopes().isEmpty()) {
            RegisteredClient client = resolveAuthenticatedClient(clientCredentials.getPrincipal());
            if (hasScopes(client)) {
                return new OAuth2ClientCredentialsAuthenticationToken(
                        (Authentication) clientCredentials.getPrincipal(),
                        new LinkedHashSet<>(client.getScopes()),
                        clientCredentials.getAdditionalParameters());
            }
        }
        if (authentication instanceof OAuth2DeviceAuthorizationRequestAuthenticationToken deviceAuthorization
                && deviceAuthorization.getScopes().isEmpty()) {
            RegisteredClient client = resolveAuthenticatedClient(deviceAuthorization.getPrincipal());
            if (hasScopes(client)) {
                return new OAuth2DeviceAuthorizationRequestAuthenticationToken(
                        (Authentication) deviceAuthorization.getPrincipal(),
                        deviceAuthorization.getAuthorizationUri(),
                        new LinkedHashSet<>(client.getScopes()),
                        deviceAuthorization.getAdditionalParameters());
            }
        }
        return authentication;
    }

    private RegisteredClient resolveAuthenticatedClient(Object principal) {
        return principal instanceof OAuth2ClientAuthenticationToken clientAuthentication
                ? clientAuthentication.getRegisteredClient()
                : null;
    }

    private boolean hasScopes(RegisteredClient client) {
        return client != null && !client.getScopes().isEmpty();
    }
}

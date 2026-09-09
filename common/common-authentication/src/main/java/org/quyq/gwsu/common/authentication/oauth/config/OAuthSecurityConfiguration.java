package org.quyq.gwsu.common.authentication.oauth.config;

import org.quyq.gwsu.common.authentication.oauth.path.AuthenticationEndpointPathResolver;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.quyq.gwsu.common.authentication.oauth.converter.OAuth2DefaultScopeAuthenticationConverter;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2JwkSetResponseWrappingFilter;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2ResponseWriter;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2DeviceVerificationResultHandler;
import org.quyq.gwsu.common.authentication.oauth.security.OAuthDeviceVerificationEntryRequestMatcher;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2UnifiedErrorResponseHandler;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2UnifiedSuccessResponseHandler;
import org.quyq.gwsu.common.authentication.oauth.security.OAuthLoginEntryPoint;
import org.quyq.gwsu.common.authentication.oauth.security.OAuthUserSessionSnapshotResolver;
import org.quyq.gwsu.common.authentication.oauth.security.CustomSecurityContextAuthenticationFilter;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2AccessTokenResponseSuccessHandler;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2DeviceAuthorizationRequestAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * OAuth2.1 授权服务端点安全配置。
 *
 * @author Quyq
 */
@AutoConfiguration(after = OAuthAuthorizationServerConfiguration.class)
@ConditionalOnClass({HttpSecurity.class, RegisteredClientRepository.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
public class OAuthSecurityConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            SecurityUtils securityUtils,
            RegisteredClientRepository registeredClientRepository,
            AuthenticationEndpointPathResolver pathResolver,
            OAuthFrontendEndpointResolver frontendEndpointResolver,
            OAuthAuthorizationViewProviderManager viewProviderManager,
            OAuthUserSessionSnapshotResolver userSessionSnapshotResolver,
            CustomOAuth2AccessTokenResponseSuccessHandler accessTokenResponseSuccessHandler,
            OAuth2UnifiedSuccessResponseHandler successResponseHandler,
            OAuth2UnifiedErrorResponseHandler errorResponseHandler,
            OAuth2ResponseWriter responseWriter,
            ObjectMapper objectMapper) throws Exception {
        String consentContextPath = pathResolver.resolve("/auth/oauth2/consent-context");
        String deviceVerificationPath = pathResolver.resolve("/auth/oauth2/device_verification");
        String jwkSetPath = pathResolver.resolve("/auth/oauth2/jwks");
        OAuthDeviceVerificationEntryRequestMatcher deviceVerificationEntryMatcher =
                new OAuthDeviceVerificationEntryRequestMatcher(deviceVerificationPath);
        OAuth2DeviceVerificationResultHandler deviceVerificationResultHandler =
                new OAuth2DeviceVerificationResultHandler(
                        viewProviderManager, frontendEndpointResolver, deviceVerificationPath);
        http.securityMatcher(pathResolver.resolve("/auth/oauth2/**"), pathResolver.resolve("/.well-known/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(consentContextPath).permitAll()
                        .requestMatchers(deviceVerificationEntryMatcher).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(new CustomSecurityContextAuthenticationFilter(securityUtils, userSessionSnapshotResolver),
                        SecurityContextHolderFilter.class)
                .addFilterBefore(new OAuth2JwkSetResponseWrappingFilter(jwkSetPath, objectMapper, responseWriter),
                        SecurityContextHolderFilter.class)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        new OAuthLoginEntryPoint(frontendEndpointResolver, viewProviderManager)))
                .oauth2AuthorizationServer(oauth2 -> oauth2
                        .clientAuthentication(client -> client
                                .errorResponseHandler(errorResponseHandler))
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestConverter(new OAuth2DefaultScopeAuthenticationConverter(
                                        new OAuth2AuthorizationCodeRequestAuthenticationConverter(),
                                        registeredClientRepository))
                                .consentPage(frontendEndpointResolver.apiUrl(
                                        pathResolver.resolve("/auth/oauth2/loginConsent"))))
                        .tokenEndpoint(token -> token
                                .accessTokenRequestConverter(new OAuth2DefaultScopeAuthenticationConverter(
                                        new OAuth2ClientCredentialsAuthenticationConverter(),
                                        registeredClientRepository))
                                .accessTokenResponseHandler(accessTokenResponseSuccessHandler)
                                .errorResponseHandler(errorResponseHandler))
                        .deviceAuthorizationEndpoint(device -> device
                                .deviceAuthorizationRequestConverter(new OAuth2DefaultScopeAuthenticationConverter(
                                        new OAuth2DeviceAuthorizationRequestAuthenticationConverter(),
                                        registeredClientRepository))
                                .deviceAuthorizationResponseHandler(successResponseHandler)
                                .errorResponseHandler(errorResponseHandler))
                        .deviceVerificationEndpoint(device -> device
                                .consentPage(frontendEndpointResolver.apiUrl(
                                        pathResolver.resolve("/auth/oauth2/loginDeviceConsent")))
                                .deviceVerificationResponseHandler(deviceVerificationResultHandler)
                                .errorResponseHandler(deviceVerificationResultHandler))
                        .tokenIntrospectionEndpoint(introspection -> introspection
                                .introspectionResponseHandler(successResponseHandler)
                                .errorResponseHandler(errorResponseHandler))
                        .tokenRevocationEndpoint(revocation -> revocation
                                .revocationResponseHandler(successResponseHandler)
                                .errorResponseHandler(errorResponseHandler)));
        return http.build();
    }

}

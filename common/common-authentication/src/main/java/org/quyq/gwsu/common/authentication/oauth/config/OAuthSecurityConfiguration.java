package org.quyq.gwsu.common.authentication.oauth.config;

import org.quyq.gwsu.common.authentication.oauth.path.AuthenticationEndpointPathResolver;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

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
            AuthenticationEndpointPathResolver pathResolver,
            OAuthFrontendEndpointResolver frontendEndpointResolver,
            OAuthAuthorizationViewProviderManager viewProviderManager,
            OAuthUserSessionSnapshotResolver userSessionSnapshotResolver,
            CustomOAuth2AccessTokenResponseSuccessHandler accessTokenResponseSuccessHandler) throws Exception {
        http.securityMatcher(pathResolver.resolve("/oauth2/**"), pathResolver.resolve("/.well-known/**"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(new CustomSecurityContextAuthenticationFilter(securityUtils, userSessionSnapshotResolver),
                        SecurityContextHolderFilter.class)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        new OAuthLoginEntryPoint(frontendEndpointResolver, viewProviderManager)))
                .oauth2AuthorizationServer(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization.consentPage(
                                frontendEndpointResolver.apiUrl(pathResolver.resolve("/oauth2/loginConsent"))))
                        .tokenEndpoint(token -> token.accessTokenResponseHandler(accessTokenResponseSuccessHandler)));
        return http.build();
    }

}

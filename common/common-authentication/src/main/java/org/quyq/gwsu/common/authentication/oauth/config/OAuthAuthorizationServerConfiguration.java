package org.quyq.gwsu.common.authentication.oauth.config;

import org.quyq.gwsu.common.authentication.config.AuthenticationConfiguration;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientInfoCache;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientInfoProvider;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthRegisteredClientConverter;
import org.quyq.gwsu.common.authentication.oauth.client.CustomRegisteredClientRepository;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.quyq.gwsu.common.authentication.oauth.frontend.ManagerOAuthAuthorizationViewProvider;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProvider;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.path.AuthenticationEndpointPathResolver;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2ResponseWriter;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2UnifiedErrorResponseHandler;
import org.quyq.gwsu.common.authentication.oauth.response.OAuth2UnifiedSuccessResponseHandler;
import org.quyq.gwsu.common.authentication.oauth.security.OAuthUserSessionSnapshotResolver;
import org.quyq.gwsu.common.authentication.oauth.store.CustomOAuth2AuthorizationService;
import org.quyq.gwsu.common.authentication.oauth.store.RatelOAuth2AuthorizationStore;
import org.quyq.gwsu.common.authentication.oauth.store.RedisOAuth2AuthorizationStore;
import org.quyq.gwsu.common.authentication.oauth.store.OAuth2AuthorizationSerializer;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2AccessTokenGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2AccessTokenResponseSuccessHandler;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2AuthorizationCodeGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2DeviceCodeGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuthSubjectWriter;
import org.quyq.gwsu.common.authentication.oauth.token.CustomOAuth2UserCodeGenerator;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientApi;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * OAuth2.1 授权服务核心配置。
 *
 * @author Quyq
 */
@AutoConfiguration(after = AuthenticationConfiguration.class)
@ConditionalOnClass(RegisteredClientRepository.class)
public class OAuthAuthorizationServerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEndpointPathResolver authenticationEndpointPathResolver() {
        return new AuthenticationEndpointPathResolver();
    }

    @Bean
    public OAuthFrontendEndpointResolver oauthFrontendEndpointResolver() {
        return new OAuthFrontendEndpointResolver();
    }

    @Bean
    public OAuthClientInfoCache oauthClientInfoCache(CacheUtils cacheUtils) {
        return new OAuthClientInfoCache(cacheUtils);
    }

    @Bean
    public OAuthRegisteredClientConverter oauthRegisteredClientConverter() {
        return new OAuthRegisteredClientConverter();
    }

    @Bean
    public OAuthClientInfoProvider oauthClientInfoProvider(
            OAuthClientApi oauthClientApi,
            OAuthClientInfoCache cache) {
        return new OAuthClientInfoProvider(oauthClientApi, cache);
    }

    @Bean
    public OAuthClientAccountTypeResolver oauthClientAccountTypeResolver(OAuthClientInfoProvider clientInfoProvider) {
        return new OAuthClientAccountTypeResolver(clientInfoProvider);
    }

    @Bean
    public OAuthUserSessionSnapshotResolver oauthUserSessionSnapshotResolver(SecurityUtils securityUtils) {
        return new OAuthUserSessionSnapshotResolver(securityUtils);
    }

    @Bean
    public ManagerOAuthAuthorizationViewProvider managerOAuthAuthorizationViewProvider(
            OAuthFrontendEndpointResolver endpointResolver) {
        return new ManagerOAuthAuthorizationViewProvider(endpointResolver);
    }

    @Bean
    public OAuthAuthorizationViewProviderManager oauthAuthorizationViewProviderManager(
            List<OAuthAuthorizationViewProvider> providers,
            OAuthClientAccountTypeResolver accountTypeResolver) {
        return new OAuthAuthorizationViewProviderManager(providers, accountTypeResolver);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            OAuthClientInfoProvider clientInfoProvider,
            OAuthRegisteredClientConverter converter) {
        return new CustomRegisteredClientRepository(clientInfoProvider, converter);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthenticationEndpointPathResolver pathResolver) {
        return AuthorizationServerSettings.builder()
                .authorizationEndpoint(pathResolver.resolve("/auth/oauth2/authorize"))
                .deviceAuthorizationEndpoint(pathResolver.resolve("/auth/oauth2/device_authorization"))
                .deviceVerificationEndpoint(pathResolver.resolve("/auth/oauth2/device_verification"))
                .tokenEndpoint(pathResolver.resolve("/auth/oauth2/token"))
                .tokenRevocationEndpoint(pathResolver.resolve("/auth/oauth2/revoke"))
                .tokenIntrospectionEndpoint(pathResolver.resolve("/auth/oauth2/introspect"))
                .jwkSetEndpoint(pathResolver.resolve("/auth/oauth2/jwks"))
                .build();
    }

    @Bean
    public OAuth2AuthorizationSerializer oauth2AuthorizationSerializer() {
        return new OAuth2AuthorizationSerializer();
    }

    @Bean
    public RatelOAuth2AuthorizationStore ratelOAuth2AuthorizationStore(
            CacheUtils cacheUtils,
            OAuth2AuthorizationSerializer serializer) {
        return new RedisOAuth2AuthorizationStore(cacheUtils, serializer);
    }

    @Bean
    public CustomOAuth2AuthorizationService oauth2AuthorizationService(
            RatelOAuth2AuthorizationStore store,
            SecurityUtils securityUtils) {
        return new CustomOAuth2AuthorizationService(store, securityUtils);
    }

    @Bean
    public OAuth2TokenGenerator<OAuth2Token> oauth2TokenGenerator(
            OAuthClientAccountTypeResolver accountTypeResolver) {
        return new DelegatingOAuth2TokenGenerator(
                new CustomOAuth2AuthorizationCodeGenerator(),
                new CustomOAuth2DeviceCodeGenerator(),
                new CustomOAuth2UserCodeGenerator(),
                new OAuth2RefreshTokenGenerator(),
                new CustomOAuth2AccessTokenGenerator(accountTypeResolver));
    }

    @Bean
    public CustomOAuthSubjectWriter ratelOAuthSubjectWriter(
            CustomOAuth2AuthorizationService authorizationService,
            OAuthClientInfoProvider clientInfoProvider,
            OAuthClientAccountTypeResolver accountTypeResolver) {
        return new CustomOAuthSubjectWriter(authorizationService, clientInfoProvider, accountTypeResolver);
    }

    @Bean
    public CustomOAuth2AccessTokenResponseSuccessHandler oauth2AccessTokenResponseSuccessHandler(
            CustomOAuthSubjectWriter subjectWriter,
            OAuth2UnifiedSuccessResponseHandler successResponseHandler) {
        return new CustomOAuth2AccessTokenResponseSuccessHandler(subjectWriter, successResponseHandler);
    }

    @Bean
    public OAuth2ResponseWriter oauth2ResponseWriter(ObjectMapper objectMapper) {
        return new OAuth2ResponseWriter(objectMapper);
    }

    @Bean
    public OAuth2UnifiedSuccessResponseHandler oauth2UnifiedSuccessResponseHandler(
            OAuth2ResponseWriter responseWriter) {
        return new OAuth2UnifiedSuccessResponseHandler(responseWriter);
    }

    @Bean
    public OAuth2UnifiedErrorResponseHandler oauth2UnifiedErrorResponseHandler(
            OAuth2ResponseWriter responseWriter,
            ObjectMapper objectMapper) {
        return new OAuth2UnifiedErrorResponseHandler(responseWriter, objectMapper);
    }

}

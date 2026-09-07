package org.quyq.gwsu.common.authentication.oauth.config;

import org.quyq.gwsu.common.authentication.config.AuthenticationConfiguration;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientInfoCache;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientInfoProvider;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthRegisteredClientConverter;
import org.quyq.gwsu.common.authentication.oauth.client.RatelRegisteredClientRepository;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;
import org.quyq.gwsu.common.authentication.oauth.frontend.ManagerOAuthAuthorizationViewProvider;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProvider;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthAuthorizationViewProviderManager;
import org.quyq.gwsu.common.authentication.oauth.path.AuthenticationEndpointPathResolver;
import org.quyq.gwsu.common.authentication.oauth.security.OAuthUserSessionSnapshotResolver;
import org.quyq.gwsu.common.authentication.oauth.store.RatelOAuth2AuthorizationService;
import org.quyq.gwsu.common.authentication.oauth.store.RatelOAuth2AuthorizationStore;
import org.quyq.gwsu.common.authentication.oauth.store.RedisOAuth2AuthorizationStore;
import org.quyq.gwsu.common.authentication.oauth.store.OAuth2AuthorizationSerializer;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuth2AccessTokenGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuth2AccessTokenResponseSuccessHandler;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuth2AuthorizationCodeGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuth2DeviceCodeGenerator;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuthSubjectWriter;
import org.quyq.gwsu.common.authentication.oauth.token.RatelOAuth2UserCodeGenerator;
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
        return new RatelRegisteredClientRepository(clientInfoProvider, converter);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthenticationEndpointPathResolver pathResolver) {
        return AuthorizationServerSettings.builder()
                .authorizationEndpoint(pathResolver.resolve("/oauth2/authorize"))
                .deviceAuthorizationEndpoint(pathResolver.resolve("/oauth2/device_authorization"))
                .deviceVerificationEndpoint(pathResolver.resolve("/oauth2/device_verification"))
                .tokenEndpoint(pathResolver.resolve("/oauth2/token"))
                .tokenRevocationEndpoint(pathResolver.resolve("/oauth2/revoke"))
                .tokenIntrospectionEndpoint(pathResolver.resolve("/oauth2/introspect"))
                .jwkSetEndpoint(pathResolver.resolve("/oauth2/jwks"))
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
    public RatelOAuth2AuthorizationService oauth2AuthorizationService(
            RatelOAuth2AuthorizationStore store,
            SecurityUtils securityUtils) {
        return new RatelOAuth2AuthorizationService(store, securityUtils);
    }

    @Bean
    public OAuth2TokenGenerator<OAuth2Token> oauth2TokenGenerator(
            OAuthClientAccountTypeResolver accountTypeResolver) {
        return new DelegatingOAuth2TokenGenerator(
                new RatelOAuth2AuthorizationCodeGenerator(),
                new RatelOAuth2DeviceCodeGenerator(),
                new RatelOAuth2UserCodeGenerator(),
                new OAuth2RefreshTokenGenerator(),
                new RatelOAuth2AccessTokenGenerator(accountTypeResolver));
    }

    @Bean
    public RatelOAuthSubjectWriter ratelOAuthSubjectWriter(
            RatelOAuth2AuthorizationService authorizationService,
            OAuthClientInfoProvider clientInfoProvider,
            OAuthClientAccountTypeResolver accountTypeResolver) {
        return new RatelOAuthSubjectWriter(authorizationService, clientInfoProvider, accountTypeResolver);
    }

    @Bean
    public RatelOAuth2AccessTokenResponseSuccessHandler oauth2AccessTokenResponseSuccessHandler(
            RatelOAuthSubjectWriter subjectWriter) {
        return new RatelOAuth2AccessTokenResponseSuccessHandler(subjectWriter);
    }

}

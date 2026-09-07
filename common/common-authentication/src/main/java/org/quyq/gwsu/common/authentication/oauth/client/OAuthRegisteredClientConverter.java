package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * OAuth 应用配置转换器。
 *
 * @author Quyq
 */
public class OAuthRegisteredClientConverter {

    private static final long DEFAULT_ACCESS_TOKEN_TTL = 7200L;
    private static final long DEFAULT_REFRESH_TOKEN_TTL = 2592000L;
    private static final long DEFAULT_AUTHORIZATION_CODE_TTL = 300L;
    private static final long DEFAULT_DEVICE_CODE_TTL = 300L;

    public RegisteredClient convert(OAuthClientInfoVO vo) {
        AssertUtils.notNull(vo, CommonErrorCode.E04001);
        AssertUtils.hasText(vo.getId(), CommonErrorCode.E04004);
        AssertUtils.hasText(vo.getClientId(), CommonErrorCode.E04004);
        AssertUtils.isTrue(OAuthClientStatus.ENABLED == vo.getStatus(), CommonErrorCode.E03002);

        RegisteredClient.Builder builder = RegisteredClient.withId(vo.getId())
                .clientId(vo.getClientId())
                .clientName(Optional.ofNullable(vo.getClientName()).orElse(vo.getClientId()))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(Boolean.TRUE.equals(vo.getRequireProofKey()))
                        .requireAuthorizationConsent(Boolean.TRUE.equals(vo.getRequireAuthorizationConsent()))
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .authorizationCodeTimeToLive(Duration.ofSeconds(effective(vo.getAuthorizationCodeTtlSeconds(), DEFAULT_AUTHORIZATION_CODE_TTL)))
                        .accessTokenTimeToLive(Duration.ofSeconds(effective(vo.getAccessTokenTtlSeconds(), DEFAULT_ACCESS_TOKEN_TTL)))
                        .refreshTokenTimeToLive(Duration.ofSeconds(effective(vo.getRefreshTokenTtlSeconds(), DEFAULT_REFRESH_TOKEN_TTL)))
                        .deviceCodeTimeToLive(Duration.ofSeconds(effective(vo.getDeviceCodeTtlSeconds(), DEFAULT_DEVICE_CODE_TTL)))
                        .reuseRefreshTokens(Boolean.TRUE.equals(vo.getReuseRefreshTokens()))
                        .build());

        if (StringUtils.hasText(vo.getClientSecret())) {
            builder.clientSecret(vo.getClientSecret());
        }
        if (!CollectionUtils.isEmpty(vo.getScopes())) {
            vo.getScopes().forEach(builder::scope);
        }
        if (!CollectionUtils.isEmpty(vo.getRedirectUris())) {
            vo.getRedirectUris().forEach(builder::redirectUri);
        }
        if (!CollectionUtils.isEmpty(vo.getPostLogoutRedirectUris())) {
            vo.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        }
        if (!CollectionUtils.isEmpty(vo.getAuthorizationGrantTypes())) {
            vo.getAuthorizationGrantTypes().forEach(type -> builder.authorizationGrantType(new AuthorizationGrantType(type.getValue())));
        }
        if (!CollectionUtils.isEmpty(vo.getClientAuthenticationMethods())) {
            vo.getClientAuthenticationMethods().forEach(method -> builder.clientAuthenticationMethod(toMethod(method)));
        }
        return builder.build();
    }

    private ClientAuthenticationMethod toMethod(OAuthClientAuthenticationMethod method) {
        return switch (method) {
            case NONE -> ClientAuthenticationMethod.NONE;
            case CLIENT_SECRET_BASIC -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case CLIENT_SECRET_POST -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
        };
    }

    private long effective(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

}

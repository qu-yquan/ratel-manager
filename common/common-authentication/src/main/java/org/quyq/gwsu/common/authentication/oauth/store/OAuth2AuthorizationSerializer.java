package org.quyq.gwsu.common.authentication.oauth.store;

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * OAuth 授权对象专用序列化器。
 *
 * @author Quyq
 */
public class OAuth2AuthorizationSerializer {

    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String DEVICE_CODE = "device_code";
    private static final String USER_CODE = "user_code";

    private final GenericJacksonJsonRedisSerializer serializer;

    public OAuth2AuthorizationSerializer() {
        ClassLoader classLoader = OAuth2AuthorizationSerializer.class.getClassLoader();
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.springframework.security.")
                .allowIfSubType("org.quyq.gwsu.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubTypeIsArray();
        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(
                classLoader,
                typeValidatorBuilder);
        this.serializer = GenericJacksonJsonRedisSerializer.create(builder -> builder
                .enableDefaultTyping(typeValidatorBuilder.build())
                .customize(mapperBuilder -> mapperBuilder.addModules(securityModules)));
    }

    public String serialize(OAuth2Authorization authorization) {
        StoredAuthorization storedAuthorization = new StoredAuthorization(
                authorization.getId(),
                authorization.getRegisteredClientId(),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantType(),
                authorization.getAuthorizedScopes(),
                authorization.getAttributes(),
                storedTokens(authorization));
        return Base64.getEncoder().encodeToString(serializer.serialize(storedAuthorization));
    }

    public OAuth2Authorization deserialize(String source) {
        StoredAuthorization storedAuthorization = serializer.deserialize(
                Base64.getDecoder().decode(source),
                StoredAuthorization.class);
        if (storedAuthorization == null) {
            return null;
        }

        RegisteredClient registeredClient = RegisteredClient.withId(storedAuthorization.registeredClientId())
                .clientId(storedAuthorization.registeredClientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(storedAuthorization.authorizationGrantType())
                .redirectUri("urn:ratel:oauth2:authorization-storage")
                .build();
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(storedAuthorization.id())
                .principalName(storedAuthorization.principalName())
                .authorizationGrantType(storedAuthorization.authorizationGrantType())
                .authorizedScopes(storedAuthorization.authorizedScopes())
                .attributes(attributes -> attributes.putAll(storedAuthorization.attributes()));
        storedAuthorization.tokens().forEach(storedToken -> builder.token(
                restoreToken(storedToken),
                metadata -> metadata.putAll(storedToken.metadata())));
        return builder.build();
    }

    private List<StoredToken> storedTokens(OAuth2Authorization authorization) {
        return Stream.of(
                        authorization.getToken(OAuth2AuthorizationCode.class),
                        authorization.getAccessToken(),
                        authorization.getRefreshToken(),
                        authorization.getToken(OAuth2DeviceCode.class),
                        authorization.getToken(OAuth2UserCode.class))
                .filter(Objects::nonNull)
                .map(this::storeToken)
                .toList();
    }

    private StoredToken storeToken(OAuth2Authorization.Token<?> tokenHolder) {
        OAuth2Token token = tokenHolder.getToken();
        String type;
        String accessTokenType = null;
        Set<String> scopes = Set.of();
        if (token instanceof OAuth2AuthorizationCode) {
            type = AUTHORIZATION_CODE;
        } else if (token instanceof OAuth2AccessToken accessToken) {
            type = ACCESS_TOKEN;
            accessTokenType = accessToken.getTokenType().getValue();
            scopes = accessToken.getScopes();
        } else if (token instanceof OAuth2RefreshToken) {
            type = REFRESH_TOKEN;
        } else if (token instanceof OAuth2DeviceCode) {
            type = DEVICE_CODE;
        } else if (token instanceof OAuth2UserCode) {
            type = USER_CODE;
        } else {
            throw new IllegalArgumentException("不支持的 OAuth Token 类型: " + token.getClass().getName());
        }
        return new StoredToken(
                type,
                token.getTokenValue(),
                token.getIssuedAt(),
                token.getExpiresAt(),
                accessTokenType,
                scopes,
                new LinkedHashMap<>(tokenHolder.getMetadata()));
    }

    private OAuth2Token restoreToken(StoredToken storedToken) {
        return switch (storedToken.type()) {
            case AUTHORIZATION_CODE -> new OAuth2AuthorizationCode(
                    storedToken.tokenValue(), storedToken.issuedAt(), storedToken.expiresAt());
            case ACCESS_TOKEN -> new OAuth2AccessToken(
                    new OAuth2AccessToken.TokenType(storedToken.accessTokenType()),
                    storedToken.tokenValue(),
                    storedToken.issuedAt(),
                    storedToken.expiresAt(),
                    storedToken.scopes());
            case REFRESH_TOKEN -> new OAuth2RefreshToken(
                    storedToken.tokenValue(), storedToken.issuedAt(), storedToken.expiresAt());
            case DEVICE_CODE -> new OAuth2DeviceCode(
                    storedToken.tokenValue(), storedToken.issuedAt(), storedToken.expiresAt());
            case USER_CODE -> new OAuth2UserCode(
                    storedToken.tokenValue(), storedToken.issuedAt(), storedToken.expiresAt());
            default -> throw new IllegalArgumentException("不支持的 OAuth Token 存储类型: " + storedToken.type());
        };
    }

    private record StoredAuthorization(
            String id,
            String registeredClientId,
            String principalName,
            AuthorizationGrantType authorizationGrantType,
            Set<String> authorizedScopes,
            Map<String, Object> attributes,
            List<StoredToken> tokens) {
    }

    private record StoredToken(
            String type,
            String tokenValue,
            Instant issuedAt,
            Instant expiresAt,
            String accessTokenType,
            Set<String> scopes,
            Map<String, Object> metadata) {
    }

}

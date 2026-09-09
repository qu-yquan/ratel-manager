package org.quyq.gwsu.common.authentication.oauth.store;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 基于 Redis 的 OAuth 授权数据存储。
 *
 * @author Quyq
 */
public class RedisOAuth2AuthorizationStore implements RatelOAuth2AuthorizationStore {

    private static final String AUTHORIZATION_KEY_PREFIX = "authentication:oauth2:authorization:";
    private static final String TOKEN_KEY_PREFIX = "authentication:oauth2:authorization:token:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final CacheUtils cacheUtils;

    private final OAuth2AuthorizationSerializer serializer;

    public RedisOAuth2AuthorizationStore(
            CacheUtils cacheUtils,
            OAuth2AuthorizationSerializer serializer) {
        this.cacheUtils = cacheUtils;
        this.serializer = serializer;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Duration ttl = ttl(authorization);
        cacheUtils.withRebel(() -> {
            OAuth2Authorization previous = deserialize(cacheUtils.get(authKey(authorization.getId())));
            removeReplacedTokenIndexes(previous, authorization);
            cacheUtils.set(authKey(authorization.getId()), serializer.serialize(authorization), ttl);
            tokenValues(authorization).forEach(token -> cacheUtils.set(tokenKey(token), authorization.getId(), ttl));
            return true;
        });
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        cacheUtils.withRebel(() -> {
            cacheUtils.delete(authKey(authorization.getId()));
            tokenValues(authorization).forEach(token -> cacheUtils.delete(tokenKey(token)));
            return true;
        });
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return cacheUtils.withRebel(() -> deserialize(cacheUtils.get(authKey(id))));
    }

    @Override
    public OAuth2Authorization findByToken(String tokenValue, OAuth2TokenType tokenType) {
        return cacheUtils.withRebel(() -> {
            String id = cacheUtils.get(tokenKey(tokenValue));
            if (id == null) {
                return null;
            }
            OAuth2Authorization authorization = deserialize(cacheUtils.get(authKey(id)));
            return matches(authorization, tokenValue, tokenType) ? authorization : null;
        });
    }

    private void removeReplacedTokenIndexes(
            OAuth2Authorization previous,
            OAuth2Authorization current) {
        if (previous == null) {
            return;
        }
        Set<String> replacedTokenValues = new HashSet<>(tokenValues(previous));
        replacedTokenValues.removeAll(tokenValues(current));
        replacedTokenValues.forEach(token -> cacheUtils.deleteIfEquals(
                tokenKey(token), current.getId()));
    }

    private boolean matches(
            OAuth2Authorization authorization,
            String tokenValue,
            OAuth2TokenType tokenType) {
        if (authorization == null || tokenValue == null) {
            return false;
        }
        if (tokenType == null) {
            return tokenValues(authorization).contains(tokenValue);
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            return Objects.equals(tokenValue(authorization.getAccessToken()), tokenValue);
        }
        if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return Objects.equals(tokenValue(authorization.getRefreshToken()), tokenValue);
        }
        return switch (tokenType.getValue()) {
            case OAuth2ParameterNames.CODE, "authorization_code" ->
                    Objects.equals(tokenValue(authorization.getToken(OAuth2AuthorizationCode.class)), tokenValue);
            case OAuth2ParameterNames.DEVICE_CODE ->
                    Objects.equals(tokenValue(authorization.getToken(OAuth2DeviceCode.class)), tokenValue);
            case OAuth2ParameterNames.USER_CODE ->
                    Objects.equals(tokenValue(authorization.getToken(OAuth2UserCode.class)), tokenValue);
            case OAuth2ParameterNames.STATE -> Objects.equals(
                    authorization.getAttribute(OAuth2ParameterNames.STATE), tokenValue);
            default -> false;
        };
    }

    private OAuth2Authorization deserialize(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof String value) {
            return serializer.deserialize(value);
        }
        throw new IllegalStateException("不支持的 OAuth 授权数据存储类型: " + source.getClass().getName());
    }

    private List<String> tokenValues(OAuth2Authorization authorization) {
        return Stream.of(
                        tokenValue(authorization.getToken(OAuth2AuthorizationCode.class)),
                        tokenValue(authorization.getAccessToken()),
                        tokenValue(authorization.getRefreshToken()),
                        tokenValue(authorization.getToken(OAuth2DeviceCode.class)),
                        tokenValue(authorization.getToken(OAuth2UserCode.class)),
                        authorization.getAttribute(OAuth2ParameterNames.STATE)
                )
                .filter(Objects::nonNull)
                .toList();
    }

    private String tokenValue(OAuth2Authorization.Token<?> token) {
        return token == null || token.getToken() == null ? null : token.getToken().getTokenValue();
    }

    private Duration ttl(OAuth2Authorization authorization) {
        Instant expiresAt = Stream.of(
                        expiresAt(authorization.getToken(OAuth2AuthorizationCode.class)),
                        expiresAt(authorization.getAccessToken()),
                        expiresAt(authorization.getRefreshToken()),
                        expiresAt(authorization.getToken(OAuth2DeviceCode.class)),
                        expiresAt(authorization.getToken(OAuth2UserCode.class))
                )
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return DEFAULT_TTL;
        }
        return Duration.between(Instant.now(), expiresAt).plusSeconds(60);
    }

    private Instant expiresAt(OAuth2Authorization.Token<?> token) {
        return token == null || token.getToken() == null ? null : token.getToken().getExpiresAt();
    }

    private String authKey(String id) {
        return AUTHORIZATION_KEY_PREFIX + id;
    }

    private String tokenKey(String tokenValue) {
        return TOKEN_KEY_PREFIX + tokenValue;
    }

}

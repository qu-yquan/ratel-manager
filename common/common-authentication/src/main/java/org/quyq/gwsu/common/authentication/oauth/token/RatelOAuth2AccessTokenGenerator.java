package org.quyq.gwsu.common.authentication.oauth.token;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * 生成与现有 Ratel 安全体系兼容的 OAuth access token。
 *
 * @author Quyq
 */
public class RatelOAuth2AccessTokenGenerator implements OAuth2TokenGenerator<OAuth2AccessToken> {

    private final StringKeyGenerator sessionIdGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32);

    private final OAuthClientAccountTypeResolver accountTypeResolver;

    public RatelOAuth2AccessTokenGenerator(OAuthClientAccountTypeResolver accountTypeResolver) {
        this.accountTypeResolver = accountTypeResolver;
    }

    @Override
    public OAuth2AccessToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return null;
        }
        RegisteredClient registeredClient = context.getRegisteredClient();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        long ttlSeconds = registeredClient.getTokenSettings()
                .getAccessTokenTimeToLive()
                .toSeconds();
        SaLoginParameter loginParameter = SaLoginParameter.create()
                .setTimeout(ttlSeconds)
                .setIsShare(false)
                .setRightNowCreateTokenSession(true);
        StpLogic stpLogic = SaManager.getStpLogic(accountTypeResolver.resolve(registeredClient).name(), true);
        String authorizationId = Optional.ofNullable(context.getAuthorization())
                .map(OAuth2Authorization::getId)
                .orElseGet(sessionIdGenerator::generateKey);
        String loginId = "%s:%s:%s".formatted(
                VisitorType.CLIENT.name(), registeredClient.getClientId(), authorizationId);
        String tokenValue = stpLogic.createLoginSession(loginId, loginParameter);
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                issuedAt,
                expiresAt,
                context.getAuthorizedScopes());
    }

}

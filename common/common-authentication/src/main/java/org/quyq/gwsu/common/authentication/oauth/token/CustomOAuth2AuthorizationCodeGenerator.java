package org.quyq.gwsu.common.authentication.oauth.token;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

/**
 * 授权码生成器。
 *
 * @author Quyq
 */
public class CustomOAuth2AuthorizationCodeGenerator implements OAuth2TokenGenerator<OAuth2AuthorizationCode> {

    private final StringKeyGenerator authorizationCodeGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    @Override
    public OAuth2AuthorizationCode generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null || !"code".equals(context.getTokenType().getValue())) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient()
                .getTokenSettings()
                .getAuthorizationCodeTimeToLive());
        return new OAuth2AuthorizationCode(authorizationCodeGenerator.generateKey(), issuedAt, expiresAt);
    }

}

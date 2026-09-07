package org.quyq.gwsu.common.authentication.oauth.token;

import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * 设备授权用户码生成器。
 *
 * @author Quyq
 */
public class RatelOAuth2UserCodeGenerator implements OAuth2TokenGenerator<OAuth2UserCode> {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public OAuth2UserCode generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null || !"user_code".equals(context.getTokenType().getValue())) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient()
                .getTokenSettings()
                .getDeviceCodeTimeToLive());
        return new OAuth2UserCode(generateCode(), issuedAt, expiresAt);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH + 1);
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i == 4) {
                code.append('-');
            }
            code.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return code.toString();
    }

}

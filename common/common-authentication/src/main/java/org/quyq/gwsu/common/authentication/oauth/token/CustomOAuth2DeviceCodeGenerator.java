package org.quyq.gwsu.common.authentication.oauth.token;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

/**
 * 设备码生成器。
 *
 * @author Quyq
 */
public class CustomOAuth2DeviceCodeGenerator implements OAuth2TokenGenerator<OAuth2DeviceCode> {

    private final StringKeyGenerator deviceCodeGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    @Override
    public OAuth2DeviceCode generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null || !"device_code".equals(context.getTokenType().getValue())) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient()
                .getTokenSettings()
                .getDeviceCodeTimeToLive());
        return new OAuth2DeviceCode(deviceCodeGenerator.generateKey(), issuedAt, expiresAt);
    }

}

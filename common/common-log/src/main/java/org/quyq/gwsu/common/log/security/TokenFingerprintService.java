package org.quyq.gwsu.common.log.security;

import org.quyq.gwsu.common.log.config.properties.LogInfoConfigProperties;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 使用 HMAC-SHA256 生成不可逆、可稳定关联的 Token 指纹。
 */
public class TokenFingerprintService {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    private final String keyVersion;

    public TokenFingerprintService(LogInfoConfigProperties.TokenFingerprintProperties properties) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.keyVersion = properties.keyVersion();
    }

    public TokenFingerprint generate(String token) {
        if (!StringUtils.hasText(token)) {
            return new TokenFingerprint(null, keyVersion);
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return new TokenFingerprint(
                    HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8))),
                    keyVersion);
        } catch (Exception exception) {
            throw new IllegalStateException("生成 Token 指纹失败", exception);
        }
    }
}

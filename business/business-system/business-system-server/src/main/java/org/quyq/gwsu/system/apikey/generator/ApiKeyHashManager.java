package org.quyq.gwsu.system.apikey.generator;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.system.config.properties.ApiKeyProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * API_KEY 摘要管理器
 *
 * @author Quyq
 */
@Component
public class ApiKeyHashManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ApiKeyTokenGenerator apiKeyTokenGenerator;
    private final ApiKeyProperties apiKeyProperties;

    public ApiKeyHashManager(ApiKeyTokenGenerator apiKeyTokenGenerator, ApiKeyProperties apiKeyProperties) {
        this.apiKeyTokenGenerator = apiKeyTokenGenerator;
        this.apiKeyProperties = apiKeyProperties;
    }

    public String hash(String apiKey) {
        String normalizedKey = apiKeyTokenGenerator.normalizeToJwt(apiKey);
        Assert.hasText(normalizedKey, "API_KEY 不能为空");

        String pepper = apiKeyProperties.getPepper();
        Assert.hasText(pepper, CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".system.api-key.pepper 未配置");

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalizedKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("API_KEY 摘要计算失败", ex);
        }
    }

    public Integer version() {
        Integer version = apiKeyProperties.getVersion();
        return version == null ? 1 : version;
    }
}

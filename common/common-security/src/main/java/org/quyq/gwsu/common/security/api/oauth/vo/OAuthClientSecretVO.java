package org.quyq.gwsu.common.security.api.oauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * OAuth 应用密钥返回对象。
 *
 * @author Quyq
 */
@Data
@Schema(description = "OAuth应用密钥")
public class OAuthClientSecretVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "OAuth客户端ID")
    private String clientId;

    @Schema(description = "仅返回一次的客户端明文密钥")
    private String clientSecret;

}

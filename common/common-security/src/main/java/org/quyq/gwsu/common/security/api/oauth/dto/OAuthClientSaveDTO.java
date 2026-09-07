package org.quyq.gwsu.common.security.api.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.List;

/**
 * OAuth 应用保存参数。
 *
 * @author Quyq
 */
@Data
@Schema(description = "OAuth应用保存参数")
public class OAuthClientSaveDTO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "OAuth客户端ID")
    private String clientId;

    @Schema(description = "应用名称")
    private String clientName;

    @Schema(description = "客户端类型")
    private OAuthClientType clientType;

    @Schema(description = "账号类型")
    private AccountType accountType;

    @Schema(description = "状态")
    private OAuthClientStatus status;

    @Schema(description = "客户端认证方式")
    private List<OAuthClientAuthenticationMethod> clientAuthenticationMethods;

    @Schema(description = "授权模式")
    private List<OAuthGrantType> authorizationGrantTypes;

    @Schema(description = "重定向URI白名单")
    private List<String> redirectUris;

    @Schema(description = "退出后重定向URI白名单")
    private List<String> postLogoutRedirectUris;

    @Schema(description = "授权范围")
    private List<String> scopes;

    @Schema(description = "是否要求PKCE")
    private Boolean requireProofKey;

    @Schema(description = "是否要求授权确认")
    private Boolean requireAuthorizationConsent;

    @Schema(description = "Access Token有效期秒")
    private Long accessTokenTtlSeconds;

    @Schema(description = "Refresh Token有效期秒")
    private Long refreshTokenTtlSeconds;

    @Schema(description = "授权码有效期秒")
    private Long authorizationCodeTtlSeconds;

    @Schema(description = "设备码有效期秒")
    private Long deviceCodeTtlSeconds;

    @Schema(description = "是否复用Refresh Token")
    private Boolean reuseRefreshTokens;

    @Schema(description = "备注")
    private String remark;

}

package org.quyq.gwsu.security.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.oauth.utils.OAuthClientTextUtils;

/**
 * OAuth 应用配置表。
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_oauth_client")
@Schema(description = "OAuth应用配置表")
public class SecurityOAuthClient extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "OAuth客户端ID")
    private String clientId;

    @Schema(description = "OAuth客户端密钥密文")
    private String clientSecret;

    @Schema(description = "应用名称")
    private String clientName;

    @Schema(description = "客户端类型")
    private OAuthClientType clientType;

    @Schema(description = "账号类型")
    private AccountType accountType;

    @Schema(description = "状态")
    private OAuthClientStatus status;

    @Schema(description = "客户端认证方式，逗号分隔")
    private String clientAuthenticationMethods;

    @Schema(description = "授权模式，逗号分隔")
    private String authorizationGrantTypes;

    @Schema(description = "重定向URI白名单，逗号分隔")
    private String redirectUris;

    @Schema(description = "退出后重定向URI白名单，逗号分隔")
    private String postLogoutRedirectUris;

    @Schema(description = "授权范围，逗号分隔")
    private String scopes;

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

    public OAuthClientInfoVO toVo() {
        OAuthClientInfoVO vo = new OAuthClientInfoVO();
        vo.setId(id);
        vo.setClientId(clientId);
        vo.setClientSecret(clientSecret);
        vo.setClientName(clientName);
        vo.setClientType(clientType);
        vo.setAccountType(accountType);
        vo.setStatus(status);
        vo.setClientAuthenticationMethods(OAuthClientTextUtils.toClientAuthenticationMethods(clientAuthenticationMethods));
        vo.setAuthorizationGrantTypes(OAuthClientTextUtils.toGrantTypes(authorizationGrantTypes));
        vo.setRedirectUris(OAuthClientTextUtils.toList(redirectUris));
        vo.setPostLogoutRedirectUris(OAuthClientTextUtils.toList(postLogoutRedirectUris));
        vo.setScopes(OAuthClientTextUtils.toList(scopes));
        vo.setRequireProofKey(requireProofKey);
        vo.setRequireAuthorizationConsent(requireAuthorizationConsent);
        vo.setAccessTokenTtlSeconds(accessTokenTtlSeconds);
        vo.setRefreshTokenTtlSeconds(refreshTokenTtlSeconds);
        vo.setAuthorizationCodeTtlSeconds(authorizationCodeTtlSeconds);
        vo.setDeviceCodeTtlSeconds(deviceCodeTtlSeconds);
        vo.setReuseRefreshTokens(reuseRefreshTokens);
        vo.setRemark(remark);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityOAuthClient toDo(OAuthClientSaveDTO dto) {
        SecurityOAuthClient entity = new SecurityOAuthClient();
        entity.setId(dto.getId());
        entity.setClientId(dto.getClientId());
        entity.setClientName(dto.getClientName());
        entity.setClientType(dto.getClientType());
        entity.setAccountType(dto.getAccountType());
        entity.setStatus(dto.getStatus());
        entity.setClientAuthenticationMethods(OAuthClientTextUtils.fromList(dto.getClientAuthenticationMethods()));
        entity.setAuthorizationGrantTypes(OAuthClientTextUtils.fromList(dto.getAuthorizationGrantTypes()));
        entity.setRedirectUris(OAuthClientTextUtils.fromList(dto.getRedirectUris()));
        entity.setPostLogoutRedirectUris(OAuthClientTextUtils.fromList(dto.getPostLogoutRedirectUris()));
        entity.setScopes(OAuthClientTextUtils.fromList(dto.getScopes()));
        entity.setRequireProofKey(dto.getRequireProofKey());
        entity.setRequireAuthorizationConsent(dto.getRequireAuthorizationConsent());
        entity.setAccessTokenTtlSeconds(dto.getAccessTokenTtlSeconds());
        entity.setRefreshTokenTtlSeconds(dto.getRefreshTokenTtlSeconds());
        entity.setAuthorizationCodeTtlSeconds(dto.getAuthorizationCodeTtlSeconds());
        entity.setDeviceCodeTtlSeconds(dto.getDeviceCodeTtlSeconds());
        entity.setReuseRefreshTokens(dto.getReuseRefreshTokens());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    public boolean isConfidential() {
        return OAuthClientType.CONFIDENTIAL == clientType;
    }

}

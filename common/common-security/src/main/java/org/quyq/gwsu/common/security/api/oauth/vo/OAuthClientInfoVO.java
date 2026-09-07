package org.quyq.gwsu.common.security.api.oauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.List;

/**
 * OAuth 应用配置。
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "OAuth应用配置")
public class OAuthClientInfoVO extends ClientInfo {

    private String id;

    private String clientId;

    private String clientSecret;

    private String clientName;

    private OAuthClientType clientType;

    private AccountType accountType;

    private OAuthClientStatus status;

    private List<OAuthClientAuthenticationMethod> clientAuthenticationMethods;

    private List<OAuthGrantType> authorizationGrantTypes;

    private List<String> redirectUris;

    private List<String> postLogoutRedirectUris;

    private List<String> scopes;

    private Boolean requireProofKey;

    private Boolean requireAuthorizationConsent;

    private Long accessTokenTtlSeconds;

    private Long refreshTokenTtlSeconds;

    private Long authorizationCodeTtlSeconds;

    private Long deviceCodeTtlSeconds;

    private Boolean reuseRefreshTokens;

    private String remark;

    /**
     * 创建用于登录主体的客户端信息，不携带客户端凭证。
     *
     * @param source 完整客户端配置
     * @return 脱敏后的客户端信息
     */
    public static OAuthClientInfoVO subjectInfo(OAuthClientInfoVO source) {
        OAuthClientInfoVO target = new OAuthClientInfoVO();
        target.setId(source.getId());
        target.setClientId(source.getClientId());
        target.setClientName(source.getClientName());
        target.setClientType(source.getClientType());
        target.setAccountType(source.getAccountType());
        target.setStatus(source.getStatus());
        target.setClientAuthenticationMethods(copyOf(source.getClientAuthenticationMethods()));
        target.setAuthorizationGrantTypes(copyOf(source.getAuthorizationGrantTypes()));
        target.setRedirectUris(copyOf(source.getRedirectUris()));
        target.setPostLogoutRedirectUris(copyOf(source.getPostLogoutRedirectUris()));
        target.setScopes(copyOf(source.getScopes()));
        target.setRequireProofKey(source.getRequireProofKey());
        target.setRequireAuthorizationConsent(source.getRequireAuthorizationConsent());
        target.setAccessTokenTtlSeconds(source.getAccessTokenTtlSeconds());
        target.setRefreshTokenTtlSeconds(source.getRefreshTokenTtlSeconds());
        target.setAuthorizationCodeTtlSeconds(source.getAuthorizationCodeTtlSeconds());
        target.setDeviceCodeTtlSeconds(source.getDeviceCodeTtlSeconds());
        target.setReuseRefreshTokens(source.getReuseRefreshTokens());
        target.setRemark(source.getRemark());
        target.copyBaseProperties(source);
        return target;
    }

    private static <T> List<T> copyOf(List<T> source) {
        return source == null ? null : List.copyOf(source);
    }

}

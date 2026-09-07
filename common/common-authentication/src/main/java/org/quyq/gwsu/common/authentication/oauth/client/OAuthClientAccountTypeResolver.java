package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;

/**
 * 根据 OAuth 客户端配置解析所属账号体系。
 *
 * @author Quyq
 */
public class OAuthClientAccountTypeResolver {

    private final OAuthClientInfoProvider clientInfoProvider;

    public OAuthClientAccountTypeResolver(OAuthClientInfoProvider clientInfoProvider) {
        this.clientInfoProvider = clientInfoProvider;
    }

    public AccountType resolve(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "OAuth registered client cannot be null");
        return resolve(registeredClient.getClientId());
    }

    public AccountType resolve(String clientId) {
        OAuthClientInfoVO clientInfo = clientInfoProvider.getByClientId(clientId);
        Assert.notNull(clientInfo, "OAuth client configuration cannot be found: " + clientId);
        return clientInfo.getAccountType() == null ? AccountType.MANAGER : clientInfo.getAccountType();
    }

}

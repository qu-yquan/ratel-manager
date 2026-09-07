package org.quyq.gwsu.common.authentication.oauth.frontend;

import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按 OAuth 客户端账号类型选择授权交互页面。
 *
 * @author Quyq
 */
public class OAuthAuthorizationViewProviderManager {

    private final Map<AccountType, OAuthAuthorizationViewProvider> providers;

    private final OAuthClientAccountTypeResolver accountTypeResolver;

    public OAuthAuthorizationViewProviderManager(
            List<OAuthAuthorizationViewProvider> providers,
            OAuthClientAccountTypeResolver accountTypeResolver) {
        this.providers = new EnumMap<>(AccountType.class);
        for (OAuthAuthorizationViewProvider provider : providers) {
            OAuthAuthorizationViewProvider previous = this.providers.put(provider.accountType(), provider);
            Assert.isNull(previous, "OAuth authorization view provider already exists: " + provider.accountType());
        }
        this.accountTypeResolver = accountTypeResolver;
    }

    public OAuthAuthorizationViewProvider resolve(String clientId) {
        AccountType accountType = StringUtils.hasText(clientId)
                ? accountTypeResolver.resolve(clientId)
                : AccountType.MANAGER;
        OAuthAuthorizationViewProvider provider = providers.get(accountType);
        Assert.notNull(provider, "OAuth authorization view provider cannot be found: " + accountType);
        return provider;
    }

}

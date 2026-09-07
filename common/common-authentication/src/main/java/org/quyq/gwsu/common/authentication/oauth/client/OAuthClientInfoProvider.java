package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientApi;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;

/**
 * OAuth 客户端配置查询入口。
 *
 * @author Quyq
 */
public class OAuthClientInfoProvider {

    private final OAuthClientApi oauthClientApi;

    private final OAuthClientInfoCache cache;

    public OAuthClientInfoProvider(OAuthClientApi oauthClientApi, OAuthClientInfoCache cache) {
        this.oauthClientApi = oauthClientApi;
        this.cache = cache;
    }

    public OAuthClientInfoVO getById(String id) {
        return cache.getById(id, () -> FeignUtils.data(oauthClientApi.getById(id)))
                .orElse(null);
    }

    public OAuthClientInfoVO getByClientId(String clientId) {
        return cache.getByClientId(clientId, () -> FeignUtils.data(oauthClientApi.getByClientId(clientId)))
                .orElse(null);
    }

}

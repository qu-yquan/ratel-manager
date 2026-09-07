package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * OAuth 应用配置缓存。
 *
 * @author Quyq
 */
public class OAuthClientInfoCache {

    private static final String ID_KEY_PREFIX = "authentication:oauth2:client:id:";
    private static final String CLIENT_ID_KEY_PREFIX = "authentication:oauth2:client:client-id:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final CacheUtils cacheUtils;

    public OAuthClientInfoCache(CacheUtils cacheUtils) {
        this.cacheUtils = cacheUtils;
    }

    public Optional<OAuthClientInfoVO> getById(String id, Supplier<OAuthClientInfoVO> loader) {
        return get(ID_KEY_PREFIX + id, loader);
    }

    public Optional<OAuthClientInfoVO> getByClientId(String clientId, Supplier<OAuthClientInfoVO> loader) {
        return get(CLIENT_ID_KEY_PREFIX + clientId, loader);
    }

    public void evict(String id, String clientId) {
        cacheUtils.withRebel(() -> {
            cacheUtils.delete(ID_KEY_PREFIX + id);
            cacheUtils.delete(CLIENT_ID_KEY_PREFIX + clientId);
            return true;
        });
    }

    private Optional<OAuthClientInfoVO> get(String key, Supplier<OAuthClientInfoVO> loader) {
        OAuthClientInfoVO cached = cacheUtils.withRebel(() -> cacheUtils.get(key));
        if (cached != null) {
            return Optional.of(cached);
        }
        OAuthClientInfoVO loaded = loader.get();
        if (loaded != null) {
            cacheUtils.withRebel(() -> {
                cacheUtils.set(key, loaded, TTL);
                return true;
            });
        }
        return Optional.ofNullable(loaded);
    }

}

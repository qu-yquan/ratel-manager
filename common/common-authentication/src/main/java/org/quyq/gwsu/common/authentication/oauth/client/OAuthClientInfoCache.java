package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientCacheKeys;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * OAuth 应用配置缓存。
 *
 * @author Quyq
 */
public class OAuthClientInfoCache {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final CacheUtils cacheUtils;

    public OAuthClientInfoCache(CacheUtils cacheUtils) {
        this.cacheUtils = cacheUtils;
    }

    public Optional<OAuthClientInfoVO> getById(String id, Supplier<OAuthClientInfoVO> loader) {
        return get(OAuthClientCacheKeys.byId(id), loader);
    }

    public Optional<OAuthClientInfoVO> getByClientId(String clientId, Supplier<OAuthClientInfoVO> loader) {
        return get(OAuthClientCacheKeys.byClientId(clientId), loader);
    }

    public void evict(String id, String clientId) {
        cacheUtils.withRebel(() -> {
            cacheUtils.delete(OAuthClientCacheKeys.byId(id));
            cacheUtils.delete(OAuthClientCacheKeys.byClientId(clientId));
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

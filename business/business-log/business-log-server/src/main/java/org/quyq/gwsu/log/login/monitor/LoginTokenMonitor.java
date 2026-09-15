package org.quyq.gwsu.log.login.monitor;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * 登录 Token 失效监控注册表。
 *
 * <p>Token 作为永久 ZSet 成员保存，Token 与认证会话的映射保存在永久 Hash 中。
 * OAuth2 刷新时替换同一认证会话的旧 Token，其余记录仅在确认失效或登出后移除。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginTokenMonitor {

    private static final String MONITOR_INDEX_KEY = "log:login:token-monitor:index";

    private static final String MONITOR_METADATA_KEY = "log:login:token-monitor:metadata";

    private static final String AUTHORIZATION_TOKEN_KEY = "log:login:token-monitor:authorization";

    private final CacheUtils cacheUtils;

    public void register(String authorizationId, String token) {
        if (!StringUtils.hasText(authorizationId) || !StringUtils.hasText(token)) {
            return;
        }
        String previousToken = cacheUtils.hGet(AUTHORIZATION_TOKEN_KEY, authorizationId);
        if (StringUtils.hasText(previousToken) && !previousToken.equals(token)) {
            remove(previousToken);
        }
        cacheUtils.hSet(MONITOR_METADATA_KEY, token, authorizationId);
        cacheUtils.hSet(AUTHORIZATION_TOKEN_KEY, authorizationId, token);
        cacheUtils.zAdd(MONITOR_INDEX_KEY, token, System.currentTimeMillis());
        cacheUtils.persist(MONITOR_METADATA_KEY);
        cacheUtils.persist(AUTHORIZATION_TOKEN_KEY);
        cacheUtils.persist(MONITOR_INDEX_KEY);
    }

    public Set<String> getBatch(int batchSize) {
        if (batchSize <= 0) {
            return Collections.emptySet();
        }
        return cacheUtils.zRange(MONITOR_INDEX_KEY, 0, batchSize - 1L);
    }

    public boolean isDue(String token, long now) {
        Double nextCheckTime = cacheUtils.zScore(MONITOR_INDEX_KEY, token);
        return nextCheckTime != null && nextCheckTime <= now;
    }

    public String getAuthorizationId(String token) {
        String metadata = cacheUtils.hGet(MONITOR_METADATA_KEY, token);
        if (!StringUtils.hasText(metadata)) {
            return null;
        }
        int legacyDelimiter = metadata.indexOf('|');
        return legacyDelimiter < 0 ? metadata : metadata.substring(0, legacyDelimiter);
    }

    public void postpone(String token, long nextCheckTime) {
        cacheUtils.zAdd(MONITOR_INDEX_KEY, token, nextCheckTime);
    }

    public void remove(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String authorizationId = getAuthorizationId(token);
        cacheUtils.zRemove(MONITOR_INDEX_KEY, token);
        cacheUtils.hDelete(MONITOR_METADATA_KEY, token);
        if (StringUtils.hasText(authorizationId)) {
            String currentToken = cacheUtils.hGet(AUTHORIZATION_TOKEN_KEY, authorizationId);
            if (token.equals(currentToken)) {
                cacheUtils.hDelete(AUTHORIZATION_TOKEN_KEY, authorizationId);
            }
        }
    }
}

package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class BoundedSkillResourceCache {

    private final int maxEntries;
    private final long ttlMillis;
    private final Map<String, CacheEntry> entries;

    BoundedSkillResourceCache(int maxEntries, Duration ttl) {
        this.maxEntries = maxEntries;
        this.ttlMillis = ttl.toMillis();
        this.entries = new LinkedHashMap<>(16, 0.75F, true);
    }

    synchronized byte[] get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() <= System.currentTimeMillis()) {
            entries.remove(key);
            return null;
        }
        return entry.content().clone();
    }

    synchronized void put(String key, byte[] content) {
        entries.put(key, new CacheEntry(content.clone(), System.currentTimeMillis() + ttlMillis));
        while (entries.size() > maxEntries) {
            entries.remove(entries.keySet().iterator().next());
        }
    }

    private record CacheEntry(byte[] content, long expireAt) {
    }
}

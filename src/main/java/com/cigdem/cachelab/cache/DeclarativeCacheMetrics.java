package com.cigdem.cachelab.cache;

import org.springframework.stereotype.Component;

@Component
public class DeclarativeCacheMetrics {

    private final CacheStatsTracker stats = new CacheStatsTracker();

    public void recordRequest() {
        stats.recordRequest();
    }

    public void recordMiss() {
        stats.recordMiss();
    }

    public long missCount() {
        return stats.missCount();
    }

    public CacheStatsTracker.Snapshot snapshot() {
        return stats.snapshot();
    }

    public void reset() {
        stats.reset();
    }
}

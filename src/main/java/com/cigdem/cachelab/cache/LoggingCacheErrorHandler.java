package com.cigdem.cachelab.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

    private final SpringCacheFailureTracker failureTracker;

    public LoggingCacheErrorHandler(SpringCacheFailureTracker failureTracker) {
        this.failureTracker = failureTracker;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        record("GET", exception, cache, key);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        record("PUT", exception, cache, key);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        record("EVICT", exception, cache, key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        record("CLEAR", exception, cache, "all");
    }

    private void record(String operation, RuntimeException exception, Cache cache, Object key) {
        failureTracker.recordFailure();
        log.warn("SPRING CACHE {} FAILED cache={} key={} -> database fallback will be used where possible: {}",
                operation, cache.getName(), key, exception.getClass().getSimpleName());
    }
}

package com.cigdem.cachelab.cache;

import com.cigdem.cachelab.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeclarativeProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(DeclarativeProductCacheService.class);

    private final DeclarativeProductCacheLoader loader;
    private final DeclarativeCacheMetrics metrics;
    private final SpringCacheFailureTracker failureTracker;

    public DeclarativeProductCacheService(
            DeclarativeProductCacheLoader loader,
            DeclarativeCacheMetrics metrics,
            SpringCacheFailureTracker failureTracker
    ) {
        this.loader = loader;
        this.metrics = metrics;
        this.failureTracker = failureTracker;
    }

    public ProductResponse get(Long productId) {
        return getWithSource(productId).product();
    }

    public CacheLookupResult getWithSource(Long productId) {
        metrics.recordRequest();
        long missesBefore = metrics.missCount();
        long failuresBefore = failureTracker.count();
        ProductResponse product = loader.get(productId);

        if (failureTracker.count() > failuresBefore) {
            return new CacheLookupResult(product, "DATABASE_CACHE_FAILED");
        }

        if (metrics.missCount() == missesBefore) {
            log.info("DECLARATIVE CACHE HIT -> productId={}", productId);
            return new CacheLookupResult(product, "REDIS_HIT");
        }

        return new CacheLookupResult(product, "DATABASE_AND_CACHE_FILL");
    }

    public CacheStatsTracker.Snapshot stats() {
        return metrics.snapshot();
    }

    public void resetStats() {
        metrics.reset();
    }
}

package com.cigdem.cachelab.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidator.class);

    private final ManualProductCacheService manualCache;
    private final LocalProductCacheService localCache;
    private final CacheManager cacheManager;

    public ProductCacheInvalidator(
            ManualProductCacheService manualCache,
            LocalProductCacheService localCache,
            CacheManager cacheManager
    ) {
        this.manualCache = manualCache;
        this.localCache = localCache;
        this.cacheManager = cacheManager;
    }

    public void evictEverywhereAfterCommit(Long productId) {
        runAfterCommit(() -> evictEverywhereNow(productId));
    }

    public void evictEverywhereNow(Long productId) {
        manualCache.evict(productId);
        localCache.evict(productId);
        evictSpringCacheNow(CacheNames.DECLARATIVE_PRODUCT, productId);
        evictSpringCacheNow(CacheNames.SELF_INVOCATION_PRODUCT, productId);
        evictSpringCacheNow(CacheNames.ROLLBACK_PRODUCT, productId);
        log.info("CACHE INVALIDATION -> all Product cache variants, productId={}", productId);
    }

    public void evictDeclarativeNow(Long productId) {
        evictSpringCacheNow(CacheNames.DECLARATIVE_PRODUCT, productId);
    }

    public void evictSelfInvocationNow(Long productId) {
        evictSpringCacheNow(CacheNames.SELF_INVOCATION_PRODUCT, productId);
    }

    public void evictRollbackNow(Long productId) {
        evictSpringCacheNow(CacheNames.ROLLBACK_PRODUCT, productId);
    }

    public void evictRollbackAfterCommit(Long productId) {
        runAfterCommit(() -> evictRollbackNow(productId));
    }

    private void evictSpringCacheNow(String cacheName, Long productId) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache is not configured: " + cacheName);
        }

        try {
            cache.evict(productId);
        } catch (RuntimeException exception) {
            // Cache ana veri kaynagi degildir. Redis yokken DB yazimi yine tamamlanabilsin.
            log.warn("CACHE EVICT FAILED -> cache={}, productId={}, error={}",
                    cacheName, productId, exception.getClass().getSimpleName());
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

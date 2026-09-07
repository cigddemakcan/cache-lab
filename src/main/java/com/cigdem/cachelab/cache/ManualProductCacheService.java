package com.cigdem.cachelab.cache;

import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ManualProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ManualProductCacheService.class);

    private final RedisTemplate<String, ProductResponse> redisTemplate;
    private final ProductDatabaseReader databaseReader;
    private final Duration ttl;
    private final CacheStatsTracker stats = new CacheStatsTracker();

    public ManualProductCacheService(
            RedisTemplate<String, ProductResponse> redisTemplate,
            ProductDatabaseReader databaseReader,
            @Value("${cachelab.cache.product-ttl}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.databaseReader = databaseReader;
        this.ttl = ttl;
    }

    public ProductResponse get(Long productId) {
        return getWithSource(productId).product();
    }

    public CacheLookupResult getWithSource(Long productId) {
        stats.recordRequest();
        String key = CacheNames.manualProductKey(productId);

        try {
            ProductResponse cachedProduct = redisTemplate.opsForValue().get(key);
            if (cachedProduct != null) {
                log.info("MANUAL CACHE HIT -> key={}", key);
                return new CacheLookupResult(cachedProduct, "REDIS_HIT");
            }
        } catch (RuntimeException exception) {
            stats.recordMiss();
            stats.recordFailure();
            log.warn("MANUAL CACHE READ FAILED -> DB fallback, key={}, error={}",
                    key, exception.getClass().getSimpleName());
            return new CacheLookupResult(databaseReader.findById(productId), "DATABASE_FALLBACK");
        }

        stats.recordMiss();
        log.info("MANUAL CACHE MISS -> key={}", key);
        ProductResponse product = databaseReader.findById(productId);

        try {
            redisTemplate.opsForValue().set(key, product, ttl);
            log.info("MANUAL CACHE PUT -> key={}, ttl={}", key, ttl);
        } catch (RuntimeException exception) {
            stats.recordFailure();
            log.warn("MANUAL CACHE WRITE FAILED -> response still comes from DB, key={}, error={}",
                    key, exception.getClass().getSimpleName());
            return new CacheLookupResult(product, "DATABASE_CACHE_WRITE_FAILED");
        }

        return new CacheLookupResult(product, "DATABASE_AND_CACHE_FILL");
    }

    public void put(Long productId, ProductResponse product) {
        redisTemplate.opsForValue().set(CacheNames.manualProductKey(productId), product, ttl);
    }

    public ProductResponse peek(Long productId) {
        return redisTemplate.opsForValue().get(CacheNames.manualProductKey(productId));
    }

    public void evict(Long productId) {
        try {
            redisTemplate.delete(CacheNames.manualProductKey(productId));
        } catch (RuntimeException exception) {
            stats.recordFailure();
            log.warn("MANUAL CACHE EVICT FAILED -> productId={}, error={}",
                    productId, exception.getClass().getSimpleName());
        }
    }

    public CacheStatsTracker.Snapshot stats() {
        return stats.snapshot();
    }

    public void resetStats() {
        stats.reset();
    }
}

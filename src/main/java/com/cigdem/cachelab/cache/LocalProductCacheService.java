package com.cigdem.cachelab.cache;

import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(LocalProductCacheService.class);

    private final ProductDatabaseReader databaseReader;
    private final String instanceId;
    private final Map<Long, ProductResponse> localCache = new ConcurrentHashMap<>();

    public LocalProductCacheService(
            ProductDatabaseReader databaseReader,
            @Value("${cachelab.instance-id}") String instanceId
    ) {
        this.databaseReader = databaseReader;
        this.instanceId = instanceId;
    }

    public LocalCacheLookup get(Long productId) {
        ProductResponse cached = localCache.get(productId);
        if (cached != null) {
            log.info("LOCAL CACHE HIT -> instance={}, productId={}", instanceId, productId);
            return new LocalCacheLookup(instanceId, "LOCAL_MEMORY_HIT", cached);
        }

        log.info("LOCAL CACHE MISS -> instance={}, productId={}", instanceId, productId);
        ProductResponse product = databaseReader.findById(productId);
        localCache.put(productId, product);
        return new LocalCacheLookup(instanceId, "DATABASE_AND_LOCAL_CACHE_FILL", product);
    }

    public void evict(Long productId) {
        localCache.remove(productId);
    }

    public void clear() {
        localCache.clear();
    }

    public record LocalCacheLookup(String instanceId, String source, ProductResponse product) {
    }
}

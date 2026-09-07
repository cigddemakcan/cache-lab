package com.cigdem.cachelab.cache;

import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DeclarativeProductCacheLoader {

    private static final Logger log = LoggerFactory.getLogger(DeclarativeProductCacheLoader.class);

    private final ProductDatabaseReader databaseReader;
    private final DeclarativeCacheMetrics metrics;

    public DeclarativeProductCacheLoader(
            ProductDatabaseReader databaseReader,
            DeclarativeCacheMetrics metrics
    ) {
        this.databaseReader = databaseReader;
        this.metrics = metrics;
    }

    @Cacheable(cacheNames = CacheNames.DECLARATIVE_PRODUCT, key = "#productId")
    public ProductResponse get(Long productId) {
        // Bu satir sadece cache miss'te calisir. Hit'te Spring proxy metodu atlar.
        metrics.recordMiss();
        log.info("DECLARATIVE CACHE MISS -> productId={}, method body is running", productId);
        return databaseReader.findById(productId);
    }
}

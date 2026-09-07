package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.DeclarativeProductCacheService;
import com.cigdem.cachelab.cache.ManualProductCacheService;
import com.cigdem.cachelab.cache.SpringCacheFailureTracker;
import com.cigdem.cachelab.product.ProductDatabaseReader;
import org.springframework.stereotype.Service;

@Service
public class CacheLabMetricsService {

    private final ManualProductCacheService manualCache;
    private final DeclarativeProductCacheService declarativeCache;
    private final SpringCacheFailureTracker springCacheFailureTracker;
    private final ProductDatabaseReader databaseReader;

    public CacheLabMetricsService(
            ManualProductCacheService manualCache,
            DeclarativeProductCacheService declarativeCache,
            SpringCacheFailureTracker springCacheFailureTracker,
            ProductDatabaseReader databaseReader
    ) {
        this.manualCache = manualCache;
        this.declarativeCache = declarativeCache;
        this.springCacheFailureTracker = springCacheFailureTracker;
        this.databaseReader = databaseReader;
    }

    public LabResponses.CacheStats currentStats() {
        return new LabResponses.CacheStats(
                manualCache.stats(),
                declarativeCache.stats(),
                springCacheFailureTracker.count(),
                databaseReader.readCount()
        );
    }

    public void reset() {
        manualCache.resetStats();
        declarativeCache.resetStats();
        springCacheFailureTracker.reset();
        databaseReader.resetReadCount();
    }
}

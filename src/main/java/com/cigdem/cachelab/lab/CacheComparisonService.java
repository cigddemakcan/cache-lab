package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheLookupResult;
import com.cigdem.cachelab.cache.DeclarativeProductCacheService;
import com.cigdem.cachelab.cache.ManualProductCacheService;
import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class CacheComparisonService {

    private final ProductDatabaseReader databaseReader;
    private final ManualProductCacheService manualCache;
    private final DeclarativeProductCacheService declarativeCache;
    private final ProductCacheInvalidator cacheInvalidator;
    private final CacheLabMetricsService metricsService;

    public CacheComparisonService(
            ProductDatabaseReader databaseReader,
            ManualProductCacheService manualCache,
            DeclarativeProductCacheService declarativeCache,
            ProductCacheInvalidator cacheInvalidator,
            CacheLabMetricsService metricsService
    ) {
        this.databaseReader = databaseReader;
        this.manualCache = manualCache;
        this.declarativeCache = declarativeCache;
        this.cacheInvalidator = cacheInvalidator;
        this.metricsService = metricsService;
    }

    public LabResponses.Comparison compare(Long productId) {
        // Her compare cagrisi kendi basina okunabilir bir deney olsun.
        metricsService.reset();

        Measurement<ProductResponse> database = measure(() -> databaseReader.findById(productId));

        manualCache.evict(productId);
        Measurement<CacheLookupResult> manualCold = measure(() -> manualCache.getWithSource(productId));
        Measurement<CacheLookupResult> manualWarm = measure(() -> manualCache.getWithSource(productId));

        cacheInvalidator.evictDeclarativeNow(productId);
        Measurement<CacheLookupResult> declarativeCold =
                measure(() -> declarativeCache.getWithSource(productId));
        Measurement<CacheLookupResult> declarativeWarm =
                measure(() -> declarativeCache.getWithSource(productId));

        return new LabResponses.Comparison(
                productId,
                database.value(),
                timing("DATABASE", "NO_CACHE", database.elapsedMicroseconds()),
                timing("REDIS_TEMPLATE", "COLD", manualCold.elapsedMicroseconds()),
                timing("REDIS_TEMPLATE", "WARM", manualWarm.elapsedMicroseconds()),
                timing("SPRING_CACHE", "COLD", declarativeCold.elapsedMicroseconds()),
                timing("SPRING_CACHE", "WARM", declarativeWarm.elapsedMicroseconds()),
                databaseReader.readCount(),
                "Cold cache DB'ye gider; warm cache Redis'ten doner. Manual ve declarative "
                        + "yaklasimlarin hizindan cok kodlama modeli farklidir."
        );
    }

    public LabResponses.TimedProductRead directDatabase(Long productId) {
        Measurement<ProductResponse> result = measure(() -> databaseReader.findById(productId));
        return new LabResponses.TimedProductRead(
                "DATABASE",
                "NO_CACHE",
                "DATABASE",
                result.elapsedMicroseconds(),
                result.value()
        );
    }

    public LabResponses.TimedProductRead manual(Long productId) {
        Measurement<CacheLookupResult> result = measure(() -> manualCache.getWithSource(productId));
        return new LabResponses.TimedProductRead(
                "REDIS_TEMPLATE",
                cacheState(result.value().source()),
                result.value().source(),
                result.elapsedMicroseconds(),
                result.value().product()
        );
    }

    public LabResponses.TimedProductRead declarative(Long productId) {
        Measurement<CacheLookupResult> result = measure(() -> declarativeCache.getWithSource(productId));
        return new LabResponses.TimedProductRead(
                "SPRING_CACHE",
                cacheState(result.value().source()),
                result.value().source(),
                result.elapsedMicroseconds(),
                result.value().product()
        );
    }

    private String cacheState(String source) {
        return "REDIS_HIT".equals(source) ? "WARM" : "COLD_OR_UNAVAILABLE";
    }

    private LabResponses.Timing timing(String method, String cacheState, long elapsedMicroseconds) {
        return new LabResponses.Timing(method, cacheState, elapsedMicroseconds);
    }

    private <T> Measurement<T> measure(Supplier<T> action) {
        long startedAt = System.nanoTime();
        T value = action.get();
        long elapsed = (System.nanoTime() - startedAt) / 1_000;
        return new Measurement<>(value, elapsed);
    }

    private record Measurement<T>(T value, long elapsedMicroseconds) {
    }
}

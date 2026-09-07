package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.LocalProductCacheService;
import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/cache-lab")
public class CacheLabController {

    private final CacheComparisonService comparisonService;
    private final CacheLabMetricsService metricsService;
    private final CacheTtlInspector ttlInspector;
    private final ProductCacheInvalidator cacheInvalidator;
    private final LocalProductCacheService localCache;
    private final SelfInvocationExperimentService selfInvocationExperiment;
    private final TransactionRollbackExperimentService rollbackExperiment;

    public CacheLabController(
            CacheComparisonService comparisonService,
            CacheLabMetricsService metricsService,
            CacheTtlInspector ttlInspector,
            ProductCacheInvalidator cacheInvalidator,
            LocalProductCacheService localCache,
            SelfInvocationExperimentService selfInvocationExperiment,
            TransactionRollbackExperimentService rollbackExperiment
    ) {
        this.comparisonService = comparisonService;
        this.metricsService = metricsService;
        this.ttlInspector = ttlInspector;
        this.cacheInvalidator = cacheInvalidator;
        this.localCache = localCache;
        this.selfInvocationExperiment = selfInvocationExperiment;
        this.rollbackExperiment = rollbackExperiment;
    }

    @GetMapping("/database/products/{productId}")
    public LabResponses.TimedProductRead database(@PathVariable Long productId) {
        return comparisonService.directDatabase(productId);
    }

    @GetMapping("/manual/products/{productId}")
    public LabResponses.TimedProductRead manual(@PathVariable Long productId) {
        return comparisonService.manual(productId);
    }

    @GetMapping("/declarative/products/{productId}")
    public LabResponses.TimedProductRead declarative(@PathVariable Long productId) {
        return comparisonService.declarative(productId);
    }

    @GetMapping("/local/products/{productId}")
    public LocalProductCacheService.LocalCacheLookup local(@PathVariable Long productId) {
        return localCache.get(productId);
    }

    @GetMapping("/compare/{productId}")
    public LabResponses.Comparison compare(@PathVariable Long productId) {
        return comparisonService.compare(productId);
    }

    @GetMapping("/stats")
    public LabResponses.CacheStats stats() {
        return metricsService.currentStats();
    }

    @PostMapping("/stats/reset")
    public LabResponses.ResetResult resetStats() {
        metricsService.reset();
        return new LabResponses.ResetResult("Cache and database counters reset");
    }

    @GetMapping("/ttl/{productId}")
    public LabResponses.TtlInspection ttl(@PathVariable Long productId) {
        return ttlInspector.inspect(productId);
    }

    @DeleteMapping("/cache/{productId}")
    public LabResponses.CacheEvictionResult evict(@PathVariable Long productId) {
        cacheInvalidator.evictEverywhereNow(productId);
        return new LabResponses.CacheEvictionResult(productId, "All cache variants evicted");
    }

    @PostMapping("/local/clear")
    public LabResponses.ResetResult clearLocalCache() {
        localCache.clear();
        return new LabResponses.ResetResult("Only this application instance's local cache was cleared");
    }

    @GetMapping("/experiments/self-invocation/broken/{productId}")
    public LabResponses.SelfInvocationExperiment brokenSelfInvocation(@PathVariable Long productId) {
        return selfInvocationExperiment.broken(productId);
    }

    @GetMapping("/experiments/self-invocation/fixed/{productId}")
    public LabResponses.SelfInvocationExperiment fixedSelfInvocation(@PathVariable Long productId) {
        return selfInvocationExperiment.fixed(productId);
    }

    @PostMapping("/experiments/rollback/broken/{productId}")
    public LabResponses.TransactionRollbackExperiment brokenRollback(
            @PathVariable Long productId,
            @RequestParam @DecimalMin("0.00") BigDecimal attemptedPrice
    ) {
        return rollbackExperiment.broken(productId, attemptedPrice);
    }

    @PostMapping("/experiments/rollback/fixed/{productId}")
    public LabResponses.TransactionRollbackExperiment fixedRollback(
            @PathVariable Long productId,
            @RequestParam @DecimalMin("0.00") BigDecimal attemptedPrice
    ) {
        return rollbackExperiment.fixed(productId, attemptedPrice);
    }
}

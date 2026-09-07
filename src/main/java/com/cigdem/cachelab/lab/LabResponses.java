package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheStatsTracker;
import com.cigdem.cachelab.product.ProductResponse;

import java.math.BigDecimal;

public final class LabResponses {

    private LabResponses() {
    }

    public record TimedProductRead(
            String method,
            String cacheState,
            String source,
            long elapsedMicroseconds,
            ProductResponse product
    ) {
    }

    public record Timing(
            String method,
            String cacheState,
            long elapsedMicroseconds
    ) {
    }

    public record Comparison(
            Long productId,
            ProductResponse product,
            Timing databaseWithoutCache,
            Timing manualCold,
            Timing manualWarm,
            Timing declarativeCold,
            Timing declarativeWarm,
            long databaseReadsDuringComparison,
            String conclusion
    ) {
    }

    public record CacheStats(
            CacheStatsTracker.Snapshot manual,
            CacheStatsTracker.Snapshot declarative,
            long springCacheOperationFailures,
            long databaseReads
    ) {
    }

    public record TtlEntry(
            String key,
            Long remainingSeconds,
            String state
    ) {
    }

    public record TtlInspection(
            Long productId,
            TtlEntry manual,
            TtlEntry declarative
    ) {
    }

    public record SelfInvocationExperiment(
            String mode,
            ProductResponse product,
            long firstCallMicroseconds,
            long secondCallMicroseconds,
            long databaseReadsDuringExperiment,
            String explanation
    ) {
    }

    public record TransactionRollbackExperiment(
            String mode,
            Long productId,
            BigDecimal originalDatabasePrice,
            BigDecimal attemptedPrice,
            BigDecimal databasePriceAfterRollback,
            BigDecimal cachedPriceAfterRollback,
            boolean staleCache,
            String explanation
    ) {
    }

    public record CacheEvictionResult(Long productId, String result) {
    }

    public record ResetResult(String result) {
    }
}

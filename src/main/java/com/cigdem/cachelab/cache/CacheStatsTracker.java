package com.cigdem.cachelab.cache;

import java.util.concurrent.atomic.LongAdder;

public final class CacheStatsTracker {

    private final LongAdder requests = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder failures = new LongAdder();

    public void recordRequest() {
        requests.increment();
    }

    public void recordMiss() {
        misses.increment();
    }

    public void recordFailure() {
        failures.increment();
    }

    public long missCount() {
        return misses.sum();
    }

    public long failureCount() {
        return failures.sum();
    }

    public Snapshot snapshot() {
        long requestCount = requests.sum();
        long missCount = misses.sum();
        long hitCount = Math.max(0, requestCount - missCount);
        double hitRate = requestCount == 0
                ? 0.0
                : Math.round((hitCount * 10000.0) / requestCount) / 100.0;

        return new Snapshot(requestCount, hitCount, missCount, failures.sum(), hitRate);
    }

    public void reset() {
        requests.reset();
        misses.reset();
        failures.reset();
    }

    public record Snapshot(
            long requests,
            long hits,
            long misses,
            long failures,
            double hitRatePercent
    ) {
    }
}

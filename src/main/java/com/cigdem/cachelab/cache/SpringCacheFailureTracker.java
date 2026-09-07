package com.cigdem.cachelab.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
public class SpringCacheFailureTracker {

    private final LongAdder failures = new LongAdder();

    public void recordFailure() {
        failures.increment();
    }

    public long count() {
        return failures.sum();
    }

    public void reset() {
        failures.reset();
    }
}

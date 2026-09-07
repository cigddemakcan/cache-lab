package com.cigdem.cachelab.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheStatsTrackerTest {

    @Test
    void calculatesHitRateFromRequestsAndMisses() {
        CacheStatsTracker tracker = new CacheStatsTracker();
        tracker.recordRequest();
        tracker.recordMiss();
        tracker.recordRequest();
        tracker.recordRequest();
        tracker.recordRequest();

        CacheStatsTracker.Snapshot snapshot = tracker.snapshot();

        assertThat(snapshot.requests()).isEqualTo(4);
        assertThat(snapshot.misses()).isEqualTo(1);
        assertThat(snapshot.hits()).isEqualTo(3);
        assertThat(snapshot.hitRatePercent()).isEqualTo(75.0);
    }

    @Test
    void resetClearsEveryCounter() {
        CacheStatsTracker tracker = new CacheStatsTracker();
        tracker.recordRequest();
        tracker.recordMiss();
        tracker.recordFailure();

        tracker.reset();

        assertThat(tracker.snapshot())
                .isEqualTo(new CacheStatsTracker.Snapshot(0, 0, 0, 0, 0.0));
    }
}

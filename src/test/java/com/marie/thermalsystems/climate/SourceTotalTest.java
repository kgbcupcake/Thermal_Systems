package com.marie.thermalsystems.climate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceTotalTest {

    private record Source(double output, Object network) {
    }

    private static ClimateEngine.SourceTotal total(List<Source> sources) {
        return ClimateEngine.total(sources, Source::output, Source::network);
    }

    @Test
    void oneNetworkBoundAtManyConduitsCountsOnce() {
        List<Source> sources = new ArrayList<>();
        sources.add(new Source(75.0, "network"));
        for (int i = 0; i < 6; i++) {
            sources.add(new Source(75.0, "network"));
        }

        ClimateEngine.SourceTotal total = total(sources);

        assertEquals(1, total.count());
        assertEquals(75.0, total.output(), 1e-9);
    }

    @Test
    void networkCountsAtItsLargestReportedValue() {
        // One generator on a two-generator network reports only its own share.
        ClimateEngine.SourceTotal total = total(List.of(new Source(75.0, "network"), new Source(150.0, "network")));

        assertEquals(150.0, total.output(), 1e-9);
    }

    @Test
    void standaloneSourcesAndSeparateNetworksAllCount() {
        ClimateEngine.SourceTotal total = total(List.of(
                new Source(10.0, null), new Source(20.0, null), new Source(30.0, "a"), new Source(40.0, "b")));

        assertEquals(4, total.count());
        assertEquals(100.0, total.output(), 1e-9);
    }
}

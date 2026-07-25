package com.marie.thermalsystems.integration.mekanism;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link MekanismNetworkSum} sums a fake list of already-collected
 * outputs correctly, entirely independent of {@link MekanismNetworkDiscovery}
 * or any real Mekanism/world state - the actual network query is exercised
 * in-game per the Testing section of the network-binding spec.
 */
class MekanismNetworkSumTest {

    @Test
    void sumsMultipleOutputs() {
        assertEquals(30.0, MekanismNetworkSum.sum(List.of(10.0, 5.0, 15.0)), 1e-9);
    }

    @Test
    void sumOfEmptyListIsZero() {
        assertEquals(0.0, MekanismNetworkSum.sum(List.of()), 1e-9);
    }

    @Test
    void sumHandlesNegativeOutputs() {
        assertEquals(-5.0, MekanismNetworkSum.sum(List.of(10.0, -15.0)), 1e-9);
    }
}

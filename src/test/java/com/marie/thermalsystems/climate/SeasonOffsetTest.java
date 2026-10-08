package com.marie.thermalsystems.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonOffsetTest {

    @Test
    void winterBiomeDeltaBecomesColderCelsius() {
        assertEquals(-13.5, SeasonOffset.fromBiomeDelta(0.35, 0.8), 1e-9);
    }

    @Test
    void summerBiomeDeltaBecomesWarmerCelsius() {
        assertEquals(7.5, SeasonOffset.fromBiomeDelta(1.05, 0.8), 1e-9);
    }

    @Test
    void nonFiniteDeltaContributesNothing() {
        assertEquals(0.0, SeasonOffset.fromBiomeDelta(Double.NaN, 0.8), 0.0);
        assertEquals(0.0, SeasonOffset.fromBiomeDelta(0.8, Double.POSITIVE_INFINITY), 0.0);
    }
}

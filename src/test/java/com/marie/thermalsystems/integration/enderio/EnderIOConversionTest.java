package com.marie.thermalsystems.integration.enderio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderIOConversionTest {

    @Test
    void producesNoHeatWhenNoEnergyReceived() {
        double heatOutput = EnderIOConversion.convert(0L, 0.001);

        assertEquals(0.0, heatOutput, 1e-9);
    }

    @Test
    void scalesEnergyReceivedByConversionCoefficient() {
        double heatOutput = EnderIOConversion.convert(4000L, 0.001);

        assertEquals(4000L * 0.001, heatOutput, 1e-9);
    }

    @Test
    void producesMoreHeatWithHigherThroughput() {
        double low = EnderIOConversion.convert(1000L, 0.001);
        double high = EnderIOConversion.convert(8000L, 0.001);

        assertTrue(high > low);
    }

    @Test
    void rejectsNegativeEnergyReceived() {
        assertThrows(IllegalArgumentException.class,
                () -> EnderIOConversion.convert(-1L, 0.001));
    }

    @Test
    void rejectsNegativeConversionCoefficient() {
        assertThrows(IllegalArgumentException.class,
                () -> EnderIOConversion.convert(1000L, -0.001));
    }

    @Test
    void rejectsNaNConversionCoefficient() {
        assertThrows(IllegalArgumentException.class,
                () -> EnderIOConversion.convert(1000L, Double.NaN));
    }

    @Test
    void rejectsInfiniteConversionCoefficient() {
        assertThrows(IllegalArgumentException.class,
                () -> EnderIOConversion.convert(1000L, Double.POSITIVE_INFINITY));
    }
}

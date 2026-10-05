package com.marie.thermalsystems.integration.enderio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderIOConversionTest {

    @Test
    void energyToHeatScalesFillFractionByCoefficient() {
        assertEquals(30.0, EnderIOConversion.energyToHeat(5000L, 10000L, 60.0), 1e-9);
    }

    @Test
    void energyToHeatAtFullBufferEqualsCoefficient() {
        assertEquals(60.0, EnderIOConversion.energyToHeat(10000L, 10000L, 60.0), 1e-9);
    }

    @Test
    void energyToHeatOfZeroEnergyIsZero() {
        assertEquals(0.0, EnderIOConversion.energyToHeat(0L, 10000L, 60.0), 1e-9);
    }

    @Test
    void energyToHeatOfZeroCapacityIsZero() {
        assertEquals(0.0, EnderIOConversion.energyToHeat(0L, 0L, 60.0), 1e-9);
    }

    @Test
    void energyToHeatRejectsNegativeEnergy() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(-1L, 10000L, 60.0));
    }

    @Test
    void energyToHeatClampsEnergyAboveCapacityToFullBuffer() {
        assertEquals(60.0, EnderIOConversion.energyToHeat(20000L, 10000L, 60.0), 1e-9);
    }

    @Test
    void energyToHeatRejectsInvalidCoefficient() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(1000L, 10000L, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(1000L, 10000L, -0.001));
    }
}

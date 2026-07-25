package com.marie.thermalsystems.integration.enderio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderIOConversionTest {

    @Test
    void energyToHeatScalesEnergyByCoefficient() {
        assertEquals(5.0, EnderIOConversion.energyToHeat(5000L, 0.001), 1e-9);
    }

    @Test
    void energyToHeatOfZeroEnergyIsZero() {
        assertEquals(0.0, EnderIOConversion.energyToHeat(0L, 0.001), 1e-9);
    }

    @Test
    void energyToHeatRejectsNegativeEnergy() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(-1L, 0.001));
    }

    @Test
    void energyToHeatRejectsInvalidCoefficient() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(1000L, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.energyToHeat(1000L, -0.001));
    }
}

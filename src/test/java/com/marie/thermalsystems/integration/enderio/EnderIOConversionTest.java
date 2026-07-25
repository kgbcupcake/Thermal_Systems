package com.marie.thermalsystems.integration.enderio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderIOConversionTest {

    @Test
    void fluidToHeatScalesAmountByCoefficient() {
        assertEquals(10.0, EnderIOConversion.fluidToHeat(1000, 0.01), 1e-9);
    }

    @Test
    void fluidToHeatOfZeroAmountIsZero() {
        assertEquals(0.0, EnderIOConversion.fluidToHeat(0, 0.01), 1e-9);
    }

    @Test
    void fluidToHeatRejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.fluidToHeat(-1, 0.01));
    }

    @Test
    void fluidToHeatRejectsInvalidCoefficient() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.fluidToHeat(100, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.fluidToHeat(100, -0.01));
    }

    @Test
    void heatToFluidAmountIsTheInverseOfFluidToHeat() {
        int amount = EnderIOConversion.heatToFluidAmount(10.0, 0.01);
        assertEquals(1000, amount);
    }

    @Test
    void heatToFluidAmountWithZeroCoefficientIsZero() {
        assertEquals(0, EnderIOConversion.heatToFluidAmount(10.0, 0.0));
    }

    @Test
    void heatToFluidAmountRejectsInvalidHeat() {
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.heatToFluidAmount(-1.0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> EnderIOConversion.heatToFluidAmount(Double.NaN, 0.01));
    }

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

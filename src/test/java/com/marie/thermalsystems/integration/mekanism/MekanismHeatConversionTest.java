package com.marie.thermalsystems.integration.mekanism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MekanismHeatConversionTest {

    @Test
    void producesHeatWhenHotterThanReference() {
        MekanismHeatConversion.Output output = MekanismHeatConversion.convert(450.0, 300.0, 0.05);

        assertTrue(output.heatOutput() > 0);
        assertEquals(0.0, output.coolingOutput(), 1e-9);
        assertEquals((450.0 - 300.0) * 0.05, output.heatOutput(), 1e-9);
    }

    @Test
    void producesCoolingWhenColderThanReference() {
        MekanismHeatConversion.Output output = MekanismHeatConversion.convert(150.0, 300.0, 0.05);

        assertTrue(output.coolingOutput() > 0);
        assertEquals(0.0, output.heatOutput(), 1e-9);
        assertEquals((300.0 - 150.0) * 0.05, output.coolingOutput(), 1e-9);
    }

    @Test
    void producesNeitherWhenExactlyAtReference() {
        MekanismHeatConversion.Output output = MekanismHeatConversion.convert(300.0, 300.0, 0.05);

        assertEquals(0.0, output.heatOutput(), 1e-9);
        assertEquals(0.0, output.coolingOutput(), 1e-9);
    }

    @Test
    void heatAndCoolingAreNeverBothNonZero() {
        MekanismHeatConversion.Output hot = MekanismHeatConversion.convert(600.0, 300.0, 0.05);
        MekanismHeatConversion.Output cold = MekanismHeatConversion.convert(100.0, 300.0, 0.05);

        assertTrue(hot.heatOutput() > 0 && hot.coolingOutput() == 0.0);
        assertTrue(cold.coolingOutput() > 0 && cold.heatOutput() == 0.0);
    }

    @Test
    void rejectsNaNMekanismTemperature() {
        assertThrows(IllegalArgumentException.class,
                () -> MekanismHeatConversion.convert(Double.NaN, 300.0, 0.05));
    }

    @Test
    void rejectsInfiniteReferenceTemperature() {
        assertThrows(IllegalArgumentException.class,
                () -> MekanismHeatConversion.convert(400.0, Double.POSITIVE_INFINITY, 0.05));
    }

    @Test
    void rejectsInfiniteConversionCoefficient() {
        assertThrows(IllegalArgumentException.class,
                () -> MekanismHeatConversion.convert(400.0, 300.0, Double.NEGATIVE_INFINITY));
    }
}

package com.marie.thermalsystems.integration.pneumaticcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalExchangerConversionTest {

    @Test
    void producesHeatWhenExchangerIsHotterThanReference() {
        ThermalExchangerConversion.Output output = ThermalExchangerConversion.convert(320.0, 293.15, 0.05);

        assertTrue(output.heatOutput() > 0);
        assertEquals(0.0, output.coolingOutput(), 1e-9);
        assertEquals((320.0 - 293.15) * 0.05, output.heatOutput(), 1e-9);
    }

    @Test
    void producesCoolingWhenExchangerIsColderThanReference() {
        ThermalExchangerConversion.Output output = ThermalExchangerConversion.convert(250.0, 293.15, 0.05);

        assertTrue(output.coolingOutput() > 0);
        assertEquals(0.0, output.heatOutput(), 1e-9);
        assertEquals((293.15 - 250.0) * 0.05, output.coolingOutput(), 1e-9);
    }

    @Test
    void producesNeitherWhenExactlyAtReference() {
        ThermalExchangerConversion.Output output = ThermalExchangerConversion.convert(293.15, 293.15, 0.05);

        assertEquals(0.0, output.heatOutput(), 1e-9);
        assertEquals(0.0, output.coolingOutput(), 1e-9);
    }

    @Test
    void heatAndCoolingAreNeverBothNonZero() {
        ThermalExchangerConversion.Output hot = ThermalExchangerConversion.convert(400.0, 293.15, 0.05);
        ThermalExchangerConversion.Output cold = ThermalExchangerConversion.convert(200.0, 293.15, 0.05);

        assertTrue(hot.heatOutput() > 0 && hot.coolingOutput() == 0.0);
        assertTrue(cold.coolingOutput() > 0 && cold.heatOutput() == 0.0);
    }

    @Test
    void rejectsNaNExchangerTemperature() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalExchangerConversion.convert(Double.NaN, 293.15, 0.05));
    }

    @Test
    void rejectsInfiniteReferenceTemperature() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalExchangerConversion.convert(300.0, Double.POSITIVE_INFINITY, 0.05));
    }

    @Test
    void rejectsInfiniteConversionCoefficient() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalExchangerConversion.convert(300.0, 293.15, Double.NEGATIVE_INFINITY));
    }
}

package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.data.config.ThermalConfig;
import mekanism.api.heat.IHeatHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link MekanismBlockHeatSource} reads a Mekanism block entity's
 * own native heat state and passes it through {@link MekanismHeatConversion}
 * unmodified, using a minimal in-test stub {@link IHeatHandler} - never
 * registered with NeoForge, never present in a running game - rather than a
 * reintroduction of the removed exchanger block. Position-based capability
 * resolution against a real Mekanism block entity requires Mekanism itself
 * to be present and is exercised in-game per the Testing section of the
 * Phase 4/5 correction spec.
 */
class MekanismBlockHeatSourceTest {

    private static final class StubHeatHandler implements IHeatHandler {

        private final double temperature;

        StubHeatHandler(double temperature) {
            this.temperature = temperature;
        }

        @Override
        public int getHeatCapacitorCount() {
            return 1;
        }

        @Override
        public double getTemperature(int capacitor) {
            return temperature;
        }

        @Override
        public double getInverseConduction(int capacitor) {
            return 1.0;
        }

        @Override
        public double getHeatCapacity(int capacitor) {
            return 1.0;
        }

        @Override
        public void handleHeat(int capacitor, double transfer) {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }

    @Test
    void hotHandlerProducesHeatOnly() {
        double referenceTemperature = ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get();
        StubHeatHandler hot = new StubHeatHandler(referenceTemperature + 50.0);

        MekanismHeatConversion.Output output = MekanismBlockHeatSource.convert(hot);

        assertEquals(50.0 * conversionCoefficient, output.heatOutput(), 1e-9);
        assertEquals(0.0, output.coolingOutput());
    }

    @Test
    void coldHandlerProducesCoolingOnly() {
        double referenceTemperature = ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get();
        StubHeatHandler cold = new StubHeatHandler(referenceTemperature - 50.0);

        MekanismHeatConversion.Output output = MekanismBlockHeatSource.convert(cold);

        assertEquals(0.0, output.heatOutput());
        assertEquals(50.0 * conversionCoefficient, output.coolingOutput(), 1e-9);
    }
}

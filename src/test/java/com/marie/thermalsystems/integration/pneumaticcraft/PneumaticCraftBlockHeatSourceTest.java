package com.marie.thermalsystems.integration.pneumaticcraft;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link PneumaticCraftBlockHeatSource} reads a PNC:R block
 * entity's own native heat state and passes it through
 * {@link ThermalExchangerConversion} unmodified, using a minimal in-test
 * stub {@link IHeatExchangerLogic} - never registered with NeoForge, never
 * present in a running game - rather than a reintroduction of the removed
 * exchanger block. Position-based capability resolution against a real
 * PNC:R block entity requires PNC:R itself to be present and is exercised
 * in-game per the Testing section of the Phase 4/5 correction spec.
 */
class PneumaticCraftBlockHeatSourceTest {

    private static final class StubHeatExchangerLogic implements IHeatExchangerLogic {

        private double temperature;

        StubHeatExchangerLogic(double temperature) {
            this.temperature = temperature;
        }

        @Override
        public void tick() {
        }

        @Override
        public void initializeAsHull(Level level, BlockPos pos, BiPredicate<LevelAccessor, BlockPos> filter, Direction... directions) {
        }

        @Override
        public void initializeAmbientTemperature(Level level, BlockPos pos) {
        }

        @Override
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        @Override
        public double getTemperature() {
            return temperature;
        }

        @Override
        public int getTemperatureAsInt() {
            return (int) temperature;
        }

        @Override
        public double getAmbientTemperature() {
            return temperature;
        }

        @Override
        public void setThermalResistance(double resistance) {
        }

        @Override
        public double getThermalResistance() {
            return 1.0;
        }

        @Override
        public void setThermalCapacity(double capacity) {
        }

        @Override
        public double getThermalCapacity() {
            return 1.0;
        }

        @Override
        public void addHeat(double heat) {
        }

        @Override
        public boolean isSideConnected(Direction direction) {
            return true;
        }
    }

    @Test
    void hotLogicProducesHeatOnly() {
        double referenceTemperature = ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get();
        StubHeatExchangerLogic hot = new StubHeatExchangerLogic(referenceTemperature + 50.0);

        ThermalExchangerConversion.Output output = PneumaticCraftBlockHeatSource.convert(hot);

        assertEquals(50.0 * conversionCoefficient, output.heatOutput(), 1e-9);
        assertEquals(0.0, output.coolingOutput());
    }

    @Test
    void coldLogicProducesCoolingOnly() {
        double referenceTemperature = ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get();
        StubHeatExchangerLogic cold = new StubHeatExchangerLogic(referenceTemperature - 50.0);

        ThermalExchangerConversion.Output output = PneumaticCraftBlockHeatSource.convert(cold);

        assertEquals(0.0, output.heatOutput());
        assertEquals(50.0 * conversionCoefficient, output.coolingOutput(), 1e-9);
    }
}

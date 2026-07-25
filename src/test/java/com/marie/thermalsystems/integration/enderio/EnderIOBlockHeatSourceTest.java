package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.data.config.ThermalConfig;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link EnderIOBlockHeatSource} reads a Stirling Generator's own
 * native {@link IEnergyStorage} and passes it through
 * {@link EnderIOConversion} unmodified, using a minimal in-test stub
 * {@link IEnergyStorage} - never registered with NeoForge, never present in
 * a running game - rather than a reintroduction of the removed Boiler.
 * Position-based capability resolution against a real Ender IO block entity
 * requires Ender IO itself to be present and is exercised in-game per the
 * Testing section of the no-blocks correction spec.
 */
class EnderIOBlockHeatSourceTest {

    private static final class StubEnergyStorage implements IEnergyStorage {

        private final int energyStored;

        StubEnergyStorage(int energyStored) {
            this.energyStored = energyStored;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    @Test
    void storedEnergyProducesHeatScaledByCoefficient() {
        double coefficient = ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get();
        StubEnergyStorage storage = new StubEnergyStorage(5000);

        double heatOutput = EnderIOBlockHeatSource.convert(storage);

        assertEquals(5000 * coefficient, heatOutput, 1e-9);
    }

    @Test
    void emptyStorageProducesNoHeat() {
        StubEnergyStorage storage = new StubEnergyStorage(0);

        double heatOutput = EnderIOBlockHeatSource.convert(storage);

        assertEquals(0.0, heatOutput, 1e-9);
    }
}

package com.marie.thermalsystems.integration.enderio.blockentity;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.integration.enderio.EnderIOConversion;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Translation boundary between an Ender IO Energy Conduit network and a
 * Thermal Systems climate zone. Not a battery: it holds no energy buffer,
 * only a per-tick counter of FE received. Ender IO's Energy Conduit
 * discovers this block the same way it discovers any energy-consuming
 * machine - by finding the standard NeoForge {@code IEnergyStorage}
 * capability here (see {@link EnderIOIntegration} for the registration and
 * {@link EnderIOConversion} for why this, not a fictional Ender IO heat
 * capability, is the real integration surface). Every tick, whatever FE was
 * received is converted fresh into heat output and discarded - this never
 * becomes a second energy storage system.
 */
public class EnderIOThermalAdapterBlockEntity extends BlockEntity implements IHeatSource, IEnergyStorage {

    private long energyReceivedThisTick;
    private double heatOutput;

    public EnderIOThermalAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(EnderIOIntegration.THERMAL_ADAPTER_BLOCK_ENTITY.get(), pos, state);
    }

    public void serverTick() {
        double conversionCoefficient = ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get();
        heatOutput = EnderIOConversion.convert(energyReceivedThisTick, conversionCoefficient);
        energyReceivedThisTick = 0;
    }

    @Override
    public double getHeatOutput() {
        return heatOutput;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }
        long cap = ThermalConfig.ENDERIO_MAX_ENERGY_RECEIVED_PER_TICK.get();
        long available = Math.max(0, cap - energyReceivedThisTick);
        int accepted = (int) Math.min(maxReceive, available);
        if (!simulate && accepted > 0) {
            energyReceivedThisTick += accepted;
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(energyReceivedThisTick, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return ThermalConfig.ENDERIO_MAX_ENERGY_RECEIVED_PER_TICK.get();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            ThermalSystemsAPI.unbindHeatSource(level, worldPosition);
        }
    }
}

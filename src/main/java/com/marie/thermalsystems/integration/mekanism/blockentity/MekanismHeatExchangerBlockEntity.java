package com.marie.thermalsystems.integration.mekanism.blockentity;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.integration.mekanism.MekanismHeatConversion;
import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import mekanism.api.IContentsListener;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IMekanismHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Translation boundary between a Mekanism heat network and a Thermal Systems
 * climate zone. Exposes itself to Mekanism as an {@link IMekanismHeatHandler}
 * (via {@link MekanismIntegration}'s reconstruction of Mekanism's own
 * {@code mekanism:heat_handler} capability token) holding a single
 * {@link AdapterHeatCapacitor}, and to Thermal Systems as an
 * {@link IHeatSource}/{@link ICoolingSource} via this mod's own capability
 * API. The capacitor's stored heat is not a second heat simulation - it is
 * the mandatory storage every Mekanism heat handler must expose so
 * Mekanism's own heat network can conduct heat into and out of this block
 * exactly as it would any other capacitor. Every evaluation reads the
 * capacitor's live temperature and converts it fresh through
 * {@link MekanismHeatConversion}.
 */
public class MekanismHeatExchangerBlockEntity extends BlockEntity implements IHeatSource, ICoolingSource, IMekanismHeatHandler {

    private final AdapterHeatCapacitor capacitor;
    private double heatOutput;
    private double coolingOutput;

    public MekanismHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(MekanismIntegration.HEAT_EXCHANGER_BLOCK_ENTITY.get(), pos, state);
        this.capacitor = new AdapterHeatCapacitor(this::setChanged, HeatAPI.AMBIENT_TEMP, HeatAPI.DEFAULT_HEAT_CAPACITY);
    }

    public void serverTick() {
        double referenceTemperature = ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get();
        MekanismHeatConversion.Output output =
                MekanismHeatConversion.convert(capacitor.getTemperature(), referenceTemperature, conversionCoefficient);
        heatOutput = output.heatOutput();
        coolingOutput = output.coolingOutput();
    }

    @Override
    public double getHeatOutput() {
        return heatOutput;
    }

    @Override
    public double getCoolingOutput() {
        return coolingOutput;
    }

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return List.of(capacitor);
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            ThermalSystemsAPI.unbindHeatSource(level, worldPosition);
            ThermalSystemsAPI.unbindCoolingSource(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Capacitor", capacitor.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Capacitor")) {
            capacitor.deserializeNBT(registries, tag.getCompound("Capacitor"));
        }
    }

    /**
     * Minimal {@link IHeatCapacitor}. Mekanism's own convenience
     * implementation, {@code BasicHeatCapacitor}, lives in
     * {@code mekanism.common} rather than the {@code :api} jar this mod
     * compiles against, so this reimplements the same handful of methods
     * directly against the public {@link IHeatCapacitor} contract.
     */
    private static final class AdapterHeatCapacitor implements IHeatCapacitor {

        private final IContentsListener listener;
        private final double heatCapacity;
        private double storedHeat;

        AdapterHeatCapacitor(IContentsListener listener, double initialTemperature, double heatCapacity) {
            this.listener = listener;
            this.heatCapacity = heatCapacity;
            this.storedHeat = initialTemperature * heatCapacity;
        }

        @Override
        public double getTemperature() {
            return storedHeat / heatCapacity;
        }

        @Override
        public double getInverseConduction() {
            return HeatAPI.DEFAULT_INVERSE_CONDUCTION;
        }

        @Override
        public double getInverseInsulation() {
            return HeatAPI.DEFAULT_INVERSE_INSULATION;
        }

        @Override
        public double getHeatCapacity() {
            return heatCapacity;
        }

        @Override
        public double getHeat() {
            return storedHeat;
        }

        @Override
        public void setHeat(double heat) {
            storedHeat = heat;
            onContentsChanged();
        }

        @Override
        public void handleHeat(double transfer) {
            storedHeat += transfer;
            onContentsChanged();
        }

        @Override
        public void onContentsChanged() {
            listener.onContentsChanged();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            if (nbt.contains("StoredHeat")) {
                storedHeat = nbt.getDouble("StoredHeat");
            }
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putDouble("StoredHeat", storedHeat);
            return nbt;
        }
    }
}

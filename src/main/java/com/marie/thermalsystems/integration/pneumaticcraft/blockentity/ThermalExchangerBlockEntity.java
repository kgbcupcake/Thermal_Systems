package com.marie.thermalsystems.integration.pneumaticcraft.blockentity;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.ThermalExchangerConversion;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exposes itself to PneumaticCraft: Repressurized as a heat exchanger block
 * (via {@code PNCCapabilities.HEAT_EXCHANGER_BLOCK}) and to Thermal Systems
 * as an {@link IHeatSource}/{@link ICoolingSource} (via this mod's own
 * capability API, registered by {@link PneumaticCraftIntegration}). Holds no
 * simulation state of its own beyond the PNC:R exchanger it owns - every
 * evaluation reads the exchanger's live temperature and converts it fresh
 * through {@link ThermalExchangerConversion}, so this never becomes a
 * second heat simulation system.
 */
public class ThermalExchangerBlockEntity extends BlockEntity implements IHeatSource, ICoolingSource {

    private final IHeatExchangerLogic exchanger;

    /**
     * Runtime-only by design, not persisted. PNC:R's own
     * {@code HeatExchangerLogicTicking.serializeNBT()}/{@code deserializeNBT()}
     * never write or restore ambient temperature - only {@code temperature}
     * and {@code behaviours} - and a freshly constructed exchanger always
     * starts with an uninitialized ({@code -1.0}) ambient temperature. If this
     * flag were restored from NBT as {@code true}, a reloaded exchanger would
     * skip {@link IHeatExchangerLogic#initializeAmbientTemperature} while its
     * ambient temperature was still uninitialized. So this must run once per
     * exchanger object lifetime (i.e. once per block-entity load), matching
     * what PNC:R itself does in {@code initializeAsHull}.
     */
    private boolean ambientInitialized;

    public ThermalExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(PneumaticCraftIntegration.THERMAL_EXCHANGER_BLOCK_ENTITY.get(), pos, state);
        this.exchanger = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();
    }

    public IHeatExchangerLogic getHeatExchangerLogic() {
        return exchanger;
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        if (!ambientInitialized) {
            exchanger.initializeAmbientTemperature(level, worldPosition);
            ambientInitialized = true;
        }
        exchanger.tick();
    }

    @Override
    public double getHeatOutput() {
        return convert().heatOutput();
    }

    @Override
    public double getCoolingOutput() {
        return convert().coolingOutput();
    }

    private ThermalExchangerConversion.Output convert() {
        double exchangerTemperature = exchanger.getTemperature();
        double referenceTemperature = ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get();
        return ThermalExchangerConversion.convert(exchangerTemperature, referenceTemperature, conversionCoefficient);
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
        tag.put("Exchanger", exchanger.serializeNBT());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Exchanger")) {
            exchanger.deserializeNBT(tag.getCompound("Exchanger"));
        }
    }
}

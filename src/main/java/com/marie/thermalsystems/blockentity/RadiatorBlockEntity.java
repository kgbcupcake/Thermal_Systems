package com.marie.thermalsystems.blockentity;

import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.registry.ModBlockEntities;
import com.marie.thermalsystems.zone.ClimateZone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Stores a radiator's zone binding and its current network-assigned heat
 * share. Implements {@link IHeatSource} directly; ClimateEngine consumes it
 * purely through that interface and remains unaware it is backed by a
 * radiator or a steam network.
 */
public class RadiatorBlockEntity extends BlockEntity implements IHeatSource {

    private UUID zoneId;
    private double currentHeatShare;

    public RadiatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATOR.get(), pos, state);
    }

    @Override
    public double getHeatOutput() {
        return currentHeatShare;
    }

    public void setHeatShare(double heatShare) {
        this.currentHeatShare = heatShare;
    }

    public boolean isBound() {
        return zoneId != null;
    }

    /**
     * @throws IllegalStateException if already bound to a zone
     */
    public void bindTo(ClimateZone zone) {
        if (isBound()) {
            throw new IllegalStateException("Radiator is already bound to a zone.");
        }
        zoneId = zone.getId();
        zone.getHeatSources().add(this);
    }

    public void unbind() {
        if (isBound() && level != null) {
            ClimateManager.get().getZone(level.dimension(), zoneId)
                    .ifPresent(zone -> zone.getHeatSources().remove(this));
        }
        zoneId = null;
    }

    @Override
    public void setRemoved() {
        unbind();
        super.setRemoved();
    }
}

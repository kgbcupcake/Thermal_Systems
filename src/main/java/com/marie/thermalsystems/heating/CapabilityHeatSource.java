package com.marie.thermalsystems.heating;

import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts a foreign BlockEntity's capability-provided {@link IHeatSource} so
 * it can be bound to a zone the same way a RadiatorBlockEntity is. Holds a
 * position, not a live capability/BlockEntity reference; resolves the
 * capability fresh via {@link HeatSourceCapabilities#HEAT_SOURCE} on every
 * evaluation and treats a missing capability as zero output rather than
 * throwing, since the block may have been broken or the chunk unloaded
 * since binding.
 */
public class CapabilityHeatSource implements IHeatSource {

    private final Level level;
    private final BlockPos pos;

    public CapabilityHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public BlockPos pos() {
        return pos;
    }

    /** The live capability at {@link #pos()}, or {@code null} if it's gone. */
    @Nullable
    public IHeatSource capability() {
        return HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null);
    }

    @Override
    public double getHeatOutput() {
        IHeatSource capability = capability();
        return capability != null ? capability.getHeatOutput() : 0.0;
    }

    @Override
    public Object getNetworkId() {
        IHeatSource capability = capability();
        return capability != null ? capability.getNetworkId() : null;
    }
}

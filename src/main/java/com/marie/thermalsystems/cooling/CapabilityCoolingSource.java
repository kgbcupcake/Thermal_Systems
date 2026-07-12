package com.marie.thermalsystems.cooling;

import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Mirror of {@link com.marie.thermalsystems.heating.CapabilityHeatSource}
 * for cooling sources.
 */
public class CapabilityCoolingSource implements ICoolingSource {

    private final Level level;
    private final BlockPos pos;

    public CapabilityCoolingSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public double getCoolingOutput() {
        ICoolingSource capability = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null);
        return capability != null ? capability.getCoolingOutput() : 0.0;
    }
}

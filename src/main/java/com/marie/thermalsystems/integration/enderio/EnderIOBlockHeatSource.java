package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Adapts a real Ender IO Stirling Generator's own, standard NeoForge
 * {@link IEnergyStorage} capability so it can be bound to a zone the same
 * way a RadiatorBlockEntity is. Holds a position, not a live capability/
 * BlockEntity reference; resolves {@link Capabilities.EnergyStorage#BLOCK}
 * fresh via {@link IEnergyStorage#getEnergyStored()} on every evaluation and
 * treats a missing storage as zero output rather than throwing, since the
 * block may have been broken or the chunk unloaded since binding. Never owns
 * or simulates energy state of its own - Ender IO's own machine is the only
 * generation involved.
 *
 * <p>Unlike Mekanism's temperature-delta split, the generator's converted
 * output has no natural heat/cool sign of its own - it only ever produces
 * energy - so the split here is mode-gated instead, via
 * {@link EnderIOThermalModeAttachment}: the converted value flows to
 * {@link #getHeatOutput()} in {@link ThermalMode#HEAT} and to
 * {@link #getCoolingOutput()} in {@link ThermalMode#COOL}, never both at
 * once.
 *
 * <p><b>Network delegation:</b> {@link ActiveSourcePositions} tracks the
 * generator's own position (see {@link EnderIOIntegration#onChunkLoad})
 * alongside its conduits so direct radiation reaches a player standing next
 * to the visible machine even when its conduit run is farther than
 * {@code sourceRadiationRadius} away. When an adjacent conduit exists,
 * {@link #getHeatOutput()}/{@link #getCoolingOutput()}/{@link #getNetworkId()}
 * all delegate to an {@link EnderIONetworkPosition} wrapping that conduit,
 * so querying the generator's position returns the exact same
 * network-summed value and network id a query against any of its conduits
 * would - {@code SourceRadiationTickHandler}'s per-network dedup then
 * collapses them to one contribution no matter which position (generator or
 * conduit) happened to be in range, instead of double-counting the
 * generator's own output once directly and once again via the network sum.
 * Only the mode-gated single-machine {@link #convert()} value is used as a
 * last resort, for the pathological case of a generator with no adjacent
 * conduit at all.
 */
final class EnderIOBlockHeatSource implements IHeatSource, ICoolingSource {

    private final Level level;
    private final BlockPos pos;

    EnderIOBlockHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        EnderIONetworkPosition network = adjacentNetwork();
        if (network != null) {
            return network.getHeatOutput();
        }
        return currentMode() == ThermalMode.HEAT ? convert() : 0.0;
    }

    @Override
    public double getCoolingOutput() {
        EnderIONetworkPosition network = adjacentNetwork();
        if (network != null) {
            return network.getCoolingOutput();
        }
        return currentMode() == ThermalMode.COOL ? convert() : 0.0;
    }

    /**
     * Delegates to the adjacent conduit network's own id when one exists, so
     * a generator and its conduits dedupe as the same contribution in
     * {@code SourceRadiationTickHandler} - see this class's Javadoc. Falls
     * back to {@code null} (this adapter's own single-machine default) only
     * when the generator has no adjacent conduit at all.
     */
    @Override
    public Object getNetworkId() {
        EnderIONetworkPosition network = adjacentNetwork();
        return network != null ? network.getNetworkId() : null;
    }

    /**
     * The Stirling Generator has no conduit connectivity of its own; it only
     * ever feeds power into whichever conduit bundle touches one of its six
     * faces. Returns an {@link EnderIONetworkPosition} wrapping the first
     * such adjacent conduit found, or {@code null} if the generator is
     * currently unconnected (e.g. just placed, or its conduit was broken).
     */
    private EnderIONetworkPosition adjacentNetwork() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null && neighbor.getType() == EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE) {
                return new EnderIONetworkPosition(level, neighborPos);
            }
        }
        return null;
    }

    private ThermalMode currentMode() {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return ThermalMode.HEAT;
        }
        return EnderIOThermalModeAttachment.get(blockEntity);
    }

    private double convert() {
        IEnergyStorage storage = Capabilities.EnergyStorage.BLOCK.getCapability(level, pos, null, null, null);
        if (storage == null) {
            return 0.0;
        }
        return convert(storage);
    }

    static double convert(IEnergyStorage storage) {
        double coefficient = ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get() * ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER.get();
        return EnderIOConversion.energyToHeat(storage.getEnergyStored(), coefficient);
    }
}

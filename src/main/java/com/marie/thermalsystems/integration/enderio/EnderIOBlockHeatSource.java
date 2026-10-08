package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.climate.IHeatPump;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.HeatPumpControl;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
 * energy - so it's an {@link IHeatPump}: inside a zone it heats or cools as
 * that zone's thermostat calls for (and stands by at target). Outside every
 * zone it follows the zone its conduit network runs into, if any, and only
 * otherwise lets {@link ThermalConfig#ENDERIO_COOLING_MODE} pick the
 * direction. Never both at once.
 *
 * <p>{@link #getNetworkId()} shares its adjacent conduit's network id (via a
 * capability-free flood-fill) so {@code SourceRadiationTickHandler} dedupes
 * the generator and its conduit as one contribution. It must never go
 * through {@link EnderIONetworkPosition} to get that id - that class's
 * {@code recompute()} queries this generator's own capability as part of
 * its boundary scan, so calling back into it here would recurse infinitely.
 */
final class EnderIOBlockHeatSource implements IHeatSource, ICoolingSource, IHeatPump {

    private final Level level;
    private final BlockPos pos;

    EnderIOBlockHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        return direction(getControllingZone()) == ClimateDemand.HEATING ? convert() : 0.0;
    }

    @Override
    public double getCoolingOutput() {
        return direction(getControllingZone()) == ClimateDemand.COOLING ? convert() : 0.0;
    }

    @Override
    public double getPumpOutput() {
        return convert();
    }

    /** Its own zone, or else the zone of the conduit network it feeds. */
    @Override
    public Optional<ClimateZone> getControllingZone() {
        Optional<ClimateZone> own = ZoneSpatialIndex.resolve(level, pos);
        if (own.isPresent()) {
            return own;
        }
        BlockPos conduit = adjacentConduit();
        return conduit == null ? Optional.empty() : new EnderIONetworkPosition(level, conduit).networkZone();
    }

    static ClimateDemand direction(Optional<ClimateZone> controllingZone) {
        return HeatPumpControl.direction(controllingZone, ThermalConfig.ENDERIO_COOLING_MODE.get());
    }

    @Nullable
    private BlockPos adjacentConduit() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null && neighbor.getType() == EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE) {
                return neighborPos;
            }
        }
        return null;
    }

    /** {@code null} if the generator has no adjacent conduit. */
    @Override
    public Object getNetworkId() {
        BlockPos conduit = adjacentConduit();
        if (conduit == null) {
            return null;
        }
        EnderIONetworkDiscovery.NetworkResult network =
                EnderIONetworkDiscovery.discover(conduit, level, EnderIOIntegration.CONDUIT_BLOCK_ENTITY_TYPE);
        return network.conduits().stream().min(BlockPos::compareTo).orElse(null);
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
        return EnderIOConversion.energyToHeat(storage.getEnergyStored(), storage.getMaxEnergyStored(), coefficient);
    }
}

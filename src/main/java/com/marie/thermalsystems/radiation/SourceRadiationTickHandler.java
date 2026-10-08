package com.marie.thermalsystems.radiation;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.climate.AmbientTemperature;
import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Once every {@code sourceRadiationInterval} server ticks, radiates every
 * position tracked in {@link ActiveSourcePositions} directly to every
 * online player within {@code sourceRadiationRadius} blocks (Chebyshev
 * distance), summing {@code getHeatOutput()}/{@code getCoolingOutput()} via
 * the same {@link HeatSourceCapabilities}/{@link CoolingSourceCapabilities}
 * lookups every other integration uses - deduped per
 * {@link IHeatSource#getNetworkId()}/{@link ICoolingSource#getNetworkId()} so
 * a single network contributes its output only once no matter how many of
 * its in-range positions get summed, see {@link #radiateTo} - and delivering
 * the result through the same {@link ITemperatureBridge} mechanism
 * {@code PlayerTemperatureBridgeHandler} uses for zone-ambient temperature -
 * just a different number feeding the same bridge call. Written generically
 * against {@link IHeatSource}/{@link ICoolingSource}; knows nothing about
 * Ender IO, Mekanism, PneumaticCraft, or bindings.
 *
 * <p>{@code getHeatOutput()}/{@code getCoolingOutput()} are rates (degrees
 * Celsius per simulation second), not absolute temperatures, so the summed
 * rate can't be handed to {@link ITemperatureBridge#applyAmbientTemperature}
 * as-is. It's scaled by {@code heatTransferCoefficient} - the same
 * coefficient {@code TemperatureCalculator} applies to a zone's
 * {@code totalHeatOutput} - and added to the configured default ambient
 * temperature, so one config knob keeps both paths comparably sensitive
 * without this handler reimplementing the zone's stateful convergence
 * simulation. The season shift is deliberately left out of the bridge value:
 * bridges sum this contribution with {@code PlayerTemperatureBridgeHandler}'s,
 * which already carries the season (outside a zone) or the zone's own
 * temperature (inside one), so including it here would count the outdoor
 * cold a second time - enough to freeze a player standing in a heated room.
 *
 * <p>Every online player gets a bridge call every interval, even one with
 * no tracked source within radius - that call just carries a net radiated
 * rate of 0 (so the bridge value collapses to the configured default, a
 * zero delta). Bridges have no dedicated "remove my
 * contribution" call, so skipping the call entirely when a player walks out
 * of range would leave that player's last nonzero contribution stuck
 * permanently under this handler's {@code sourceId} - sending 0 is how a
 * contribution actually clears. This can't reintroduce the zone-vs-radiation
 * collision the fixed-UUID/{@code sourceId} system was built to prevent:
 * this handler and {@code PlayerTemperatureBridgeHandler} already use
 * distinct, fixed {@code sourceId}s (see {@link #SOURCE_ID} below), so
 * zeroing this handler's contribution never touches the other's.
 *
 * <p>A player standing inside a zone gets no radiation at all: the zone's
 * own temperature, already driven by those same sources toward its
 * thermostat target, is all they feel. Adding radiation on top would count
 * every heater twice and push the player well past the target they set.
 */
public final class SourceRadiationTickHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRadiationTickHandler.class);

    /**
     * Fixed, unique-to-this-caller id passed as {@link ITemperatureBridge}'s
     * {@code sourceId} - see that interface's Javadoc. Identifies every call
     * this handler makes as the "direct source radiation" contribution, kept
     * distinct from {@code PlayerTemperatureBridgeHandler}'s own id so a
     * bridge backed by a caller-keyed additive attribute system (like LSO)
     * doesn't let one caller's contribution silently overwrite the other's.
     * Never regenerate this value.
     */
    private static final UUID SOURCE_ID = UUID.fromString("3d7a9c1e-4f6b-4e2a-8c5d-2b9f7a1e6d4c");

    /**
     * Instance state, not static: this handler is created once by
     * {@link com.marie.thermalsystems.ThermalSystemsMod} and registered on the
     * game bus. Every per-player map is cleared on logout and dimension
     * change, and everything on server stop.
     */

    /**
     * Most recent felt temperature near sources per player (seasonal ambient plus radiation), updated every
     * interval - read by the debug command and the config Home page. Not the value handed to the bridges,
     * which leaves the season out.
     */
    private final Map<UUID, Double> latestApplied = new ConcurrentHashMap<>();

    /**
     * Last temperature per player that was actually reported as a change. The
     * change check compares against this, not against {@link #latestApplied},
     * so a slow drift smaller than the epsilon per interval still surfaces
     * once it accumulates past it.
     */
    private final Map<UUID, Double> lastReported = new ConcurrentHashMap<>();

    /** Players whose previous interval had a tracked source within radiation radius. */
    private final Set<UUID> hadSourceInRange = ConcurrentHashMap.newKeySet();

    /**
     * Tracks, per level, whether {@link ActiveSourcePositions#getAll} was
     * empty on the previous tick this handler ran - purely diagnostic, so a
     * source silently disappearing (e.g. an unexpected chunk-unload eviction)
     * shows up as a single logged transition instead of either total silence
     * or a log line every {@code sourceRadiationInterval} ticks forever.
     */
    private final Map<ResourceKey<Level>, Boolean> lastPositionsEmpty = new ConcurrentHashMap<>();

    /** Extra per-player lines appended to {@code /thermal debug radiation} by optional integrations. */
    private final List<Function<ServerPlayer, String>> debugLineProviders = new ArrayList<>();

    private int tickCounter = 0;
    private Boolean wasEnabled = null;

    /**
     * Lets an optional integration add one line per player to
     * {@code /thermal debug radiation} without this handler knowing about it.
     */
    public void addDebugLineProvider(Function<ServerPlayer, String> provider) {
        debugLineProviders.add(provider);
    }

    /** The temperature last handed to the bridges for {@code playerId}, or {@code null} before the first interval. */
    public Double appliedTemperature(UUID playerId) {
        return latestApplied.get(playerId);
    }

    public boolean hasSourceInRange(UUID playerId) {
        return hadSourceInRange.contains(playerId);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        boolean enabled = ThermalConfig.SYSTEM_ENABLED.get();
        logIfEnabledChanged(enabled);
        if (!enabled) {
            tickCounter = 0;
            return;
        }

        int interval = ThermalConfig.SOURCE_RADIATION_INTERVAL.get();
        tickCounter++;
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        List<ITemperatureBridge> bridges = ThermalSystemsAPI.getTemperatureBridges();
        if (bridges.isEmpty()) {
            return;
        }

        int radius = ThermalConfig.SOURCE_RADIATION_RADIUS.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Set<BlockPos> positions = ActiveSourcePositions.getAll(level);
            logIfPositionsEmptyChanged(level, positions.isEmpty());
            for (ServerPlayer player : level.players()) {
                boolean inZone = ZoneSpatialIndex.resolve(level, player.blockPosition()).isPresent();
                radiateTo(level, player, inZone ? Set.of() : positions, radius, bridges);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ActiveSourcePositions.clear();
        latestApplied.clear();
        lastReported.clear();
        hadSourceInRange.clear();
        lastPositionsEmpty.clear();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayer(event.getEntity().getUUID());
    }

    /**
     * A dimension change starts a fresh comparison: the player's sources in
     * the new level are unrelated to the old one's, so carrying the old
     * state over would report a spurious "no longer has a tracked source".
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearPlayer(event.getEntity().getUUID());
    }

    private void clearPlayer(UUID playerId) {
        latestApplied.remove(playerId);
        lastReported.remove(playerId);
        hadSourceInRange.remove(playerId);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thermal")
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("radiation")
                                        .executes(context -> debugRadiation(context.getSource())))));
    }

    /**
     * The only place this handler's per-player state is ever sent to chat, and
     * only to whoever ran the command.
     */
    private int debugRadiation(CommandSourceStack source) {
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            Double applied = latestApplied.get(player.getUUID());
            String line = "[radiation] " + player.getGameProfile().getName()
                    + " applied=" + (applied != null ? applied + "C" : "n/a")
                    + " sourceInRange=" + hadSourceInRange.contains(player.getUUID());
            source.sendSuccess(() -> Component.literal(line), false);
            for (Function<ServerPlayer, String> provider : debugLineProviders) {
                String extra = provider.apply(player);
                source.sendSuccess(() -> Component.literal(extra), false);
            }
            count++;
        }
        return count;
    }

    /**
     * Sums each in-range position's heat/cooling, but only once per distinct
     * {@link IHeatSource#getNetworkId()}/{@link ICoolingSource#getNetworkId()}
     * - several in-range positions sharing a network id (e.g. multiple
     * segments of the same Ender IO conduit network) contribute that
     * network's output exactly once, no matter how many of its positions are
     * in range, at the largest value any of them reports (a conduit's sum
     * already includes the generator beside it). A {@code null} network id (the default for sources with no
     * network of their own) falls back to deduping by the position itself,
     * preserving today's per-position behavior for non-networked sources.
     */
    private void radiateTo(ServerLevel level, ServerPlayer player, Set<BlockPos> positions, int radius,
                                   List<ITemperatureBridge> bridges) {
        BlockPos playerPos = player.blockPosition();
        Map<Object, Double> heatByNetwork = new LinkedHashMap<>();
        Map<Object, Double> coolingByNetwork = new LinkedHashMap<>();
        boolean anyInRange = false;
        for (BlockPos pos : positions) {
            if (chebyshevDistance(playerPos, pos) > radius) {
                continue;
            }
            anyInRange = true;
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null);
            if (heatSource != null) {
                double heatOutput = heatSource.getHeatOutput();
                if (heatOutput != 0.0) {
                    Object key = heatSource.getNetworkId();
                    heatByNetwork.merge(key != null ? key : pos, heatOutput, Math::max);
                }
            }
            ICoolingSource coolingSource = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null);
            if (coolingSource != null) {
                double coolingOutput = coolingSource.getCoolingOutput();
                if (coolingOutput != 0.0) {
                    Object key = coolingSource.getNetworkId();
                    coolingByNetwork.merge(key != null ? key : pos, coolingOutput, Math::max);
                }
            }
        }

        double net = heatByNetwork.values().stream().mapToDouble(Double::doubleValue).sum()
                - coolingByNetwork.values().stream().mapToDouble(Double::doubleValue).sum();

        double radiated = ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get() * net;
        double bridgeTemperature = AmbientTemperature.configured() + radiated;
        for (ITemperatureBridge bridge : bridges) {
            bridge.applyAmbientTemperature(player, bridgeTemperature, SOURCE_ID);
        }
        double radiatedTemperature = AmbientTemperature.at(level, playerPos) + radiated;
        latestApplied.put(player.getUUID(), radiatedTemperature);
        logIfNoLongerInRange(player, anyInRange);
        logIfChanged(player, radiatedTemperature, heatByNetwork.keySet(), coolingByNetwork.keySet());
    }

    /**
     * Logs only on transition (empty -&gt; non-empty or vice versa), not on
     * every tick this handler runs, so a source silently disappearing from
     * {@link ActiveSourcePositions} - e.g. an unexpected chunk-unload
     * eviction - shows up as one clear log line instead of either total
     * silence or unbounded repetition.
     */
    private void logIfPositionsEmptyChanged(ServerLevel level, boolean empty) {
        if (!ThermalConfig.LOGGING_ENABLED.get() || !ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
            return;
        }
        Boolean previous = lastPositionsEmpty.put(level.dimension(), empty);
        if (previous == null || previous.booleanValue() != empty) {
            LOGGER.info("[MTS] ActiveSourcePositions for dim={} is now {}",
                    level.dimension().location(), empty ? "empty (no tracked sources)" : "non-empty");
        }
    }

    /**
     * Updates whether this player has a tracked source in range, and logs
     * only the in-range -&gt; out-of-range transition. The bridge call goes
     * out with a net rate of 0 either way (see the class Javadoc); this only
     * controls the log line. Runs unconditionally, so the state stays
     * accurate for the debug command even with logging off.
     */
    private void logIfNoLongerInRange(ServerPlayer player, boolean inRange) {
        boolean wasInRange = inRange ? !hadSourceInRange.add(player.getUUID()) : hadSourceInRange.remove(player.getUUID());
        if (!inRange && wasInRange && ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] Player={} no longer has a tracked source within radiation radius",
                    player.getGameProfile().getName());
        }
    }

    private void logIfEnabledChanged(boolean enabled) {
        Boolean previous = wasEnabled;
        wasEnabled = enabled;
        if (previous == null || previous.booleanValue() == enabled || !ThermalConfig.LOGGING_ENABLED.get()) {
            return;
        }
        if (enabled) {
            LOGGER.info("[MTS] SourceRadiationTickHandler simulation resumed (SYSTEM_ENABLED=true)");
        } else {
            LOGGER.info("[MTS] SourceRadiationTickHandler simulation paused (SYSTEM_ENABLED=false)");
        }
    }

    private static int chebyshevDistance(BlockPos a, BlockPos b) {
        return Math.max(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())), Math.abs(a.getZ() - b.getZ()));
    }

    /**
     * @param heatNetworks    the distinct heat network ids (or, for a
     *                        network-less source, its own position) that
     *                        contributed to this tick's sum - see
     *                        {@link #radiateTo}'s Javadoc
     * @param coolingNetworks mirror of {@code heatNetworks} for cooling
     */
    private void logIfChanged(ServerPlayer player, double radiatedTemperature,
                                      Set<Object> heatNetworks, Set<Object> coolingNetworks) {
        Double previous = lastReported.get(player.getUUID());
        double baseline = previous != null ? previous : ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get();
        if (Math.abs(radiatedTemperature - baseline) <= ThermalConfig.RADIATION_CHANGE_EPSILON.get()) {
            if (ThermalConfig.RADIATION_DEBUG_ENABLED.get()) {
                LOGGER.debug("[MTS] Player={} direct-radiation temperature unchanged at {} (heatNetworks={}, coolingNetworks={})",
                        player.getGameProfile().getName(), radiatedTemperature, heatNetworks, coolingNetworks);
            }
            return;
        }
        lastReported.put(player.getUUID(), radiatedTemperature);
        if (ThermalConfig.LOGGING_ENABLED.get() && ThermalConfig.RADIATION_LOGGING_ENABLED.get()) {
            LOGGER.info("[MTS] Player={} direct-radiation temperature changed to {} (heatNetworks={}, coolingNetworks={})",
                    player.getGameProfile().getName(), radiatedTemperature, heatNetworks, coolingNetworks);
        }
    }
}

package com.marie.thermalsystems.radiation;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.bridge.ITemperatureBridge;
import com.marie.thermalsystems.api.cooling.CoolingSourceCapabilities;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.HeatSourceCapabilities;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Once every {@code sourceRadiationInterval} server ticks, radiates every
 * position tracked in {@link ActiveSourcePositions} directly to every
 * online player within {@code sourceRadiationRadius} blocks (Chebyshev
 * distance), summing {@code getHeatOutput()}/{@code getCoolingOutput()} via
 * the same {@link HeatSourceCapabilities}/{@link CoolingSourceCapabilities}
 * lookups every other integration uses, and delivering the result through
 * the same {@link ITemperatureBridge} mechanism
 * {@code PlayerTemperatureBridgeHandler} uses for zone-ambient temperature -
 * just a different number feeding the same bridge call. Written generically
 * against {@link IHeatSource}/{@link ICoolingSource}; knows nothing about
 * Ender IO, Mekanism, PneumaticCraft, {@code ClimateZone}, or bindings.
 *
 * <p>{@code getHeatOutput()}/{@code getCoolingOutput()} are rates (degrees
 * Celsius per simulation second), not absolute temperatures, so the summed
 * rate can't be handed to {@link ITemperatureBridge#applyAmbientTemperature}
 * as-is. It's scaled by {@code heatTransferCoefficient} - the same
 * coefficient {@code TemperatureCalculator} applies to a zone's
 * {@code totalHeatOutput} - and added to {@code defaultAmbientTemperature},
 * so one config knob keeps both paths comparably sensitive without this
 * handler reimplementing the zone's stateful convergence simulation.
 *
 * <p>A player with no tracked source within radius is skipped entirely -
 * no bridge call is made for them - so this never overwrites whatever
 * zone-ambient temperature {@code PlayerTemperatureBridgeHandler} already
 * delivered for a player outside all radiation range.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class SourceRadiationTickHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRadiationTickHandler.class);

    private static final Map<UUID, Double> LAST_RADIATED = new ConcurrentHashMap<>();

    private static int tickCounter = 0;

    private SourceRadiationTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
            if (positions.isEmpty()) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                radiateTo(level, player, positions, radius, bridges);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ActiveSourcePositions.clear();
        LAST_RADIATED.clear();
    }

    private static void radiateTo(ServerLevel level, ServerPlayer player, Set<BlockPos> positions, int radius,
                                   List<ITemperatureBridge> bridges) {
        BlockPos playerPos = player.blockPosition();
        double net = 0.0;
        boolean anyInRange = false;
        for (BlockPos pos : positions) {
            if (chebyshevDistance(playerPos, pos) > radius) {
                continue;
            }
            anyInRange = true;
            IHeatSource heatSource = HeatSourceCapabilities.HEAT_SOURCE.getCapability(level, pos, null, null, null);
            if (heatSource != null) {
                net += heatSource.getHeatOutput();
            }
            ICoolingSource coolingSource = CoolingSourceCapabilities.COOLING_SOURCE.getCapability(level, pos, null, null, null);
            if (coolingSource != null) {
                net -= coolingSource.getCoolingOutput();
            }
        }

        if (!anyInRange) {
            LAST_RADIATED.remove(player.getUUID());
            return;
        }

        double radiatedTemperature = ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get()
                + ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get() * net;
        for (ITemperatureBridge bridge : bridges) {
            bridge.applyAmbientTemperature(player, radiatedTemperature);
        }
        logIfChanged(player, radiatedTemperature);
    }

    private static int chebyshevDistance(BlockPos a, BlockPos b) {
        return Math.max(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())), Math.abs(a.getZ() - b.getZ()));
    }

    private static void logIfChanged(ServerPlayer player, double radiatedTemperature) {
        if (!ThermalConfig.LOGGING_ENABLED.get()) {
            return;
        }
        Double previous = LAST_RADIATED.put(player.getUUID(), radiatedTemperature);
        if (previous == null || previous.doubleValue() != radiatedTemperature) {
            LOGGER.info("[MTS] Player={} direct-radiation temperature changed to {}",
                    player.getGameProfile().getName(), radiatedTemperature);
        }
    }
}

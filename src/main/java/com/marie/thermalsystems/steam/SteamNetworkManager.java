package com.marie.thermalsystems.steam;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.blockentity.BoilerBlockEntity;
import com.marie.thermalsystems.blockentity.RadiatorBlockEntity;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Owns steam network discovery scheduling and caching. Tracks which levels
 * have dirty connectivity and rebuilds only those levels' networks lazily,
 * once per {@code steamNetworkRecomputeInterval} server ticks - never
 * synchronously on a block update. Discovery itself stays in
 * {@link SteamNetworkDiscovery} as a separate pure function.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class SteamNetworkManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamNetworkManager.class);
    private static final SteamNetworkManager INSTANCE = new SteamNetworkManager();
    private static int tickCounter = 0;

    private final Map<ResourceKey<Level>, Set<BlockPos>> pipePositions = new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, Boolean> dirtyLevels = new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, List<SteamNetwork>> networksByLevel = new LinkedHashMap<>();

    private SteamNetworkManager() {
    }

    public static SteamNetworkManager get() {
        return INSTANCE;
    }

    public void registerPipe(ResourceKey<Level> level, BlockPos pos) {
        pipePositions.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(pos.immutable());
        markDirty(level, pos);
    }

    public void unregisterPipe(ResourceKey<Level> level, BlockPos pos) {
        Set<BlockPos> positions = pipePositions.get(level);
        if (positions != null) {
            positions.remove(pos);
        }
        markDirty(level, pos);
    }

    public void markDirty(ResourceKey<Level> level, BlockPos pos) {
        dirtyLevels.put(level, Boolean.TRUE);
    }

    public List<SteamNetwork> getNetworks(ResourceKey<Level> level) {
        return networksByLevel.getOrDefault(level, List.of());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = ThermalConfig.STEAM_NETWORK_RECOMPUTE_INTERVAL.get();
        tickCounter++;
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        boolean loggingEnabled = ThermalConfig.LOGGING_ENABLED.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            INSTANCE.evaluate(level, loggingEnabled);
        }
    }

    private void evaluate(ServerLevel level, boolean loggingEnabled) {
        ResourceKey<Level> key = level.dimension();
        if (Boolean.TRUE.equals(dirtyLevels.get(key))) {
            dirtyLevels.put(key, Boolean.FALSE);
            networksByLevel.put(key, rebuild(level, key));
        }

        for (SteamNetwork network : getNetworks(key)) {
            evaluateNetwork(level, network, loggingEnabled);
        }
    }

    private List<SteamNetwork> rebuild(ServerLevel level, ResourceKey<Level> key) {
        Set<BlockPos> positions = pipePositions.getOrDefault(key, Set.of());
        Set<BlockPos> visited = new HashSet<>();
        List<SteamNetwork> rebuilt = new ArrayList<>();

        for (BlockPos pos : positions) {
            if (visited.contains(pos)) {
                continue;
            }
            SteamNetwork network = SteamNetworkDiscovery.discover(pos, level);
            visited.addAll(network.pipes());
            rebuilt.add(network);
        }

        return rebuilt;
    }

    private void evaluateNetwork(ServerLevel level, SteamNetwork network, boolean loggingEnabled) {
        double totalHeat = 0.0;
        for (BlockPos pos : network.boilers()) {
            if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
                totalHeat += boiler.getHeatOutput();
            }
        }

        if (!network.radiators().isEmpty()) {
            double share = totalHeat / network.radiators().size();
            for (BlockPos pos : network.radiators()) {
                if (level.getBlockEntity(pos) instanceof RadiatorBlockEntity radiator) {
                    radiator.setHeatShare(share);
                }
            }
        }

        if (loggingEnabled) {
            LOGGER.info(
                    "[MTS] SteamNetwork Boilers={} Pipes={} Radiators={} TotalHeat={}",
                    network.boilers().size(),
                    network.pipes().size(),
                    network.radiators().size(),
                    String.format(Locale.ROOT, "%.2f", totalHeat));
        }
    }
}

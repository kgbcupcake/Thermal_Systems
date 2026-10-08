package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages climate zones. Owns all active zones via {@link ZoneRegistry}
 * directly.
 */
public class ClimateManager {

    private static final ClimateManager INSTANCE = new ClimateManager();

    private final ZoneRegistry registry = new ZoneRegistry();
    private final ClimateEngine engine = new ClimateEngine(new TemperatureCalculator());

    private ClimateManager() {
    }

    public static ClimateManager get() {
        return INSTANCE;
    }

    /**
     * Creates and registers a new zone using
     * {@link ThermalConfig#DEFAULT_TARGET_TEMPERATURE} as its target
     * temperature.
     *
     * @throws IllegalArgumentException if the name is null/empty or a zone
     *                                   with that name already exists in the
     *                                   level
     */
    public ClimateZone createZone(ResourceKey<Level> level, String name) {
        return createZone(level, name, ThermalConfig.DEFAULT_TARGET_TEMPERATURE.get());
    }

    /**
     * Creates and registers a new zone.
     *
     * @throws IllegalArgumentException if the name is null/empty, a zone with
     *                                   that name already exists in the level,
     *                                   or the target temperature is NaN/infinite
     */
    public ClimateZone createZone(ResourceKey<Level> level, String name, double targetTemp) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Zone name must not be null or empty.");
        }
        if (Double.isNaN(targetTemp) || Double.isInfinite(targetTemp)) {
            throw new IllegalArgumentException("Target temperature must be a finite number, was: " + targetTemp);
        }
        if (registry.containsName(level, name)) {
            throw new IllegalArgumentException("A zone named '" + name + "' already exists.");
        }

        ClimateZone zone = new ClimateZone(
                UUID.randomUUID(), name, ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get(), targetTemp, ClimateMode.AUTO);
        registry.add(level, zone);
        return zone;
    }

    /** Re-registers a zone loaded from disk, keeping its id. Replaces any zone already registered under that id. */
    public void restoreZone(ResourceKey<Level> level, ClimateZone zone) {
        registry.add(level, zone);
    }

    public Optional<ClimateZone> removeZone(ResourceKey<Level> level, UUID id) {
        return registry.remove(level, id);
    }

    /** Drops every zone in every level - called once the server has stopped and saved. */
    public void clear() {
        registry.clear();
    }

    public Optional<ClimateZone> getZoneByName(ResourceKey<Level> level, String name) {
        return registry.getByName(level, name);
    }

    public Optional<ClimateZone> getZone(ResourceKey<Level> level, UUID id) {
        return registry.get(level, id);
    }

    public Collection<ClimateZone> getZones(ResourceKey<Level> level) {
        return registry.getZones(level);
    }

    public Map<ResourceKey<Level>, Map<UUID, ClimateZone>> getAllZones() {
        return registry.getAllZones();
    }

    /**
     * Advances every zone in every level by {@code deltaTime} seconds.
     */
    public List<ZoneAdvanceResult> advanceAll(double deltaTime, MinecraftServer server) {
        List<ZoneAdvanceResult> results = new ArrayList<>();
        for (Map.Entry<ResourceKey<Level>, Map<UUID, ClimateZone>> entry : registry.getAllZones().entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            for (ClimateZone zone : entry.getValue().values()) {
                double ambient = AmbientTemperature.forZone(level, zone);
                double totalHeatOutput = engine.advance(zone, deltaTime, ambient);
                results.add(new ZoneAdvanceResult(zone, totalHeatOutput));
            }
        }
        return results;
    }
}

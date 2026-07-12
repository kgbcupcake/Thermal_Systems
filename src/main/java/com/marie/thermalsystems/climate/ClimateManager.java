package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneRegistry;
import net.minecraft.resources.ResourceKey;
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

    /** Starting temperature, in Celsius, for newly created zones. */
    private static final double AMBIENT_STARTING_TEMPERATURE = 20.0;

    private static final ClimateManager INSTANCE = new ClimateManager();

    private final ZoneRegistry registry = new ZoneRegistry();
    private final ClimateEngine engine = new ClimateEngine(new TemperatureCalculator());

    private ClimateManager() {
    }

    public static ClimateManager get() {
        return INSTANCE;
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
                UUID.randomUUID(), name, AMBIENT_STARTING_TEMPERATURE, targetTemp, ClimateMode.OFF);
        registry.add(level, zone);
        return zone;
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
    public List<ZoneAdvanceResult> advanceAll(double deltaTime) {
        List<ZoneAdvanceResult> results = new ArrayList<>();
        for (Map<UUID, ClimateZone> zones : registry.getAllZones().values()) {
            for (ClimateZone zone : zones.values()) {
                double totalHeatOutput = engine.advance(zone, deltaTime);
                results.add(new ZoneAdvanceResult(zone, totalHeatOutput));
            }
        }
        return results;
    }
}

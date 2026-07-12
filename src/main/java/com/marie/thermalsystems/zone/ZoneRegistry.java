package com.marie.thermalsystems.zone;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns in-memory storage of {@link ClimateZone} instances, keyed by zone id
 * and tracked per level. Iteration order is insertion order so log output
 * stays stable across ticks.
 */
public class ZoneRegistry {

    private final Map<ResourceKey<Level>, Map<UUID, ClimateZone>> zonesByLevel = new LinkedHashMap<>();

    public void add(ResourceKey<Level> level, ClimateZone zone) {
        zonesByLevel
                .computeIfAbsent(level, key -> new LinkedHashMap<>())
                .put(zone.getId(), zone);
    }

    public Optional<ClimateZone> get(ResourceKey<Level> level, UUID id) {
        Map<UUID, ClimateZone> zones = zonesByLevel.get(level);
        if (zones == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(zones.get(id));
    }

    public Optional<ClimateZone> getByName(ResourceKey<Level> level, String name) {
        Map<UUID, ClimateZone> zones = zonesByLevel.get(level);
        if (zones == null) {
            return Optional.empty();
        }
        return zones.values().stream()
                .filter(zone -> zone.getName().equals(name))
                .findFirst();
    }

    public boolean containsName(ResourceKey<Level> level, String name) {
        return getByName(level, name).isPresent();
    }

    public Collection<ClimateZone> getZones(ResourceKey<Level> level) {
        Map<UUID, ClimateZone> zones = zonesByLevel.get(level);
        if (zones == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(zones.values());
    }

    public Map<ResourceKey<Level>, Map<UUID, ClimateZone>> getAllZones() {
        return Collections.unmodifiableMap(zonesByLevel);
    }
}

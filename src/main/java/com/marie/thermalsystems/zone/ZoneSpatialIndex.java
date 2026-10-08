package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.climate.ClimateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves which bound ClimateZone, if any, contains a given position. Pure
 * lookup: never mutates zone state. If a position falls inside more than one
 * zone's bounds, returns the zone that registered its bounds first, matching
 * ZoneRegistry's existing deterministic iteration order.
 */
public final class ZoneSpatialIndex {

    private ZoneSpatialIndex() {
    }

    public static Optional<ClimateZone> resolve(Level level, BlockPos pos) {
        ResourceKey<Level> key = level.dimension();
        return resolveAmong(ClimateManager.get().getZones(key), pos);
    }

    /** The first zone in {@code level} (in registration order) containing any of {@code positions}. */
    public static Optional<ClimateZone> firstContainingAny(Level level, Collection<BlockPos> positions) {
        return firstContainingAnyAmong(ClimateManager.get().getZones(level.dimension()), positions);
    }

    static Optional<ClimateZone> firstContainingAnyAmong(Collection<ClimateZone> zones, Collection<BlockPos> positions) {
        for (ClimateZone zone : zones) {
            for (BlockPos pos : positions) {
                if (zone.containsPosition(pos)) {
                    return Optional.of(zone);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The zone containing {@code pos}, or failing that the zone whose bounds come closest to it,
     * within {@code reach} blocks. For things mounted on a room's wall: a box marked out by
     * clicking the floor, or the inside faces of the walls, can stop just short of them.
     */
    public static Optional<ClimateZone> resolveNearby(Level level, BlockPos pos, int reach) {
        return resolveNearbyAmong(ClimateManager.get().getZones(level.dimension()), pos, reach);
    }

    /**
     * The first zone in {@code level} whose bounds intersect the box spanning {@code corner1} and
     * {@code corner2}, ignoring the zone with id {@code exclude} (the zone being resized, if any).
     */
    public static Optional<ClimateZone> firstOverlapping(Level level, BlockPos corner1, BlockPos corner2,
                                                         @Nullable UUID exclude) {
        return firstOverlappingAmong(ClimateManager.get().getZones(level.dimension()), corner1, corner2, exclude);
    }

    /**
     * Pure containment resolution over an explicit zone collection, split out
     * from {@link #resolve(Level, BlockPos)} so the tie-break logic is
     * testable without a live Level.
     */
    static Optional<ClimateZone> resolveAmong(Collection<ClimateZone> zones, BlockPos pos) {
        for (ClimateZone zone : zones) {
            if (zone.containsPosition(pos)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }

    static Optional<ClimateZone> resolveNearbyAmong(Collection<ClimateZone> zones, BlockPos pos, int reach) {
        Optional<ClimateZone> containing = resolveAmong(zones, pos);
        if (containing.isPresent()) {
            return containing;
        }
        ClimateZone closest = null;
        long closestSq = (long) reach * reach + 1;
        for (ClimateZone zone : zones) {
            if (!zone.hasBounds()) {
                continue;
            }
            long distanceSq = distanceSq(zone, pos);
            if (distanceSq < closestSq) {
                closest = zone;
                closestSq = distanceSq;
            }
        }
        return Optional.ofNullable(closest);
    }

    /** Squared block distance from {@code pos} to the nearest block inside {@code zone}'s bounds. */
    private static long distanceSq(ClimateZone zone, BlockPos pos) {
        long dx = gap(pos.getX(), zone.getBoundsMin().getX(), zone.getBoundsMax().getX());
        long dy = gap(pos.getY(), zone.getBoundsMin().getY(), zone.getBoundsMax().getY());
        long dz = gap(pos.getZ(), zone.getBoundsMin().getZ(), zone.getBoundsMax().getZ());
        return dx * dx + dy * dy + dz * dz;
    }

    private static long gap(int value, int min, int max) {
        if (value < min) {
            return min - value;
        }
        return value > max ? value - max : 0;
    }

    static Optional<ClimateZone> firstOverlappingAmong(Collection<ClimateZone> zones, BlockPos corner1,
                                                       BlockPos corner2, @Nullable UUID exclude) {
        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        for (ClimateZone zone : zones) {
            if (!zone.hasBounds() || zone.getId().equals(exclude)) {
                continue;
            }
            BlockPos min = zone.getBoundsMin();
            BlockPos max = zone.getBoundsMax();
            if (minX <= max.getX() && maxX >= min.getX()
                    && minY <= max.getY() && maxY >= min.getY()
                    && minZ <= max.getZ() && maxZ >= min.getZ()) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }
}

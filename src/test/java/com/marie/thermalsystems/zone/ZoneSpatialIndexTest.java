package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneSpatialIndexTest {

    private static ClimateZone zoneWithBounds(String name, BlockPos corner1, BlockPos corner2) {
        ClimateZone zone = new ClimateZone(UUID.randomUUID(), name, 20.0, 20.0, ClimateMode.OFF);
        zone.setBounds(corner1, corner2);
        return zone;
    }

    @Test
    void resolvesToTheZoneContainingThePosition() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        ClimateZone kitchen = zoneWithBounds("Kitchen", new BlockPos(20, 60, 0), new BlockPos(30, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveAmong(List.of(bedroom, kitchen), new BlockPos(5, 65, 5));

        assertTrue(resolved.isPresent());
        assertEquals("Bedroom", resolved.get().getName());
    }

    @Test
    void resolvesToEmptyWhenNoZoneContainsThePosition() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveAmong(List.of(bedroom), new BlockPos(100, 65, 100));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void unboundedZonesAreNeverResolved() {
        ClimateZone unbounded = new ClimateZone(UUID.randomUUID(), "Unbounded", 20.0, 20.0, ClimateMode.OFF);

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveAmong(List.of(unbounded), new BlockPos(0, 0, 0));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void overlappingBoundsResolveToTheFirstRegisteredZone() {
        ClimateZone first = zoneWithBounds("First", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        ClimateZone second = zoneWithBounds("Second", new BlockPos(5, 60, 5), new BlockPos(15, 70, 15));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveAmong(List.of(first, second), new BlockPos(7, 65, 7));

        assertTrue(resolved.isPresent());
        assertEquals("First", resolved.get().getName());
    }
}

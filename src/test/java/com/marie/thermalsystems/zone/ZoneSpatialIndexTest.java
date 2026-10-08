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

    @Test
    void nearbyPrefersTheContainingZone() {
        ClimateZone hall = zoneWithBounds("Hall", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(12, 60, 0), new BlockPos(20, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveNearbyAmong(List.of(hall, bedroom), new BlockPos(12, 65, 5), 6);

        assertTrue(resolved.isPresent());
        assertEquals("Bedroom", resolved.get().getName());
    }

    @Test
    void nearbyFindsAFloorOnlyZoneBelowAWallMount() {
        ClimateZone floor = zoneWithBounds("Room", new BlockPos(0, 64, 0), new BlockPos(10, 64, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveNearbyAmong(List.of(floor), new BlockPos(5, 66, 0), 6);

        assertTrue(resolved.isPresent());
        assertEquals("Room", resolved.get().getName());
    }

    @Test
    void nearbyPicksTheClosestZone() {
        ClimateZone far = zoneWithBounds("Far", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        ClimateZone near = zoneWithBounds("Near", new BlockPos(16, 60, 0), new BlockPos(20, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveNearbyAmong(List.of(far, near), new BlockPos(14, 65, 5), 6);

        assertTrue(resolved.isPresent());
        assertEquals("Near", resolved.get().getName());
    }

    @Test
    void nearbyIgnoresZonesBeyondReach() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.resolveNearbyAmong(List.of(bedroom), new BlockPos(17, 65, 5), 6);

        assertTrue(resolved.isEmpty());
    }

    @Test
    void firstContainingAnyFindsTheZoneHoldingOneOfThePositions() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        ClimateZone kitchen = zoneWithBounds("Kitchen", new BlockPos(20, 60, 0), new BlockPos(30, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.firstContainingAnyAmong(List.of(bedroom, kitchen),
                List.of(new BlockPos(15, 59, 5), new BlockPos(25, 60, 5)));

        assertTrue(resolved.isPresent());
        assertEquals("Kitchen", resolved.get().getName());
    }

    @Test
    void firstContainingAnyIsEmptyWhenEveryPositionIsOutside() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> resolved = ZoneSpatialIndex.firstContainingAnyAmong(List.of(bedroom),
                List.of(new BlockPos(5, 59, 5), new BlockPos(11, 65, 5)));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void selectionSharingABlockOverlaps() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> overlap = ZoneSpatialIndex.firstOverlappingAmong(List.of(bedroom),
                new BlockPos(10, 65, 10), new BlockPos(20, 75, 20), null);

        assertTrue(overlap.isPresent());
        assertEquals("Bedroom", overlap.get().getName());
    }

    @Test
    void adjacentSelectionDoesNotOverlap() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> overlap = ZoneSpatialIndex.firstOverlappingAmong(List.of(bedroom),
                new BlockPos(11, 60, 0), new BlockPos(20, 70, 10), null);

        assertTrue(overlap.isEmpty());
    }

    @Test
    void resizingAZoneIgnoresItself() {
        ClimateZone bedroom = zoneWithBounds("Bedroom", new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        Optional<ClimateZone> overlap = ZoneSpatialIndex.firstOverlappingAmong(List.of(bedroom),
                new BlockPos(2, 60, 2), new BlockPos(12, 70, 12), bedroom.getId());

        assertTrue(overlap.isEmpty());
    }

    @Test
    void unboundedZonesNeverOverlap() {
        ClimateZone unbounded = new ClimateZone(UUID.randomUUID(), "Unbounded", 20.0, 20.0, ClimateMode.OFF);

        Optional<ClimateZone> overlap = ZoneSpatialIndex.firstOverlappingAmong(List.of(unbounded),
                new BlockPos(-100, 0, -100), new BlockPos(100, 100, 100), null);

        assertTrue(overlap.isEmpty());
    }
}

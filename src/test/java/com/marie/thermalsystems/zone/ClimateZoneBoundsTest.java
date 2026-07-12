package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimateZoneBoundsTest {

    private ClimateZone newZone() {
        return new ClimateZone(UUID.randomUUID(), "TestZone", 20.0, 20.0, ClimateMode.OFF);
    }

    @Test
    void hasNoBoundsByDefault() {
        ClimateZone zone = newZone();
        assertFalse(zone.hasBounds());
        assertFalse(zone.containsPosition(new BlockPos(0, 0, 0)));
    }

    @Test
    void containsPositionsInsideNormalizedBounds() {
        ClimateZone zone = newZone();
        zone.setBounds(new BlockPos(10, 70, 10), new BlockPos(0, 60, 0));

        assertTrue(zone.hasBounds());
        assertTrue(zone.containsPosition(new BlockPos(5, 65, 5)));
        assertTrue(zone.containsPosition(new BlockPos(0, 60, 0)));
        assertTrue(zone.containsPosition(new BlockPos(10, 70, 10)));
    }

    @Test
    void excludesPositionsOutsideBounds() {
        ClimateZone zone = newZone();
        zone.setBounds(new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));

        assertFalse(zone.containsPosition(new BlockPos(11, 65, 5)));
        assertFalse(zone.containsPosition(new BlockPos(5, 59, 5)));
        assertFalse(zone.containsPosition(new BlockPos(5, 65, 11)));
    }

    @Test
    void clearBoundsReturnsToUnbounded() {
        ClimateZone zone = newZone();
        zone.setBounds(new BlockPos(0, 60, 0), new BlockPos(10, 70, 10));
        zone.clearBounds();

        assertFalse(zone.hasBounds());
        assertFalse(zone.containsPosition(new BlockPos(5, 65, 5)));
    }
}

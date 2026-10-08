package com.marie.thermalsystems.block;

import com.marie.thermalsystems.block.WallThermostatTouch.Region;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WallThermostatTouchTest {

    private static final double STRIP = 0.2;
    private static final double SCREEN = 0.6;

    @Test
    void upperFaceIsTheScreen() {
        assertEquals(Region.SCREEN, WallThermostatTouch.regionOf(Direction.NORTH, 0.1, SCREEN, 0.9));
        assertEquals(Region.SCREEN, WallThermostatTouch.regionOf(Direction.EAST, 0.1, SCREEN, 0.9));
    }

    @Test
    void middleOfTheStripIsMode() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            assertEquals(Region.MODE, WallThermostatTouch.regionOf(facing, 0.5, STRIP, 0.5), facing.toString());
        }
    }

    /** Facing south the viewer looks north, so their left is west (low X) - minus sits there. */
    @Test
    void minusIsOnTheViewersLeft() {
        assertEquals(Region.MINUS, WallThermostatTouch.regionOf(Direction.SOUTH, 0.2, STRIP, 0.1));
        assertEquals(Region.PLUS, WallThermostatTouch.regionOf(Direction.SOUTH, 0.8, STRIP, 0.1));

        assertEquals(Region.MINUS, WallThermostatTouch.regionOf(Direction.NORTH, 0.8, STRIP, 0.9));
        assertEquals(Region.PLUS, WallThermostatTouch.regionOf(Direction.NORTH, 0.2, STRIP, 0.9));

        assertEquals(Region.MINUS, WallThermostatTouch.regionOf(Direction.EAST, 0.1, STRIP, 0.8));
        assertEquals(Region.PLUS, WallThermostatTouch.regionOf(Direction.EAST, 0.1, STRIP, 0.2));

        assertEquals(Region.MINUS, WallThermostatTouch.regionOf(Direction.WEST, 0.9, STRIP, 0.2));
        assertEquals(Region.PLUS, WallThermostatTouch.regionOf(Direction.WEST, 0.9, STRIP, 0.8));
    }
}

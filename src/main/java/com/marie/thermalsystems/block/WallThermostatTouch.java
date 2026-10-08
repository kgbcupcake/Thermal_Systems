package com.marie.thermalsystems.block;

import net.minecraft.core.Direction;

/**
 * Maps a click on a wall thermostat's face to the part of the screen it hit. The face is split into
 * the display (top) and a strip of three buttons along the bottom - minus, mode and plus, left to
 * right as seen by someone standing in front of it. Shared by the block (to act on the click) and
 * the renderer (to draw the buttons in the same places), so the two can never disagree.
 */
public final class WallThermostatTouch {

    public enum Region { SCREEN, MINUS, MODE, PLUS }

    /** Top of the button strip, in block-local Y (pixels 1-5 of 16). */
    public static final double BUTTON_STRIP_TOP = 5.0 / 16.0;
    /** Right edge of the minus button, in face-local U (0 = viewer's left edge of the block). */
    public static final double MINUS_RIGHT = 6.0 / 16.0;
    /** Left edge of the plus button, in face-local U. */
    public static final double PLUS_LEFT = 10.0 / 16.0;

    private WallThermostatTouch() {
    }

    /**
     * {@code facing} is the direction the screen faces; {@code localX/Y/Z} is the hit position
     * relative to the block's minimum corner, each in [0, 1].
     */
    public static Region regionOf(Direction facing, double localX, double localY, double localZ) {
        if (localY >= BUTTON_STRIP_TOP) {
            return Region.SCREEN;
        }
        double u = faceU(facing, localX, localZ);
        if (u < MINUS_RIGHT) {
            return Region.MINUS;
        }
        if (u > PLUS_LEFT) {
            return Region.PLUS;
        }
        return Region.MODE;
    }

    /** Horizontal position across the face, 0 at the viewer's left edge and 1 at their right. */
    static double faceU(Direction facing, double localX, double localZ) {
        Direction right = facing.getCounterClockWise();
        return (localX - 0.5) * right.getStepX() + (localZ - 0.5) * right.getStepZ() + 0.5;
    }
}

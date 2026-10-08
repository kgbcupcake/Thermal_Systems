package com.marie.thermalsystems.controller;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lets common code (items, blocks) open this mod's screens without referencing client-only
 * classes, which would crash a dedicated server. The client entry point installs the real
 * {@link Opener}; until then, and always on a dedicated server, every call is a no-op.
 */
public final class ThermalScreens {

    public interface Opener {

        /**
         * Opens the thermostat tablet. {@code zoneId} jumps straight to that zone's thermostat page
         * ({@code null} opens the zone list); {@code wallPos} is set when opened from a wall
         * thermostat, which pins the screen to that one room.
         */
        void openThermostat(@Nullable UUID zoneId, @Nullable BlockPos wallPos);

        void openGadget();

        /** Opens the zone editor on a box; {@code editing} is the zone being changed, or {@code null} for a new one. */
        void openZoneEditor(BlockPos first, BlockPos second, @Nullable UUID editing, String name);
    }

    private static volatile Opener opener = new Opener() {
        @Override
        public void openThermostat(@Nullable UUID zoneId, @Nullable BlockPos wallPos) {
        }

        @Override
        public void openGadget() {
        }

        @Override
        public void openZoneEditor(BlockPos first, BlockPos second, @Nullable UUID editing, String name) {
        }
    };

    private ThermalScreens() {
    }

    public static void setOpener(Opener newOpener) {
        opener = newOpener;
    }

    public static void openThermostat(@Nullable UUID zoneId, @Nullable BlockPos wallPos) {
        opener.openThermostat(zoneId, wallPos);
    }

    public static void openGadget() {
        opener.openGadget();
    }

    public static void openZoneEditor(BlockPos first, BlockPos second, @Nullable UUID editing, String name) {
        opener.openZoneEditor(first, second, editing, name);
    }
}

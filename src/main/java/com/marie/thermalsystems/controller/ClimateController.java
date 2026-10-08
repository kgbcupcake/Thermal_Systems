package com.marie.thermalsystems.controller;

import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.data.config.sync.ThermalConfigSync;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSourceScanner;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Gameplay-facing, server-authoritative zone editing used by the Zone Gadget, the thermostat
 * tablet and the wall thermostat. Every operation validates permissions and input itself and
 * reports back a {@link Result} for the player instead of throwing, since every caller is a
 * network handler or block interaction acting on untrusted client input.
 *
 * <p>Permissions: operators (and the singleplayer owner) may do anything. Otherwise a zone's
 * owner may resize, reconfigure and delete it; anyone else may only change its target and mode,
 * and only while the owner has turned on public control.
 */
public final class ClimateController {

    public static final int MAX_NAME_LENGTH = 32;
    /** Furthest, in blocks, a non-operator may stand from a zone they create or resize. */
    public static final int MAX_EDIT_DISTANCE = 96;

    public record Result(boolean success, Component message) {

        public static Result ok(String key, Object... args) {
            return new Result(true, Component.translatable("message.thermalsystems.zone." + key, args));
        }

        public static Result fail(String key, Object... args) {
            return new Result(false, Component.translatable("message.thermalsystems.zone." + key, args));
        }
    }

    private ClimateController() {
    }

    public static boolean isAdmin(ServerPlayer player) {
        return ThermalConfigSync.canEdit(player);
    }

    public static boolean canEdit(ServerPlayer player, ClimateZone zone) {
        return isAdmin(player) || player.getUUID().equals(zone.getOwner());
    }

    public static boolean canControl(ServerPlayer player, ClimateZone zone) {
        return canEdit(player, zone) || zone.isPublicControl();
    }

    public static double clampTarget(double target) {
        return Mth.clamp(target, ThermalConfig.MINIMUM_TEMPERATURE.get(), ThermalConfig.MAXIMUM_TEMPERATURE.get());
    }

    public static Result create(ServerPlayer player, String rawName, BlockPos corner1, BlockPos corner2, double target) {
        String name = cleanName(rawName);
        Result nameProblem = nameProblem(name);
        if (nameProblem != null) {
            return nameProblem;
        }
        if (!Double.isFinite(target)) {
            return Result.fail("bad_target");
        }
        Result boxProblem = validateBox(player, corner1, corner2, null);
        if (boxProblem != null) {
            return boxProblem;
        }
        ServerLevel level = player.serverLevel();
        int limit = ThermalConfig.MAX_ZONES_PER_PLAYER.get();
        if (!isAdmin(player) && limit > 0) {
            long owned = ClimateManager.get().getZones(level.dimension()).stream()
                    .filter(zone -> player.getUUID().equals(zone.getOwner()))
                    .count();
            if (owned >= limit) {
                return Result.fail("too_many", limit);
            }
        }

        ClimateZone zone;
        try {
            zone = ClimateManager.get().createZone(level.dimension(), name, clampTarget(target));
        } catch (IllegalArgumentException e) {
            return Result.fail("name_taken", name);
        }
        zone.setBounds(corner1, corner2);
        zone.setOwner(player.getUUID());
        return Result.ok("created", name, zone.volume());
    }

    /** Renames and/or resizes a zone in one go, from the Zone Gadget's editor. Nothing changes unless both are valid. */
    public static Result edit(ServerPlayer player, UUID zoneId, String rawName, BlockPos corner1, BlockPos corner2) {
        Optional<ClimateZone> found = find(player, zoneId);
        if (found.isEmpty()) {
            return Result.fail("missing");
        }
        ClimateZone zone = found.get();
        if (!canEdit(player, zone)) {
            return Result.fail("not_owner", zone.getName());
        }
        String name = cleanName(rawName);
        Result nameProblem = nameProblem(name);
        if (nameProblem != null) {
            return nameProblem;
        }
        Optional<ClimateZone> sameName = ClimateManager.get().getZoneByName(player.serverLevel().dimension(), name);
        if (sameName.isPresent() && !sameName.get().getId().equals(zoneId)) {
            return Result.fail("name_taken", name);
        }
        Result boxProblem = validateBox(player, corner1, corner2, zoneId);
        if (boxProblem != null) {
            return boxProblem;
        }
        zone.setName(name);
        zone.setBounds(corner1, corner2);
        ZoneSourceScanner.invalidate(zoneId);
        return Result.ok("saved", name, zone.volume());
    }

    private static String cleanName(@Nullable String rawName) {
        return rawName == null ? "" : rawName.strip();
    }

    @Nullable
    private static Result nameProblem(String name) {
        if (name.isEmpty()) {
            return Result.fail("name_empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return Result.fail("name_too_long", MAX_NAME_LENGTH);
        }
        return null;
    }

    /**
     * Applies a thermostat change. {@code publicControl} is only applied for players allowed to
     * edit the zone; anyone else sending a different value just has that part ignored.
     */
    public static Result updateSettings(ServerPlayer player, UUID zoneId, double target, ClimateMode mode,
                                        boolean publicControl) {
        Optional<ClimateZone> found = find(player, zoneId);
        if (found.isEmpty()) {
            return Result.fail("missing");
        }
        ClimateZone zone = found.get();
        if (!canControl(player, zone)) {
            return Result.fail("locked", zone.getName());
        }
        if (!Double.isFinite(target)) {
            return Result.fail("bad_target");
        }
        zone.setTargetTemp(clampTarget(target));
        zone.setMode(mode);
        if (publicControl != zone.isPublicControl() && canEdit(player, zone)) {
            zone.setPublicControl(publicControl);
        }
        return Result.ok("updated", zone.getName(),
                PlayerTemperatureUnits.of(player).format(zone.getTargetTemp()), mode.name());
    }

    /** One +/- press on the target, in whole steps of the player's own display unit. */
    public static Result stepTarget(ServerPlayer player, UUID zoneId, int direction) {
        TemperatureUnit unit = PlayerTemperatureUnits.of(player);
        return find(player, zoneId)
                .map(zone -> updateSettings(player, zoneId, unit.step(zone.getTargetTemp(), direction, false),
                        zone.getMode(), zone.isPublicControl()))
                .orElseGet(() -> Result.fail("missing"));
    }

    public static Result cycleMode(ServerPlayer player, UUID zoneId) {
        return find(player, zoneId)
                .map(zone -> updateSettings(player, zoneId, zone.getTargetTemp(), zone.getMode().next(), zone.isPublicControl()))
                .orElseGet(() -> Result.fail("missing"));
    }

    public static Result delete(ServerPlayer player, UUID zoneId) {
        Optional<ClimateZone> found = find(player, zoneId);
        if (found.isEmpty()) {
            return Result.fail("missing");
        }
        ClimateZone zone = found.get();
        if (!canEdit(player, zone)) {
            return Result.fail("not_owner", zone.getName());
        }
        ServerLevel level = player.serverLevel();
        ZoneSourceScanner.forget(level, zone);
        ThermalSystemsAPI.unbindAllForZone(level, zoneId);
        ClimateManager.get().removeZone(level.dimension(), zoneId);
        return Result.ok("deleted", zone.getName());
    }

    private static Optional<ClimateZone> find(ServerPlayer player, UUID zoneId) {
        return ClimateManager.get().getZone(player.serverLevel().dimension(), zoneId);
    }

    /** Returns the first problem with the box, or {@code null} when it's acceptable. */
    @Nullable
    private static Result validateBox(ServerPlayer player, BlockPos corner1, BlockPos corner2, @Nullable UUID exclude) {
        ServerLevel level = player.serverLevel();
        if (level.isOutsideBuildHeight(corner1) || level.isOutsideBuildHeight(corner2)) {
            return Result.fail("out_of_world");
        }
        boolean admin = isAdmin(player);
        long volume = volume(corner1, corner2);
        int maxVolume = ThermalConfig.MAX_ZONE_VOLUME.get();
        if (!admin && volume > maxVolume) {
            return Result.fail("too_big", volume, maxVolume);
        }
        if (!admin && distanceToBox(player.getX(), player.getY(), player.getZ(), corner1, corner2) > MAX_EDIT_DISTANCE) {
            return Result.fail("too_far");
        }
        Optional<ClimateZone> overlap = ZoneSpatialIndex.firstOverlapping(level, corner1, corner2, exclude);
        if (overlap.isPresent()) {
            return Result.fail("overlap", overlap.get().getName());
        }
        return null;
    }

    public static long volume(BlockPos corner1, BlockPos corner2) {
        return (long) (Math.abs(corner1.getX() - corner2.getX()) + 1)
                * (Math.abs(corner1.getY() - corner2.getY()) + 1)
                * (Math.abs(corner1.getZ() - corner2.getZ()) + 1);
    }

    public static double distanceToBox(double x, double y, double z, BlockPos corner1, BlockPos corner2) {
        double dx = axisDistance(x, corner1.getX(), corner2.getX());
        double dy = axisDistance(y, corner1.getY(), corner2.getY());
        double dz = axisDistance(z, corner1.getZ(), corner2.getZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisDistance(double value, int a, int b) {
        double min = Math.min(a, b);
        double max = Math.max(a, b) + 1;
        return value < min ? min - value : value > max ? value - max : 0;
    }
}

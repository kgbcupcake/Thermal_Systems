package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Serializes and deserializes a {@link ClimateZone}'s persistent state: id,
 * name, temperatures, mode, bounds, owner and public-control flag. Bound
 * heat/cooling sources are not saved - {@link ZoneSourceScanner} rebinds them
 * from the zone's bounds after a load.
 */
public final class ZoneSerializer {

    private static final String KEY_ID = "Id";
    private static final String KEY_NAME = "Name";
    private static final String KEY_CURRENT_TEMP = "CurrentTemp";
    private static final String KEY_TARGET_TEMP = "TargetTemp";
    private static final String KEY_MODE = "Mode";
    private static final String KEY_BOUNDS_MIN = "BoundsMin";
    private static final String KEY_BOUNDS_MAX = "BoundsMax";
    private static final String KEY_OWNER = "Owner";
    private static final String KEY_PUBLIC_CONTROL = "PublicControl";

    private ZoneSerializer() {
    }

    public static CompoundTag serialize(ClimateZone zone) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_ID, zone.getId());
        tag.putString(KEY_NAME, zone.getName());
        tag.putDouble(KEY_CURRENT_TEMP, zone.getCurrentTemp());
        tag.putDouble(KEY_TARGET_TEMP, zone.getTargetTemp());
        tag.putString(KEY_MODE, zone.getMode().name());
        if (zone.hasBounds()) {
            tag.putLong(KEY_BOUNDS_MIN, zone.getBoundsMin().asLong());
            tag.putLong(KEY_BOUNDS_MAX, zone.getBoundsMax().asLong());
        }
        if (zone.getOwner() != null) {
            tag.putUUID(KEY_OWNER, zone.getOwner());
        }
        tag.putBoolean(KEY_PUBLIC_CONTROL, zone.isPublicControl());
        return tag;
    }

    public static ClimateZone deserialize(CompoundTag tag) {
        UUID id = tag.getUUID(KEY_ID);
        String name = tag.getString(KEY_NAME);
        double currentTemp = tag.getDouble(KEY_CURRENT_TEMP);
        double targetTemp = tag.getDouble(KEY_TARGET_TEMP);
        ClimateMode mode = ClimateMode.parse(tag.getString(KEY_MODE));
        ClimateZone zone = new ClimateZone(id, name, currentTemp, targetTemp, mode);
        if (tag.contains(KEY_BOUNDS_MIN) && tag.contains(KEY_BOUNDS_MAX)) {
            zone.setBounds(BlockPos.of(tag.getLong(KEY_BOUNDS_MIN)), BlockPos.of(tag.getLong(KEY_BOUNDS_MAX)));
        }
        if (tag.hasUUID(KEY_OWNER)) {
            zone.setOwner(tag.getUUID(KEY_OWNER));
        }
        zone.setPublicControl(tag.getBoolean(KEY_PUBLIC_CONTROL));
        return zone;
    }
}

package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Serializes and deserializes the core state of a {@link ClimateZone}:
 * id, name, currentTemp, targetTemp, and mode. Heat/cooling source
 * serialization is out of scope for Phase 1.
 */
public final class ZoneSerializer {

    private static final String KEY_ID = "Id";
    private static final String KEY_NAME = "Name";
    private static final String KEY_CURRENT_TEMP = "CurrentTemp";
    private static final String KEY_TARGET_TEMP = "TargetTemp";
    private static final String KEY_MODE = "Mode";

    private ZoneSerializer() {
    }

    public static CompoundTag serialize(ClimateZone zone) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_ID, zone.getId());
        tag.putString(KEY_NAME, zone.getName());
        tag.putDouble(KEY_CURRENT_TEMP, zone.getCurrentTemp());
        tag.putDouble(KEY_TARGET_TEMP, zone.getTargetTemp());
        tag.putString(KEY_MODE, zone.getMode().name());
        return tag;
    }

    public static ClimateZone deserialize(CompoundTag tag) {
        UUID id = tag.getUUID(KEY_ID);
        String name = tag.getString(KEY_NAME);
        double currentTemp = tag.getDouble(KEY_CURRENT_TEMP);
        double targetTemp = tag.getDouble(KEY_TARGET_TEMP);
        ClimateMode mode = ClimateMode.valueOf(tag.getString(KEY_MODE));
        return new ClimateZone(id, name, currentTemp, targetTemp, mode);
    }
}

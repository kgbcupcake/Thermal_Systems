package com.marie.thermalsystems.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.data.config.ThermalConfigEntries;

/**
 * Converts {@link ThermalConfig}'s fields to/from a JSON tree, nested by the
 * same category keys as the underlying TOML spec. Shared by the config
 * screen's presets and export/import wiring so both operate on one
 * definition of "current config values" - every value listed in
 * {@link ThermalConfigEntries}.
 */
public final class ThermalConfigIO {

    private ThermalConfigIO() {}

    public static JsonObject buildRoot() {
        JsonObject root = new JsonObject();
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            JsonObject section = root.has(entry.section())
                    ? root.getAsJsonObject(entry.section())
                    : new JsonObject();
            switch (entry.kind()) {
                case BOOL -> section.addProperty(entry.key(), entry.read() >= 0.5);
                case INT -> section.addProperty(entry.key(), (int) Math.round(entry.read()));
                case DOUBLE -> section.addProperty(entry.key(), entry.read());
            }
            root.add(entry.section(), section);
        }
        return root;
    }

    /** Sets every value present in {@code root}, clamped to its spec range. Does not save. */
    public static void applyRoot(JsonObject root) {
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            if (!root.has(entry.section()) || !root.get(entry.section()).isJsonObject()) {
                continue;
            }
            JsonObject section = root.getAsJsonObject(entry.section());
            if (!section.has(entry.key())) {
                continue;
            }
            JsonElement element = section.get(entry.key());
            if (entry.kind() == ThermalConfigEntries.Kind.BOOL) {
                entry.write(element.getAsBoolean() ? 1 : 0);
            } else {
                entry.write(element.getAsDouble());
            }
        }
    }
}

package com.marie.thermalsystems.data.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalConfigEntriesTest {

    @Test
    void everyConfigValueIsRegisteredExactlyOnce() throws IllegalAccessException {
        Map<ModConfigSpec.ConfigValue<?>, Integer> counts = new IdentityHashMap<>();
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            counts.merge(entry.value(), 1, Integer::sum);
        }
        int fields = 0;
        for (Field field : ThermalConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                fields++;
                assertEquals(1, counts.getOrDefault(field.get(null), 0), field.getName() + " must be in ThermalConfigEntries exactly once");
            }
        }
        assertEquals(fields, ThermalConfigEntries.ALL.size());
    }

    @Test
    void pathsMatchTheSpecAndAreUnique() {
        Set<String> paths = new HashSet<>();
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            assertTrue(paths.add(entry.path()), "duplicate path " + entry.path());
            assertEquals(String.join(".", entry.value().getPath()), entry.path());
        }
    }

    @Test
    void defaultsSitInsideTheGuiRange() {
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            double def = entry.defaultValue();
            assertTrue(entry.guiMax() > entry.guiMin() && entry.step() > 0, entry.path() + " has an invalid GUI range");
            assertTrue(def >= entry.guiMin() && def <= entry.guiMax(),
                    entry.path() + " default " + def + " is outside its GUI range");
        }
    }

    @Test
    void clampUsesTheSpecRange() {
        ThermalConfigEntries.Entry radius = ThermalConfigEntries.get("integration", "sourceBindingRadius");
        assertEquals(64, radius.clampToSpec(1000));
        assertEquals(1, radius.clampToSpec(-5));
        ThermalConfigEntries.Entry coefficient = ThermalConfigEntries.get("enderio", "energyToHeatCoefficient");
        assertEquals(5000, coefficient.clampToSpec(5000), "spec range, not the GUI range, bounds a write");
        assertEquals(60.0, coefficient.clampToSpec(Double.NaN));
    }

    @Test
    void everyLabelHasALangEntry() throws Exception {
        JsonObject lang;
        try (InputStream in = getClass().getResourceAsStream("/assets/thermalsystems/lang/en_us.json")) {
            assertNotNull(in, "en_us.json not on the test classpath");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            assertTrue(lang.has(entry.langKey()), "missing lang key " + entry.langKey());
        }
    }
}

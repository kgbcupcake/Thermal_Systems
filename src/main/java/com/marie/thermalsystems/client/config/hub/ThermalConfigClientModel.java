package com.marie.thermalsystems.client.config.hub;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.data.config.ThermalConfigEntries;
import com.marie.thermalsystems.data.config.ThermalConfigEntries.Entry;
import com.marie.thermalsystems.data.config.sync.ConfigEditPayload;
import com.marie.thermalsystems.data.config.sync.ConfigSnapshotPayload;
import com.marie.thermalsystems.data.config.sync.ConfigSnapshotRequestPayload;
import com.marie.thermalsystems.data.config.sync.ThermalConfigSync;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The values {@link ThermalHubScreen}'s rows read and write. Setters only change this model; a
 * finished edit ({@link #commit()}) is sent on the screen's next tick by {@link #flushIfCommitted()},
 * so a "Reset This Module" that commits a dozen rows in one click goes out as one edit.
 *
 * <p>With no connection (opened from the main menu's Mods list) edits go straight into
 * {@link ThermalConfig} and are saved. In a world they go to the server as a {@link ConfigEditPayload},
 * and the model shows the server's values from the {@link ConfigSnapshotPayload} it replies with -
 * read-only until that reply says the player may edit.
 */
final class ThermalConfigClientModel {

    private static ThermalConfigClientModel active;

    private final Map<String, Double> values = new HashMap<>();
    private final Map<String, Double> pending = new LinkedHashMap<>();
    private final boolean remote;
    private boolean canEdit;
    private boolean commitRequested;

    private ThermalConfigClientModel(boolean remote) {
        this.remote = remote;
        this.canEdit = !remote;
        for (Entry entry : ThermalConfigEntries.ALL) {
            values.put(entry.path(), entry.read());
        }
    }

    /** A model over the local spec; in a world, its {@link #reload()} (run from the screen's {@code init}) fetches the server's values. */
    static ThermalConfigClientModel open() {
        boolean remote = Minecraft.getInstance().getConnection() != null;
        ThermalConfigClientModel model = new ThermalConfigClientModel(remote);
        active = model;
        ThermalConfigSync.setSnapshotListener(ThermalConfigClientModel::onSnapshot);
        return model;
    }

    private static void onSnapshot(ConfigSnapshotPayload payload) {
        if (active != null) {
            active.apply(payload);
        }
    }

    private void apply(ConfigSnapshotPayload payload) {
        for (Map.Entry<String, Double> value : payload.values().entrySet()) {
            if (!pending.containsKey(value.getKey())) {
                values.put(value.getKey(), value.getValue());
            }
        }
        canEdit = payload.canEdit();
    }

    boolean canEdit() {
        return canEdit;
    }

    boolean isRemote() {
        return remote;
    }

    double get(Entry entry) {
        return values.getOrDefault(entry.path(), entry.defaultValue());
    }

    boolean getBool(Entry entry) {
        return get(entry) >= 0.5;
    }

    int getInt(Entry entry) {
        return (int) Math.round(get(entry));
    }

    void set(Entry entry, double value) {
        if (!canEdit) {
            return;
        }
        double clamped = entry.clampToSpec(value);
        values.put(entry.path(), clamped);
        pending.put(entry.path(), clamped);
    }

    void commit() {
        commitRequested = true;
    }

    void flushIfCommitted() {
        if (commitRequested) {
            flush();
        }
    }

    void flush() {
        commitRequested = false;
        if (pending.isEmpty()) {
            return;
        }
        Map<String, Double> changes = new HashMap<>(pending);
        pending.clear();
        if (remote) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new ConfigEditPayload(changes));
            }
            return;
        }
        for (Map.Entry<String, Double> change : changes.entrySet()) {
            Entry entry = ThermalConfigEntries.byPath(change.getKey());
            if (entry != null) {
                entry.write(change.getValue());
            }
        }
        ThermalConfig.SPEC.save();
    }

    /** Re-reads the local spec (e.g. after an import applied outside this model); in a world, asks the server again. */
    void reload() {
        if (remote) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(ConfigSnapshotRequestPayload.INSTANCE);
            }
            return;
        }
        for (Entry entry : ThermalConfigEntries.ALL) {
            if (!pending.containsKey(entry.path())) {
                values.put(entry.path(), entry.read());
            }
        }
    }

    void close() {
        flush();
        if (active == this) {
            active = null;
        }
    }
}

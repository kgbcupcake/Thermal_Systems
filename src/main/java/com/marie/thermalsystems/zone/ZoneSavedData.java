package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.climate.ClimateManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * One dimension's climate zones on disk ({@code data/thermalsystems_zones.dat} in that dimension's
 * folder). {@link ClimateManager} stays the live owner of every zone; this only snapshots it at save
 * time and hands loaded zones back once, when the level loads. Zone temperatures change every
 * simulation step, so it always reports itself dirty rather than tracking individual edits.
 */
public final class ZoneSavedData extends SavedData {

    private static final String FILE_NAME = "thermalsystems_zones";
    private static final String KEY_ZONES = "Zones";

    private final ResourceKey<Level> dimension;
    private final List<ClimateZone> loaded;

    private ZoneSavedData(ResourceKey<Level> dimension, List<ClimateZone> loaded) {
        this.dimension = dimension;
        this.loaded = loaded;
    }

    /** Loads (or creates) this level's zone file and registers every zone in it with {@link ClimateManager}. */
    public static void loadInto(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        ZoneSavedData data = level.getDataStorage().computeIfAbsent(new Factory<>(
                () -> new ZoneSavedData(dimension, new ArrayList<>()),
                (tag, registries) -> read(dimension, tag),
                null), FILE_NAME);
        for (ClimateZone zone : data.loaded) {
            ClimateManager.get().restoreZone(dimension, zone);
        }
        data.loaded.clear();
    }

    private static ZoneSavedData read(ResourceKey<Level> dimension, CompoundTag tag) {
        List<ClimateZone> zones = new ArrayList<>();
        ListTag list = tag.getList(KEY_ZONES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            zones.add(ZoneSerializer.deserialize(list.getCompound(i)));
        }
        return new ZoneSavedData(dimension, zones);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ClimateZone zone : ClimateManager.get().getZones(dimension)) {
            list.add(ZoneSerializer.serialize(zone));
        }
        tag.put(KEY_ZONES, list);
        return tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}

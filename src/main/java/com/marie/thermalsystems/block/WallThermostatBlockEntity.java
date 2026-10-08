package com.marie.thermalsystems.block;

import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.registry.ThermalRegistries;
import com.marie.thermalsystems.zone.ClimateZone;
import com.marie.thermalsystems.zone.ZoneSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Server side: every {@link #REFRESH_TICKS} ticks, looks up the zone containing this thermostat (or
 * the closest one within {@link #ROOM_REACH} blocks) and
 * copies what the display needs (name, temperatures, mode, demand) into fields that are synced to
 * nearby clients - but only when something visible actually changed, so an idle room sends nothing.
 * Client side: holds the last synced copy for {@code WallThermostatRenderer} and for opening the
 * right zone when tapped. Nothing here is authoritative; the zone itself lives in
 * {@code ClimateManager}.
 */
public class WallThermostatBlockEntity extends BlockEntity {

    private static final int REFRESH_TICKS = 10;
    /** How far outside a zone's bounds the thermostat still counts as mounted in that room. */
    private static final int ROOM_REACH = 6;

    private static final String KEY_ZONE_ID = "ZoneId";
    private static final String KEY_ZONE_NAME = "ZoneName";
    private static final String KEY_CURRENT = "Current";
    private static final String KEY_TARGET = "Target";
    private static final String KEY_MODE = "Mode";
    private static final String KEY_DEMAND = "Demand";

    @Nullable
    private UUID zoneId;
    private String zoneName = "";
    private double currentTemp;
    private double targetTemp;
    private ClimateMode mode = ClimateMode.OFF;
    private ClimateDemand demand = ClimateDemand.IDLE;
    private int ticks;

    public WallThermostatBlockEntity(BlockPos pos, BlockState state) {
        super(ThermalRegistries.WALL_THERMOSTAT_ENTITY.get(), pos, state);
    }

    void serverTick() {
        if (++ticks >= REFRESH_TICKS) {
            ticks = 0;
            refresh();
        }
    }

    /** Re-reads the zone now and pushes any visible change to clients. */
    void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<ClimateZone> zone = ZoneSpatialIndex.resolveNearby(level, worldPosition, ROOM_REACH);
        UUID newId = zone.map(ClimateZone::getId).orElse(null);
        String newName = zone.map(ClimateZone::getName).orElse("");
        double newCurrent = zone.map(ClimateZone::getCurrentTemp).orElse(0.0);
        double newTarget = zone.map(ClimateZone::getTargetTemp).orElse(0.0);
        ClimateMode newMode = zone.map(ClimateZone::getMode).orElse(ClimateMode.OFF);
        ClimateDemand newDemand = zone.map(ClimateZone::getDemand).orElse(ClimateDemand.IDLE);

        boolean wasCalling = isCalling();
        boolean changed = !Objects.equals(zoneId, newId)
                || !zoneName.equals(newName)
                || tenths(currentTemp) != tenths(newCurrent)
                || tenths(targetTemp) != tenths(newTarget)
                || mode != newMode
                || demand != newDemand;
        if (!changed) {
            return;
        }

        zoneId = newId;
        zoneName = newName;
        currentTemp = newCurrent;
        targetTemp = newTarget;
        mode = newMode;
        demand = newDemand;

        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        if (wasCalling != isCalling()) {
            WallThermostatBlock.updatePowerNeighbors(state, level, worldPosition);
        }
    }

    private static long tenths(double value) {
        return Math.round(value * 10);
    }

    @Nullable
    public UUID zoneId() {
        return zoneId;
    }

    public boolean hasZone() {
        return zoneId != null;
    }

    public String zoneName() {
        return zoneName;
    }

    public double currentTemp() {
        return currentTemp;
    }

    public double targetTemp() {
        return targetTemp;
    }

    public ClimateMode mode() {
        return mode;
    }

    public ClimateDemand demand() {
        return demand;
    }

    public boolean isCalling() {
        return zoneId != null && demand != ClimateDemand.IDLE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (zoneId != null) {
            tag.putUUID(KEY_ZONE_ID, zoneId);
        }
        tag.putString(KEY_ZONE_NAME, zoneName);
        tag.putDouble(KEY_CURRENT, currentTemp);
        tag.putDouble(KEY_TARGET, targetTemp);
        tag.putString(KEY_MODE, mode.name());
        tag.putString(KEY_DEMAND, demand.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        zoneId = tag.hasUUID(KEY_ZONE_ID) ? tag.getUUID(KEY_ZONE_ID) : null;
        zoneName = tag.getString(KEY_ZONE_NAME);
        currentTemp = tag.getDouble(KEY_CURRENT);
        targetTemp = tag.getDouble(KEY_TARGET);
        mode = ClimateMode.parse(tag.getString(KEY_MODE));
        demand = parseDemand(tag.getString(KEY_DEMAND));
    }

    private static ClimateDemand parseDemand(String name) {
        for (ClimateDemand value : ClimateDemand.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return ClimateDemand.IDLE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

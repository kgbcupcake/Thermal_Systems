package com.marie.thermalsystems.network;

import com.marie.thermalsystems.climate.ClimateEngine;
import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.zone.ClimateZone;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Everything a client screen or overlay needs to show about one zone, as seen by one player:
 * {@code canEdit}/{@code canControl} are that player's own permissions. {@code min}/{@code max}
 * are both {@code null} for a zone without bounds; {@code ownerName} is empty for an
 * operator-only zone.
 */
public record ZoneInfo(UUID id, String name, @Nullable BlockPos min, @Nullable BlockPos max,
                       double currentTemp, double targetTemp, ClimateMode mode, ClimateDemand demand,
                       String ownerName, boolean canEdit, boolean canControl, boolean publicControl,
                       int heatSources, int coolingSources, double heatOutput, double coolingOutput) {

    public static final StreamCodec<FriendlyByteBuf, ZoneInfo> STREAM_CODEC =
            StreamCodec.of((buf, info) -> info.write(buf), ZoneInfo::read);

    public boolean hasBounds() {
        return min != null && max != null;
    }

    public long volume() {
        return hasBounds() ? ClimateController.volume(min, max) : 0;
    }

    public static ZoneInfo of(ServerPlayer viewer, ClimateZone zone) {
        ClimateEngine.SourceTotal heat = ClimateEngine.heatCapacity(zone);
        ClimateEngine.SourceTotal cooling = ClimateEngine.coolingCapacity(zone);
        return new ZoneInfo(
                zone.getId(),
                zone.getName(),
                zone.hasBounds() ? zone.getBoundsMin() : null,
                zone.hasBounds() ? zone.getBoundsMax() : null,
                zone.getCurrentTemp(),
                zone.getTargetTemp(),
                zone.getMode(),
                zone.getDemand(),
                ownerName(viewer.server, zone.getOwner()),
                ClimateController.canEdit(viewer, zone),
                ClimateController.canControl(viewer, zone),
                zone.isPublicControl(),
                heat.count(),
                cooling.count(),
                heat.output(),
                cooling.output());
    }

    /** The owner's name, or empty for an ownerless zone; falls back to {@code "?"} for an unknown profile. */
    public static String ownerName(MinecraftServer server, @Nullable UUID owner) {
        if (owner == null) {
            return "";
        }
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        GameProfileCache cache = server.getProfileCache();
        if (cache == null) {
            return "?";
        }
        return cache.get(owner).map(GameProfile::getName).orElse("?");
    }

    private void write(FriendlyByteBuf buf) {
        UUIDUtil.STREAM_CODEC.encode(buf, id);
        buf.writeUtf(name);
        buf.writeBoolean(hasBounds());
        if (hasBounds()) {
            buf.writeBlockPos(min);
            buf.writeBlockPos(max);
        }
        buf.writeDouble(currentTemp);
        buf.writeDouble(targetTemp);
        ThermalCodecs.CLIMATE_MODE.encode(buf, mode);
        ThermalCodecs.CLIMATE_DEMAND.encode(buf, demand);
        buf.writeUtf(ownerName);
        buf.writeBoolean(canEdit);
        buf.writeBoolean(canControl);
        buf.writeBoolean(publicControl);
        buf.writeVarInt(heatSources);
        buf.writeVarInt(coolingSources);
        buf.writeDouble(heatOutput);
        buf.writeDouble(coolingOutput);
    }

    private static ZoneInfo read(FriendlyByteBuf buf) {
        UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
        String name = buf.readUtf();
        BlockPos min = null;
        BlockPos max = null;
        if (buf.readBoolean()) {
            min = buf.readBlockPos();
            max = buf.readBlockPos();
        }
        return new ZoneInfo(id, name, min, max,
                buf.readDouble(), buf.readDouble(),
                ThermalCodecs.CLIMATE_MODE.decode(buf), ThermalCodecs.CLIMATE_DEMAND.decode(buf),
                buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble());
    }

    /** Looks a zone up by id in a list received from the server. */
    public static Optional<ZoneInfo> find(Iterable<ZoneInfo> zones, @Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        for (ZoneInfo zone : zones) {
            if (zone.id().equals(id)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }
}

package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-client: every config value keyed by {@code ThermalConfigEntries.Entry#path()} (booleans as
 * 0/1), plus whether the receiving player may edit them.
 */
public record ConfigSnapshotPayload(Map<String, Double> values, boolean canEdit) implements CustomPacketPayload {

    public static final Type<ConfigSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "config_snapshot"));

    public static final StreamCodec<ByteBuf, ConfigSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, String, Double, Map<String, Double>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE),
            ConfigSnapshotPayload::values,
            ByteBufCodecs.BOOL, ConfigSnapshotPayload::canEdit,
            ConfigSnapshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server: asks for a {@link ConfigSnapshotPayload} of the server's live config values. */
public record ConfigSnapshotRequestPayload() implements CustomPacketPayload {

    public static final ConfigSnapshotRequestPayload INSTANCE = new ConfigSnapshotRequestPayload();

    public static final Type<ConfigSnapshotRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "config_snapshot_request"));

    public static final StreamCodec<ByteBuf, ConfigSnapshotRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

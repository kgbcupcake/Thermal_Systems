package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server: asks for the sender's own {@link HubStatusPayload}, polled while the Hub's Home page is showing. */
public record HubStatusRequestPayload() implements CustomPacketPayload {

    public static final HubStatusRequestPayload INSTANCE = new HubStatusRequestPayload();

    public static final Type<HubStatusRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "hub_status_request"));

    public static final StreamCodec<ByteBuf, HubStatusRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

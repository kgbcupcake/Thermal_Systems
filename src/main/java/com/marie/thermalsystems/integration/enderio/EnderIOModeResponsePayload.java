package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client reply to {@link EnderIOModeRequestPayload}, carrying the
 * requested position's current {@link ThermalMode} (encoded by
 * {@link ThermalMode#name()}) back to {@link EnderIOClientIntegration}.
 */
record EnderIOModeResponsePayload(BlockPos pos, String mode) implements CustomPacketPayload {

    static final Type<EnderIOModeResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "enderio_mode_response"));

    static final StreamCodec<ByteBuf, EnderIOModeResponsePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EnderIOModeResponsePayload::pos,
            ByteBufCodecs.STRING_UTF8, EnderIOModeResponsePayload::mode,
            EnderIOModeResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

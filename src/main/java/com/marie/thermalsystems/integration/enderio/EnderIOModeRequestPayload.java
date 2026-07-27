package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-to-server request for a Stirling Generator's current
 * {@link ThermalMode}, sent once {@link EnderIOClientIntegration} confirms a
 * newly-opened screen belongs to a real generator - see
 * {@link EnderIOIntegration}'s {@code RegisterPayloadHandlersEvent} listener
 * for the server-side handler, which replies with
 * {@link EnderIOModeResponsePayload}.
 *
 * <p>{@link dev.marie.framework.network.GenericStateSyncPayload} is
 * client-to-server only (Marie's Lib registers it exclusively via
 * {@code playToServer}), so it can't carry this request's server-to-client
 * reply leg - hence this mod's own minimal payload pair instead of reusing
 * it for both directions.
 */
record EnderIOModeRequestPayload(BlockPos pos) implements CustomPacketPayload {

    static final Type<EnderIOModeRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "enderio_mode_request"));

    static final StreamCodec<ByteBuf, EnderIOModeRequestPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(EnderIOModeRequestPayload::new, EnderIOModeRequestPayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void sendToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new EnderIOModeRequestPayload(pos));
    }
}

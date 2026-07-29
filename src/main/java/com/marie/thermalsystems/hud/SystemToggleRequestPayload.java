package com.marie.thermalsystems.hud;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-to-server request to set {@code ThermalConfig.SYSTEM_ENABLED}'s live value, sent by
 * {@code ThermalSystemsControlPanel}'s "System Enabled" row toggle - a player-scoped control, not
 * tied to any block position.
 *
 * <p>{@link dev.marie.framework.network.GenericStateSyncPayload} doesn't fit here: it's explicitly
 * {@code BlockPos}-keyed (see its own javadoc), and this toggle belongs to the whole persistent HUD
 * panel, not any one block. Mirrors the {@code EnderIOModeRequestPayload}/
 * {@code EnderIOModeResponsePayload} custom-payload pattern instead, minus the response leg: the
 * client reads its own local {@code ThermalConfig.SYSTEM_ENABLED} directly for display rather than
 * round-tripping a query, which is correct for the common singleplayer/integrated-server case where
 * client and server share the same JVM and {@code ModConfigSpec} instance.
 *
 * <p>Public (unlike the Ender IO mode payloads, which stay package-private within
 * {@code integration.enderio}) because this payload is sent from
 * {@code com.marie.thermalsystems.client.hud}, a different package from where it's registered and
 * handled ({@link ThermalSystemsHud}, here in {@code com.marie.thermalsystems.hud}) - the
 * client/server split this mod already uses for config and screens, applied to this feature too.
 */
public record SystemToggleRequestPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<SystemToggleRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "system_toggle_request"));

    public static final StreamCodec<ByteBuf, SystemToggleRequestPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(SystemToggleRequestPayload::new, SystemToggleRequestPayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToServer(boolean enabled) {
        PacketDistributor.sendToServer(new SystemToggleRequestPayload(enabled));
    }
}

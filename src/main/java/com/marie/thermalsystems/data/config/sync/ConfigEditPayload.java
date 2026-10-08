package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Client-to-server: config values the player changed, keyed by {@code ThermalConfigEntries.Entry#path()} (booleans as 0/1). */
public record ConfigEditPayload(Map<String, Double> changes) implements CustomPacketPayload {

    public static final Type<ConfigEditPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "config_edit"));

    public static final StreamCodec<ByteBuf, ConfigEditPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, Double, Map<String, Double>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE)
                    .map(ConfigEditPayload::new, ConfigEditPayload::changes);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

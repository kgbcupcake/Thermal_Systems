package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client: the receiving player's live climate for the Hub's Home page. {@code zoneName} is
 * empty when the player stands outside every zone (the zone fields are then meaningless);
 * {@code appliedTemperature} is {@code NaN} until source radiation has run for them once.
 * {@code outsideTemperature} is the seasonal air temperature where the player is standing.
 */
public record HubStatusPayload(String zoneName, double zoneTemperature, double zoneTarget, String zoneMode,
                               double appliedTemperature, boolean sourceInRange,
                               double outsideTemperature) implements CustomPacketPayload {

    public static final Type<HubStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "hub_status"));

    /** Written by hand: {@link StreamCodec#composite} only accepts six fields. */
    public static final StreamCodec<ByteBuf, HubStatusPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HubStatusPayload decode(ByteBuf buf) {
            return new HubStatusPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, HubStatusPayload value) {
            ByteBufCodecs.STRING_UTF8.encode(buf, value.zoneName());
            ByteBufCodecs.DOUBLE.encode(buf, value.zoneTemperature());
            ByteBufCodecs.DOUBLE.encode(buf, value.zoneTarget());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.zoneMode());
            ByteBufCodecs.DOUBLE.encode(buf, value.appliedTemperature());
            ByteBufCodecs.BOOL.encode(buf, value.sourceInRange());
            ByteBufCodecs.DOUBLE.encode(buf, value.outsideTemperature());
        }
    };

    public boolean inZone() {
        return !zoneName.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

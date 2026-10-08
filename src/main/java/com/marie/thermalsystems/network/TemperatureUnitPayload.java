package com.marie.thermalsystems.network;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.controller.TemperatureUnit;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client tells the server which unit it displays temperatures in; sent on join and whenever it changes. */
public record TemperatureUnitPayload(TemperatureUnit unit) implements CustomPacketPayload {

    public static final Type<TemperatureUnitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "temperature_unit"));
    public static final StreamCodec<ByteBuf, TemperatureUnitPayload> STREAM_CODEC =
            ThermalCodecs.TEMPERATURE_UNIT.map(TemperatureUnitPayload::new, TemperatureUnitPayload::unit);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

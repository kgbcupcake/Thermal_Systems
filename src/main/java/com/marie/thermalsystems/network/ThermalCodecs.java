package com.marie.thermalsystems.network;

import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.controller.TemperatureUnit;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Stream codecs for this mod's enums, shared by payloads and synced item components. */
public final class ThermalCodecs {

    public static final StreamCodec<ByteBuf, ClimateMode> CLIMATE_MODE =
            ByteBufCodecs.idMapper(i -> ClimateMode.values()[i], Enum::ordinal);

    public static final StreamCodec<ByteBuf, ClimateDemand> CLIMATE_DEMAND =
            ByteBufCodecs.idMapper(i -> ClimateDemand.values()[i], Enum::ordinal);

    public static final StreamCodec<ByteBuf, TemperatureUnit> TEMPERATURE_UNIT =
            ByteBufCodecs.idMapper(i -> TemperatureUnit.values()[i], Enum::ordinal);

    private ThermalCodecs() {
    }
}

package com.marie.thermalsystems.item;

import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.network.ThermalCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A zone's thermostat settings held by the Zone Gadget in {@link GadgetMode#COPY} mode. */
public record CopiedSettings(String sourceName, double target, ClimateMode mode) {

    public static final Codec<CopiedSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source").forGetter(CopiedSettings::sourceName),
            Codec.DOUBLE.fieldOf("target").forGetter(CopiedSettings::target),
            Codec.STRING.xmap(ClimateMode::parse, Enum::name).fieldOf("mode").forGetter(CopiedSettings::mode)
    ).apply(instance, CopiedSettings::new));

    public static final StreamCodec<ByteBuf, CopiedSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CopiedSettings::sourceName,
            ByteBufCodecs.DOUBLE, CopiedSettings::target,
            ThermalCodecs.CLIMATE_MODE, CopiedSettings::mode,
            CopiedSettings::new);
}

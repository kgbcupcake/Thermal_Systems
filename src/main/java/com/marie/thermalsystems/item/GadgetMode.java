package com.marie.thermalsystems.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What right-clicking a block with the Zone Gadget does. */
public enum GadgetMode implements StringRepresentable {
    /** Pick two corners, then name and create a new zone from the gadget's menu. */
    CREATE,
    /** Click inside one of your zones to pick it, then pick new corners and apply them. */
    EDIT,
    /** Click inside any zone to see its temperatures, mode and sources in chat. */
    INSPECT,
    /** Click inside a zone to copy its target and mode, then click other zones to paste them. */
    COPY;

    public static final Codec<GadgetMode> CODEC = StringRepresentable.fromEnum(GadgetMode::values);
    public static final StreamCodec<ByteBuf, GadgetMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "item.thermalsystems.zone_gadget.mode." + getSerializedName();
    }
}

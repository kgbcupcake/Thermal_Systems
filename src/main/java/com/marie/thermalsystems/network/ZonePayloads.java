package com.marie.thermalsystems.network;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.item.GadgetMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every zone payload exchanged by the thermostat tablet, wall thermostat and Zone Gadget, handled
 * by {@link ThermalNetwork}. The zone editor sends the box it settled on with {@link GadgetSave};
 * the server re-checks it (build height, size, distance, overlap) like any other edit.
 */
public final class ZonePayloads {

    private ZonePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, path));
    }

    /** Client asks for the zones it may see; answered with a {@link ZoneList}. */
    public record ListRequest() implements CustomPacketPayload {

        public static final ListRequest INSTANCE = new ListRequest();
        public static final Type<ListRequest> TYPE = payloadType("zone_list_request");
        public static final StreamCodec<ByteBuf, ListRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server's reply: nearby and owned zones, nearest first, plus the receiving player's zone size
     * limit (0 when they're an operator and have none) so the gadget preview can flag a box that's
     * too big before it's ever sent, and the server's default target for new zones.
     */
    public record ZoneList(List<ZoneInfo> zones, int maxVolume, double defaultTarget) implements CustomPacketPayload {

        public static final Type<ZoneList> TYPE = payloadType("zone_list");
        public static final StreamCodec<FriendlyByteBuf, ZoneList> STREAM_CODEC = StreamCodec.composite(
                ZoneInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), ZoneList::zones,
                ByteBufCodecs.VAR_INT, ZoneList::maxVolume,
                ByteBufCodecs.DOUBLE, ZoneList::defaultTarget,
                ZoneList::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Thermostat change from the tablet or wall thermostat screen. */
    public record Settings(UUID zoneId, double target, ClimateMode mode, boolean publicControl)
            implements CustomPacketPayload {

        public static final Type<Settings> TYPE = payloadType("zone_settings");
        public static final StreamCodec<ByteBuf, Settings> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Settings::zoneId,
                ByteBufCodecs.DOUBLE, Settings::target,
                ThermalCodecs.CLIMATE_MODE, Settings::mode,
                ByteBufCodecs.BOOL, Settings::publicControl,
                Settings::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Delete(UUID zoneId) implements CustomPacketPayload {

        public static final Type<Delete> TYPE = payloadType("zone_delete");
        public static final StreamCodec<ByteBuf, Delete> STREAM_CODEC =
                UUIDUtil.STREAM_CODEC.map(Delete::new, Delete::zoneId);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Changes the held gadget: its mode, and optionally clears its corners, its copied settings, or
     * the zone it's editing. Switching to a different mode always clears corners and edit target.
     */
    public record GadgetUpdate(GadgetMode mode, boolean clearSelection, boolean clearCopy, boolean stopEditing)
            implements CustomPacketPayload {

        public static final Type<GadgetUpdate> TYPE = payloadType("gadget_update");
        public static final StreamCodec<ByteBuf, GadgetUpdate> STREAM_CODEC = StreamCodec.composite(
                GadgetMode.STREAM_CODEC, GadgetUpdate::mode,
                ByteBufCodecs.BOOL, GadgetUpdate::clearSelection,
                ByteBufCodecs.BOOL, GadgetUpdate::clearCopy,
                ByteBufCodecs.BOOL, GadgetUpdate::stopEditing,
                GadgetUpdate::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * The zone editor's Save: creates a zone from {@code first}/{@code second}, or with
     * {@code editing} set renames and resizes that zone ({@code target} is then ignored).
     */
    public record GadgetSave(Optional<UUID> editing, String name, double target, BlockPos first, BlockPos second)
            implements CustomPacketPayload {

        public static final Type<GadgetSave> TYPE = payloadType("gadget_save");
        public static final StreamCodec<ByteBuf, GadgetSave> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), GadgetSave::editing,
                ByteBufCodecs.stringUtf8(64), GadgetSave::name,
                ByteBufCodecs.DOUBLE, GadgetSave::target,
                BlockPos.STREAM_CODEC, GadgetSave::first,
                BlockPos.STREAM_CODEC, GadgetSave::second,
                GadgetSave::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server tells the client to open the zone editor on a box: after the second corner is
     * clicked, or when Edit mode picks up a zone ({@code editing} and its current {@code name}).
     */
    public record OpenEditor(BlockPos first, BlockPos second, Optional<UUID> editing, String name)
            implements CustomPacketPayload {

        public static final Type<OpenEditor> TYPE = payloadType("gadget_open_editor");
        public static final StreamCodec<ByteBuf, OpenEditor> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenEditor::first,
                BlockPos.STREAM_CODEC, OpenEditor::second,
                UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), OpenEditor::editing,
                ByteBufCodecs.stringUtf8(64), OpenEditor::name,
                OpenEditor::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

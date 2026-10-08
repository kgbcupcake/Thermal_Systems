package com.marie.thermalsystems.network;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.climate.ClimateManager;
import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.controller.PlayerTemperatureUnits;
import com.marie.thermalsystems.controller.ThermalScreens;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import com.marie.thermalsystems.zone.ClimateZone;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registers and handles {@link ZonePayloads}. Every client-to-server handler acts through
 * {@link ClimateController} or {@link ZoneGadgetItem} (which validate everything themselves),
 * shows the outcome on the player's action bar, and replies with a fresh
 * {@link ZonePayloads.ZoneList} so open screens update immediately.
 *
 * <p>The client half receives zone lists through {@link #setZoneListListener}, so this common class
 * never references a client-only type.
 */
public final class ThermalNetwork {

    /** Bounded zones further than this from a player are left out of their list unless they own them. */
    private static final double LIST_RANGE = 160;
    private static final int MAX_LISTED = 64;

    private static volatile Consumer<ZonePayloads.ZoneList> zoneListListener = payload -> {};

    private ThermalNetwork() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, ThermalNetwork::onRegisterPayloadHandlers);
    }

    public static void setZoneListListener(Consumer<ZonePayloads.ZoneList> listener) {
        zoneListListener = listener;
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThermalSystemsMod.MOD_ID).versioned("1");
        registrar.playToServer(ZonePayloads.ListRequest.TYPE, ZonePayloads.ListRequest.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> reply(context)));
        registrar.playToClient(ZonePayloads.ZoneList.TYPE, ZonePayloads.ZoneList.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> zoneListListener.accept(payload)));
        registrar.playToServer(ZonePayloads.Settings.TYPE, ZonePayloads.Settings.STREAM_CODEC, (payload, context) ->
                act(context, player -> ClimateController.updateSettings(
                        player, payload.zoneId(), payload.target(), payload.mode(), payload.publicControl())));
        registrar.playToServer(ZonePayloads.Delete.TYPE, ZonePayloads.Delete.STREAM_CODEC, (payload, context) ->
                act(context, player -> ClimateController.delete(player, payload.zoneId())));
        registrar.playToServer(ZonePayloads.GadgetUpdate.TYPE, ZonePayloads.GadgetUpdate.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    ZoneGadgetItem.applyUpdate((ServerPlayer) context.player(), payload);
                    reply(context);
                }));
        registrar.playToServer(ZonePayloads.GadgetSave.TYPE, ZonePayloads.GadgetSave.STREAM_CODEC, (payload, context) ->
                act(context, player -> ZoneGadgetItem.saveFromEditor(player, payload)));
        registrar.playToClient(ZonePayloads.OpenEditor.TYPE, ZonePayloads.OpenEditor.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> ThermalScreens.openZoneEditor(
                        payload.first(), payload.second(), payload.editing().orElse(null), payload.name())));
        registrar.playToServer(TemperatureUnitPayload.TYPE, TemperatureUnitPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> PlayerTemperatureUnits.set((ServerPlayer) context.player(), payload.unit())));
    }

    private static void act(IPayloadContext context, Function<ServerPlayer, ClimateController.Result> action) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ClimateController.Result result = action.apply(player);
            player.displayClientMessage(result.message(), true);
            reply(context);
        });
    }

    private static void reply(IPayloadContext context) {
        context.reply(listFor((ServerPlayer) context.player()));
    }

    public static ZonePayloads.ZoneList listFor(ServerPlayer player) {
        boolean admin = ClimateController.isAdmin(player);
        record Ranked(ClimateZone zone, double distance) {
        }
        List<Ranked> ranked = new ArrayList<>();
        for (ClimateZone zone : ClimateManager.get().getZones(player.serverLevel().dimension())) {
            boolean owned = player.getUUID().equals(zone.getOwner());
            if (zone.hasBounds()) {
                double distance = distanceToZone(player, zone);
                if (distance <= LIST_RANGE || owned) {
                    ranked.add(new Ranked(zone, distance));
                }
            } else if (owned || admin) {
                ranked.add(new Ranked(zone, Double.MAX_VALUE));
            }
        }
        ranked.sort(Comparator.comparingDouble(Ranked::distance));
        List<ZoneInfo> zones = new ArrayList<>();
        for (Ranked entry : ranked) {
            if (zones.size() >= MAX_LISTED) {
                break;
            }
            zones.add(ZoneInfo.of(player, entry.zone()));
        }
        return new ZonePayloads.ZoneList(zones, admin ? 0 : ThermalConfig.MAX_ZONE_VOLUME.get(),
                ThermalConfig.DEFAULT_TARGET_TEMPERATURE.get());
    }

    private static double distanceToZone(ServerPlayer player, ClimateZone zone) {
        double dx = axis(player.getX(), zone.getBoundsMin().getX(), zone.getBoundsMax().getX() + 1);
        double dy = axis(player.getY(), zone.getBoundsMin().getY(), zone.getBoundsMax().getY() + 1);
        double dz = axis(player.getZ(), zone.getBoundsMin().getZ(), zone.getBoundsMax().getZ() + 1);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axis(double value, double min, double max) {
        return value < min ? min - value : value > max ? value - max : 0;
    }
}

package com.marie.thermalsystems.data.config.sync;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.climate.AmbientTemperature;
import com.marie.thermalsystems.api.ThermalSystemsAPI;
import com.marie.thermalsystems.api.zone.ZoneSnapshot;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.data.config.ThermalConfigEntries;
import com.marie.thermalsystems.radiation.SourceRadiationTickHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Server-authoritative editing of {@link ThermalConfig} from the Hub config screen. {@code COMMON}
 * config isn't synced by NeoForge, so a client connected to a dedicated server would otherwise only
 * ever see and edit its own local file: the screen asks for a {@link ConfigSnapshotPayload} when it
 * opens, and sends each finished edit as a {@link ConfigEditPayload}, which only {@link #canEdit}
 * players may apply.
 *
 * <p>The client half receives snapshots through {@link #setSnapshotListener}, so this common class
 * never references a client-only type.
 */
public final class ThermalConfigSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThermalConfigSync.class);
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private static volatile Consumer<ConfigSnapshotPayload> snapshotListener = payload -> {};
    private static volatile Consumer<HubStatusPayload> statusListener = payload -> {};

    private static SourceRadiationTickHandler radiationHandler;

    private ThermalConfigSync() {
    }

    public static void init(IEventBus modEventBus, SourceRadiationTickHandler radiation) {
        radiationHandler = radiation;
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, ThermalConfigSync::onRegisterPayloadHandlers);
    }

    /** Ops (permission level 2) and the owner of a singleplayer/LAN world, cheats or not. */
    public static boolean canEdit(ServerPlayer player) {
        return player.hasPermissions(REQUIRED_PERMISSION_LEVEL)
                || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void setSnapshotListener(Consumer<ConfigSnapshotPayload> listener) {
        snapshotListener = listener;
    }

    public static void setStatusListener(Consumer<HubStatusPayload> listener) {
        statusListener = listener;
    }

    public static Map<String, Double> currentValues() {
        Map<String, Double> values = new HashMap<>();
        for (ThermalConfigEntries.Entry entry : ThermalConfigEntries.ALL) {
            values.put(entry.path(), entry.read());
        }
        return values;
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThermalSystemsMod.MOD_ID).versioned("1");
        registrar.playToServer(ConfigSnapshotRequestPayload.TYPE, ConfigSnapshotRequestPayload.STREAM_CODEC,
                ThermalConfigSync::onSnapshotRequest);
        registrar.playToServer(ConfigEditPayload.TYPE, ConfigEditPayload.STREAM_CODEC, ThermalConfigSync::onEdit);
        registrar.playToClient(ConfigSnapshotPayload.TYPE, ConfigSnapshotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> snapshotListener.accept(payload)));
        registrar.playToServer(HubStatusRequestPayload.TYPE, HubStatusRequestPayload.STREAM_CODEC,
                ThermalConfigSync::onStatusRequest);
        registrar.playToClient(HubStatusPayload.TYPE, HubStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> statusListener.accept(payload)));
    }

    private static void onStatusRequest(HubStatusRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> context.reply(statusFor((ServerPlayer) context.player())));
    }

    private static HubStatusPayload statusFor(ServerPlayer player) {
        Optional<ZoneSnapshot> zone = ThermalSystemsAPI.getZoneAt(player.level(), player.blockPosition());
        Double applied = radiationHandler != null ? radiationHandler.appliedTemperature(player.getUUID()) : null;
        boolean inRange = radiationHandler != null && radiationHandler.hasSourceInRange(player.getUUID());
        return new HubStatusPayload(
                zone.map(ZoneSnapshot::name).orElse(""),
                zone.map(ZoneSnapshot::currentTemp).orElse(0.0),
                zone.map(ZoneSnapshot::targetTemp).orElse(0.0),
                zone.map(z -> z.mode().name()).orElse(""),
                applied != null ? applied : Double.NaN,
                inRange,
                AmbientTemperature.at(player.level(), player.blockPosition()));
    }

    private static void onSnapshotRequest(ConfigSnapshotRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> context.reply(snapshotFor((ServerPlayer) context.player())));
    }

    private static void onEdit(ConfigEditPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!canEdit(player)) {
                if (ThermalConfig.LOGGING_ENABLED.get()) {
                    LOGGER.info("[MTS] Rejected config edit from {} (insufficient permission)",
                            player.getGameProfile().getName());
                }
                player.sendSystemMessage(Component.translatable("gui.thermalsystems.hub.no_permission"));
                context.reply(snapshotFor(player));
                return;
            }

            int applied = 0;
            for (Map.Entry<String, Double> change : payload.changes().entrySet()) {
                ThermalConfigEntries.Entry entry = ThermalConfigEntries.byPath(change.getKey());
                if (entry != null && change.getValue() != null) {
                    entry.write(change.getValue());
                    applied++;
                }
            }
            if (applied > 0) {
                ThermalConfig.SPEC.save();
                if (ThermalConfig.LOGGING_ENABLED.get()) {
                    LOGGER.info("[MTS] {} changed {} config value(s): {}",
                            player.getGameProfile().getName(), applied, payload.changes().keySet());
                }
            }
            context.reply(snapshotFor(player));
        });
    }

    private static ConfigSnapshotPayload snapshotFor(ServerPlayer player) {
        return new ConfigSnapshotPayload(currentValues(), canEdit(player));
    }
}

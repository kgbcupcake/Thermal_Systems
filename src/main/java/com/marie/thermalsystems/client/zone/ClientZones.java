package com.marie.thermalsystems.client.zone;

import com.marie.thermalsystems.network.ThermalNetwork;
import com.marie.thermalsystems.network.ZonePayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The client's latest copy of the zones the server lets it see. Anything showing zones (tablet,
 * gadget menu, gadget wireframes) calls {@link #poll} while visible, which re-requests the list at
 * most once per interval; the reply replaces {@link #latest()} wholesale.
 */
public final class ClientZones {

    private static final ZonePayloads.ZoneList EMPTY = new ZonePayloads.ZoneList(List.of(), 0, Double.NaN);

    private static volatile ZonePayloads.ZoneList latest = EMPTY;
    private static long lastRequestMs;

    private ClientZones() {
    }

    public static void init() {
        ThermalNetwork.setZoneListListener(payload -> latest = payload);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            latest = EMPTY;
            lastRequestMs = 0;
        });
    }

    public static ZonePayloads.ZoneList latest() {
        return latest;
    }

    /** Requests a fresh list if the last request was at least {@code intervalMs} ago. */
    public static void poll(long intervalMs) {
        if (System.currentTimeMillis() - lastRequestMs >= intervalMs) {
            requestNow();
        }
    }

    public static void requestNow() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        lastRequestMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(ZonePayloads.ListRequest.INSTANCE);
    }
}

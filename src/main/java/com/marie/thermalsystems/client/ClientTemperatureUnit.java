package com.marie.thermalsystems.client;

import com.marie.thermalsystems.controller.TemperatureUnit;
import com.marie.thermalsystems.data.config.ThermalClientConfig;
import com.marie.thermalsystems.network.TemperatureUnitPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Changes this client's display unit and keeps the server informed of it, so messages the server
 * builds for this player use the same unit as their screens.
 */
public final class ClientTemperatureUnit {

    private ClientTemperatureUnit() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> sendToServer());
    }

    public static TemperatureUnit get() {
        return ThermalClientConfig.unit();
    }

    public static String format(double celsius) {
        return get().format(celsius);
    }

    public static void toggle() {
        set(get().next());
    }

    public static void set(TemperatureUnit unit) {
        if (!ThermalClientConfig.SPEC.isLoaded()) {
            return;
        }
        ThermalClientConfig.TEMPERATURE_UNIT.set(unit);
        ThermalClientConfig.SPEC.save();
        sendToServer();
    }

    private static void sendToServer() {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new TemperatureUnitPayload(get()));
        }
    }
}

package com.marie.thermalsystems.controller;

import com.marie.thermalsystems.ThermalSystemsMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each online player's chosen display unit, as last reported by their client, so messages the
 * server builds (chat, action bar) and the wall thermostat's +/- buttons match what that player's
 * own screens show. Players who never reported one get Celsius.
 */
@EventBusSubscriber(modid = ThermalSystemsMod.MOD_ID)
public final class PlayerTemperatureUnits {

    private static final Map<UUID, TemperatureUnit> UNITS = new ConcurrentHashMap<>();

    private PlayerTemperatureUnits() {
    }

    public static TemperatureUnit of(ServerPlayer player) {
        return UNITS.getOrDefault(player.getUUID(), TemperatureUnit.CELSIUS);
    }

    public static void set(ServerPlayer player, TemperatureUnit unit) {
        UNITS.put(player.getUUID(), unit);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UNITS.remove(event.getEntity().getUUID());
    }
}

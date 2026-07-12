package com.marie.thermalsystems.api.event;

import net.neoforged.bus.api.Event;

import java.util.UUID;

/**
 * Posted on the NeoForge event bus by ClimateEngine immediately after each
 * zone's temperature is recalculated, once per zone per simulation tick, not
 * gated by {@code loggingEnabled}. Informational only, not cancellable -
 * anything that needs to influence temperature does so through
 * IHeatSource/ICoolingSource, not by reacting to this event and mutating
 * the zone back.
 */
public class ZoneTemperatureUpdatedEvent extends Event {

    private final UUID zoneId;
    private final String zoneName;
    private final double previousTemp;
    private final double newTemp;
    private final double targetTemp;

    public ZoneTemperatureUpdatedEvent(UUID zoneId, String zoneName, double previousTemp, double newTemp, double targetTemp) {
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.previousTemp = previousTemp;
        this.newTemp = newTemp;
        this.targetTemp = targetTemp;
    }

    public UUID getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public double getPreviousTemp() {
        return previousTemp;
    }

    public double getNewTemp() {
        return newTemp;
    }

    public double getTargetTemp() {
        return targetTemp;
    }
}

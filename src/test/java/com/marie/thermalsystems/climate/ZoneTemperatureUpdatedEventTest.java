package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.api.event.ZoneTemperatureUpdatedEvent;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.zone.ClimateZone;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZoneTemperatureUpdatedEventTest {

    private final ClimateEngine engine = new ClimateEngine(new TemperatureCalculator());
    private java.util.function.Consumer<ZoneTemperatureUpdatedEvent> listener;

    @AfterEach
    void unregister() {
        if (listener != null) {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    @Test
    void firesWithBeforeAndAfterTemperatures() {
        ClimateZone zone = new ClimateZone(UUID.randomUUID(), "TestZone", 20.0, 22.0, ClimateMode.HEAT);

        AtomicReference<ZoneTemperatureUpdatedEvent> captured = new AtomicReference<>();
        listener = captured::set;
        NeoForge.EVENT_BUS.addListener(ZoneTemperatureUpdatedEvent.class, listener);

        double totalHeatOutput = engine.advance(zone, 1.0);

        ZoneTemperatureUpdatedEvent event = captured.get();
        assertNotNull(event);
        assertEquals(zone.getId(), event.getZoneId());
        assertEquals("TestZone", event.getZoneName());
        assertEquals(20.0, event.getPreviousTemp(), 1e-9);
        assertEquals(zone.getCurrentTemp(), event.getNewTemp(), 1e-9);
        assertEquals(22.0, event.getTargetTemp(), 1e-9);
        assertEquals(0.0, totalHeatOutput, 1e-9);
    }
}

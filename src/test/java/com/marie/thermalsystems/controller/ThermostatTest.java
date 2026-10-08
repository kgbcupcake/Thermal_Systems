package com.marie.thermalsystems.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThermostatTest {

    private static final double FAR_AMBIENT = -100.0;

    private static ClimateDemand demand(ClimateMode mode, double current, double target, ClimateDemand previous) {
        return Thermostat.demand(mode, current, target, FAR_AMBIENT, previous);
    }

    @Test
    void offNeverCalls() {
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.OFF, 0.0, 30.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.OFF, 40.0, 10.0, ClimateDemand.COOLING));
    }

    @Test
    void heatOnlyHeats() {
        assertEquals(ClimateDemand.HEATING, demand(ClimateMode.HEAT, 15.0, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.HEAT, 25.0, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void coolOnlyCools() {
        assertEquals(ClimateDemand.COOLING, demand(ClimateMode.COOL, 25.0, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.COOL, 15.0, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void autoDoesBoth() {
        assertEquals(ClimateDemand.HEATING, demand(ClimateMode.AUTO, 15.0, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.COOLING, demand(ClimateMode.AUTO, 25.0, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void idleZoneWaitsUntilOutsideTheStartBand() {
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.AUTO, 19.6, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.AUTO, 20.4, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.HEATING, demand(ClimateMode.AUTO, 19.4, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void callingZoneKeepsGoingUntilSatisfied() {
        assertEquals(ClimateDemand.HEATING, demand(ClimateMode.AUTO, 19.9, 20.0, ClimateDemand.HEATING));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.AUTO, 19.97, 20.0, ClimateDemand.HEATING));
        assertEquals(ClimateDemand.COOLING, demand(ClimateMode.AUTO, 20.1, 20.0, ClimateDemand.COOLING));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.AUTO, 20.03, 20.0, ClimateDemand.COOLING));
    }

    @Test
    void targetJustAboveAmbientStillGetsHeated() {
        // Idle drift toward 20.0 never goes below it, so a 0.5 start band would never trip.
        assertEquals(ClimateDemand.HEATING, Thermostat.demand(ClimateMode.AUTO, 20.2, 20.5, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, Thermostat.demand(ClimateMode.AUTO, 20.3, 20.5, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void targetJustBelowAmbientStillGetsCooled() {
        assertEquals(ClimateDemand.COOLING, Thermostat.demand(ClimateMode.AUTO, 19.8, 19.5, 20.0, ClimateDemand.IDLE));
        assertEquals(ClimateDemand.IDLE, Thermostat.demand(ClimateMode.AUTO, 19.7, 19.5, 20.0, ClimateDemand.IDLE));
    }

    @Test
    void startBandShrinksNearAmbientButNotBelowSatisfiedBand() {
        assertEquals(Thermostat.START_BAND, Thermostat.startBand(23.0, 20.0), 1e-9);
        assertEquals(0.25, Thermostat.startBand(20.5, 20.0), 1e-9);
        assertEquals(Thermostat.SATISFIED_BAND, Thermostat.startBand(20.0, 20.0), 1e-9);
    }

    @Test
    void switchingModeStopsAnInProgressCall() {
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.COOL, 19.9, 20.0, ClimateDemand.HEATING));
        assertEquals(ClimateDemand.IDLE, demand(ClimateMode.OFF, 15.0, 20.0, ClimateDemand.HEATING));
    }
}

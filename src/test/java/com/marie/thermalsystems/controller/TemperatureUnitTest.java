package com.marie.thermalsystems.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureUnitTest {

    @Test
    void convertsBothWays() {
        assertEquals(68.0, TemperatureUnit.FAHRENHEIT.fromCelsius(20.0), 1e-9);
        assertEquals(-40.0, TemperatureUnit.FAHRENHEIT.fromCelsius(-40.0), 1e-9);
        assertEquals(20.0, TemperatureUnit.FAHRENHEIT.toCelsius(68.0), 1e-9);
        assertEquals(9.0, TemperatureUnit.FAHRENHEIT.deltaFromCelsius(5.0), 1e-9);
        assertEquals(23.0, TemperatureUnit.CELSIUS.fromCelsius(23.0), 1e-9);
    }

    @Test
    void formatsWithSymbol() {
        assertEquals("73.4\u00B0F", TemperatureUnit.FAHRENHEIT.format(23.0));
        assertEquals("23.0\u00B0C", TemperatureUnit.CELSIUS.format(23.0));
    }

    @Test
    void celsiusStepsByHalfAndFiveDegrees() {
        assertEquals(23.5, TemperatureUnit.CELSIUS.step(23.0, 1, false), 1e-9);
        assertEquals(22.5, TemperatureUnit.CELSIUS.step(23.0, -1, false), 1e-9);
        assertEquals(28.0, TemperatureUnit.CELSIUS.step(23.0, 1, true), 1e-9);
    }

    @Test
    void fahrenheitStepsSnapToWholeDegrees() {
        TemperatureUnit f = TemperatureUnit.FAHRENHEIT;
        // 23 °C is 73.4 °F: up goes to 74, down to 73.
        assertEquals(74.0, f.fromCelsius(f.step(23.0, 1, false)), 1e-9);
        assertEquals(73.0, f.fromCelsius(f.step(23.0, -1, false)), 1e-9);
        assertEquals(83.0, f.fromCelsius(f.step(23.0, 1, true)), 1e-9);
    }

    @Test
    void fahrenheitStepsFromAWholeDegreeMoveExactlyOne() {
        TemperatureUnit f = TemperatureUnit.FAHRENHEIT;
        double seventyFour = f.toCelsius(74.0);
        assertEquals(75.0, f.fromCelsius(f.step(seventyFour, 1, false)), 1e-9);
        assertEquals(73.0, f.fromCelsius(f.step(seventyFour, -1, false)), 1e-9);
    }
}

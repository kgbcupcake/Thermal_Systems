package com.marie.thermalsystems.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureCalculatorTest {

    private final TemperatureCalculator calculator = new TemperatureCalculator();

    @Test
    void matchesExactFormula() {
        double current = 20.0;
        double target = 22.0;
        double totalHeatOutput = 4.0;
        double deltaTime = 1.0;
        double convergenceRate = 0.02;
        double heatTransferCoefficient = 0.05;
        double min = -50.0;
        double max = 50.0;

        double effectiveConvergenceRate = convergenceRate + (heatTransferCoefficient * totalHeatOutput);
        double expected = current + (target - current) * (1.0 - Math.exp(-effectiveConvergenceRate * deltaTime));

        double actual = calculator.computeNextTemperature(
                current, target, totalHeatOutput, deltaTime, convergenceRate, heatTransferCoefficient, min, max);

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    void movesTowardTargetWhenBelow() {
        double next = calculator.computeNextTemperature(20.0, 22.0, 4.0, 1.0, 0.02, 0.05, -50.0, 50.0);
        assertTrue(next > 20.0);
        assertTrue(next < 22.0);
    }

    @Test
    void movesTowardTargetWhenAbove() {
        double next = calculator.computeNextTemperature(25.0, 22.0, 4.0, 1.0, 0.02, 0.05, -50.0, 50.0);
        assertTrue(next < 25.0);
        assertTrue(next > 22.0);
    }

    @Test
    void neverOvershootsTarget() {
        double next = calculator.computeNextTemperature(20.0, 22.0, 100.0, 1000.0, 0.02, 0.05, -50.0, 50.0);
        assertTrue(next <= 22.0);
    }

    @Test
    void higherHeatOutputConvergesFaster() {
        double lowHeat = calculator.computeNextTemperature(20.0, 22.0, 1.0, 1.0, 0.02, 0.05, -50.0, 50.0);
        double highHeat = calculator.computeNextTemperature(20.0, 22.0, 10.0, 1.0, 0.02, 0.05, -50.0, 50.0);
        assertTrue(highHeat > lowHeat);
    }

    @Test
    void zeroDeltaTimeProducesNoChange() {
        double next = calculator.computeNextTemperature(20.0, 22.0, 4.0, 0.0, 0.02, 0.05, -50.0, 50.0);
        assertEquals(20.0, next, 1e-9);
    }

    @Test
    void resultIsClampedToConfiguredBounds() {
        double next = calculator.computeNextTemperature(49.0, 60.0, 100.0, 1000.0, 0.02, 0.05, -50.0, 50.0);
        assertEquals(50.0, next, 1e-9);
    }

    @Test
    void resultIsClampedToConfiguredFloor() {
        double next = calculator.computeNextTemperature(-49.0, -60.0, 100.0, 1000.0, 0.02, 0.05, -50.0, 50.0);
        assertEquals(-50.0, next, 1e-9);
    }
}

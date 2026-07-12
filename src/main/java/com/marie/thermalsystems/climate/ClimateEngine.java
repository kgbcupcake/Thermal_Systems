package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.api.climate.ITemperatureCalculator;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.event.ZoneTemperatureUpdatedEvent;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.zone.ClimateZone;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Orchestrates a single zone's temperature update. Owns no simulation
 * state: reads state from the {@link ClimateZone}, reads tunables from
 * {@link ThermalConfig}, invokes the {@link ITemperatureCalculator}, and
 * writes the result back.
 */
public class ClimateEngine {

    private final ITemperatureCalculator calculator;

    public ClimateEngine(ITemperatureCalculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Advances the given zone by {@code deltaTime} seconds and writes the
     * resulting temperature back onto the zone. Posts a
     * {@link ZoneTemperatureUpdatedEvent} immediately after, regardless of
     * {@code loggingEnabled}.
     *
     * @return the total heat output that was applied, for reporting purposes
     */
    public double advance(ClimateZone zone, double deltaTime) {
        double totalHeatOutput = sumHeatOutput(zone);
        double previousTemp = zone.getCurrentTemp();

        double nextTemperature = calculator.computeNextTemperature(
                zone.getCurrentTemp(),
                zone.getTargetTemp(),
                totalHeatOutput,
                deltaTime,
                ThermalConfig.TEMPERATURE_CONVERGENCE_RATE.get(),
                ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get(),
                ThermalConfig.MINIMUM_TEMPERATURE.get(),
                ThermalConfig.MAXIMUM_TEMPERATURE.get()
        );

        zone.setCurrentTemp(nextTemperature);

        NeoForge.EVENT_BUS.post(new ZoneTemperatureUpdatedEvent(
                zone.getId(), zone.getName(), previousTemp, nextTemperature, zone.getTargetTemp()));

        return totalHeatOutput;
    }

    private static double sumHeatOutput(ClimateZone zone) {
        double total = 0.0;
        for (IHeatSource heatSource : zone.getHeatSources()) {
            total += heatSource.getHeatOutput();
        }
        for (ICoolingSource coolingSource : zone.getCoolingSources()) {
            total += coolingSource.getCoolingOutput();
        }
        return total;
    }
}

package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.api.climate.IHeatPump;
import com.marie.thermalsystems.api.climate.ITemperatureCalculator;
import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.event.ZoneTemperatureUpdatedEvent;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.Thermostat;
import com.marie.thermalsystems.cooling.CapabilityCoolingSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.heating.CapabilityHeatSource;
import com.marie.thermalsystems.zone.ClimateZone;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Orchestrates a single zone's temperature update. Owns no simulation
 * state: reads state from the {@link ClimateZone}, reads tunables from
 * {@link ThermalConfig}, invokes the {@link ITemperatureCalculator}, and
 * writes the result back.
 *
 * <p>The zone's {@link Thermostat} demand decides which sources count: only
 * heat sources while it calls for heat, only cooling sources while it calls
 * for cooling. While it calls for either, the zone converges on its target;
 * while idle it drifts back toward the ambient temperature (the configured
 * default, plus the season shift when a season mod is installed) with no
 * source contribution at all.
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
     * Uses {@code defaultAmbientTemperature} as the air the zone drifts toward.
     *
     * @return the total heat output that was applied, for reporting purposes
     */
    public double advance(ClimateZone zone, double deltaTime) {
        return advance(zone, deltaTime, ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get());
    }

    /**
     * Advances the given zone by {@code deltaTime} seconds and writes the
     * resulting temperature back onto the zone. {@code ambient} is the air
     * temperature the zone drifts toward while idle. Posts a
     * {@link ZoneTemperatureUpdatedEvent} immediately after, regardless of
     * {@code loggingEnabled}.
     *
     * @return the total heat output that was applied, for reporting purposes
     */
    public double advance(ClimateZone zone, double deltaTime, double ambient) {
        double previousTemp = zone.getCurrentTemp();
        ClimateDemand demand = Thermostat.demand(zone.getMode(), previousTemp, zone.getTargetTemp(), ambient, zone.getDemand());
        zone.setDemand(demand);

        double totalOutput = switch (demand) {
            case HEATING -> sumHeatOutput(zone);
            case COOLING -> sumCoolingOutput(zone);
            case IDLE -> 0.0;
        };
        double goal = demand == ClimateDemand.IDLE ? ambient : zone.getTargetTemp();

        double nextTemperature = calculator.computeNextTemperature(
                previousTemp,
                goal,
                totalOutput,
                deltaTime,
                ThermalConfig.TEMPERATURE_CONVERGENCE_RATE.get(),
                ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get(),
                ThermalConfig.MINIMUM_TEMPERATURE.get(),
                ThermalConfig.MAXIMUM_TEMPERATURE.get()
        );

        zone.setCurrentTemp(nextTemperature);

        NeoForge.EVENT_BUS.post(new ZoneTemperatureUpdatedEvent(
                zone.getId(), zone.getName(), previousTemp, nextTemperature, zone.getTargetTemp()));

        return totalOutput;
    }

    /** How many distinct sources (a whole conduit/cable network counting as one) and their combined output. */
    public record SourceTotal(int count, double output) {
    }

    public static double sumHeatOutput(ClimateZone zone) {
        return heatTotal(zone).output();
    }

    public static double sumCoolingOutput(ClimateZone zone) {
        return coolingTotal(zone).output();
    }

    public static SourceTotal heatTotal(ClimateZone zone) {
        return total(zone.getHeatSources(), IHeatSource::getHeatOutput, IHeatSource::getNetworkId);
    }

    public static SourceTotal coolingTotal(ClimateZone zone) {
        return total(zone.getCoolingSources(), ICoolingSource::getCoolingOutput, ICoolingSource::getNetworkId);
    }

    /** Like {@link #heatTotal}, but heat pumps count what they could deliver even while standing by. */
    public static SourceTotal heatCapacity(ClimateZone zone) {
        return total(zone.getHeatSources(),
                source -> unwrap(source) instanceof IHeatPump pump ? pump.getPumpOutput() : source.getHeatOutput(),
                IHeatSource::getNetworkId);
    }

    /** Like {@link #coolingTotal}, but heat pumps count what they could deliver even while standing by. */
    public static SourceTotal coolingCapacity(ClimateZone zone) {
        return total(zone.getCoolingSources(),
                source -> unwrap(source) instanceof IHeatPump pump ? pump.getPumpOutput() : source.getCoolingOutput(),
                ICoolingSource::getNetworkId);
    }

    /** Bound sources are position adapters; the heat pump, if any, is the capability behind them. */
    private static Object unwrap(Object source) {
        if (source instanceof CapabilityHeatSource bound) {
            return bound.capability();
        }
        if (source instanceof CapabilityCoolingSource bound) {
            return bound.capability();
        }
        return source;
    }

    /**
     * A zone binds every capable block inside it, so one network shows up once per conduit (and once
     * more for the machine feeding it), each reporting the whole network's output. Each network is
     * counted once, at the largest value any of its positions reports - a network position's sum
     * already includes every machine on it.
     */
    static <T> SourceTotal total(Collection<T> sources, ToDoubleFunction<T> output, Function<T, Object> networkOf) {
        Map<Object, Double> byNetwork = new HashMap<>();
        int standalone = 0;
        double total = 0.0;
        for (T source : sources) {
            double value = output.applyAsDouble(source);
            Object network = networkOf.apply(source);
            if (network == null) {
                standalone++;
                total += value;
            } else {
                byNetwork.merge(network, value, Math::max);
            }
        }
        for (double value : byNetwork.values()) {
            total += value;
        }
        return new SourceTotal(standalone + byNetwork.size(), total);
    }
}

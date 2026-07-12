package com.marie.thermalsystems.api.zone;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.controller.ClimateMode;

import java.util.List;
import java.util.UUID;

/**
 * Contract for a climate zone: a simulated volume of space with a current
 * temperature that converges toward a target temperature over time.
 */
public interface IClimateZone {

    UUID getId();

    String getName();

    double getCurrentTemp();

    void setCurrentTemp(double currentTemp);

    double getTargetTemp();

    ClimateMode getMode();

    List<IHeatSource> getHeatSources();

    List<ICoolingSource> getCoolingSources();
}

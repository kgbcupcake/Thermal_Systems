package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.api.zone.IClimateZone;
import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores the state of a single climate zone.
 */
public class ClimateZone implements IClimateZone {

    private final UUID id;
    private final String name;
    private double currentTemp;
    private final double targetTemp;
    private ClimateMode mode;
    private final List<IHeatSource> heatSources = new ArrayList<>();
    private final List<ICoolingSource> coolingSources = new ArrayList<>();
    private BlockPos boundsMin;
    private BlockPos boundsMax;

    public ClimateZone(UUID id, String name, double currentTemp, double targetTemp, ClimateMode mode) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.currentTemp = currentTemp;
        this.targetTemp = targetTemp;
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getCurrentTemp() {
        return currentTemp;
    }

    @Override
    public void setCurrentTemp(double currentTemp) {
        this.currentTemp = currentTemp;
    }

    @Override
    public double getTargetTemp() {
        return targetTemp;
    }

    @Override
    public ClimateMode getMode() {
        return mode;
    }

    public void setMode(ClimateMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public List<IHeatSource> getHeatSources() {
        return heatSources;
    }

    @Override
    public List<ICoolingSource> getCoolingSources() {
        return coolingSources;
    }

    public void addHeatSource(IHeatSource source) {
        heatSources.add(source);
    }

    public void removeHeatSource(IHeatSource source) {
        heatSources.remove(source);
    }

    public void addCoolingSource(ICoolingSource source) {
        coolingSources.add(source);
    }

    public void removeCoolingSource(ICoolingSource source) {
        coolingSources.remove(source);
    }

    /**
     * Sets or replaces this zone's spatial bounds to the axis-aligned box
     * spanning the two given corners (order does not matter).
     */
    public void setBounds(BlockPos corner1, BlockPos corner2) {
        Objects.requireNonNull(corner1, "corner1");
        Objects.requireNonNull(corner2, "corner2");
        this.boundsMin = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
        this.boundsMax = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
    }

    /**
     * Removes this zone's bounds, returning it to name/UUID-only resolution.
     */
    public void clearBounds() {
        this.boundsMin = null;
        this.boundsMax = null;
    }

    public boolean hasBounds() {
        return boundsMin != null;
    }

    public boolean containsPosition(BlockPos pos) {
        if (boundsMin == null) {
            return false;
        }
        return pos.getX() >= boundsMin.getX() && pos.getX() <= boundsMax.getX()
                && pos.getY() >= boundsMin.getY() && pos.getY() <= boundsMax.getY()
                && pos.getZ() >= boundsMin.getZ() && pos.getZ() <= boundsMax.getZ();
    }
}

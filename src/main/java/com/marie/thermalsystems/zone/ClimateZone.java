package com.marie.thermalsystems.zone;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.api.zone.IClimateZone;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores the state of a single climate zone.
 */
public class ClimateZone implements IClimateZone {

    private final UUID id;
    private String name;
    private double currentTemp;
    private double targetTemp;
    private ClimateMode mode;
    private ClimateDemand demand = ClimateDemand.IDLE;
    private final List<IHeatSource> heatSources = new ArrayList<>();
    private final List<ICoolingSource> coolingSources = new ArrayList<>();
    private BlockPos boundsMin;
    private BlockPos boundsMax;
    @Nullable
    private UUID owner;
    private boolean publicControl;

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

    /** Renames the zone; callers check the new name isn't taken by another zone in the level. */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
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

    public void setTargetTemp(double targetTemp) {
        if (Double.isNaN(targetTemp) || Double.isInfinite(targetTemp)) {
            throw new IllegalArgumentException("Target temperature must be a finite number, was: " + targetTemp);
        }
        this.targetTemp = targetTemp;
    }

    @Override
    public ClimateMode getMode() {
        return mode;
    }

    public void setMode(ClimateMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** What the thermostat asked for on the most recent simulation step. Not persisted. */
    public ClimateDemand getDemand() {
        return demand;
    }

    public void setDemand(ClimateDemand demand) {
        this.demand = Objects.requireNonNull(demand, "demand");
    }

    /** The player who created this zone, or {@code null} for a zone only operators may edit. */
    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /** When true, any player may change this zone's target and mode; bounds and deletion stay owner-only. */
    public boolean isPublicControl() {
        return publicControl;
    }

    public void setPublicControl(boolean publicControl) {
        this.publicControl = publicControl;
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

    /**
     * Returns this zone's minimum bound corner. Only valid when
     * {@link #hasBounds()} is true.
     */
    public BlockPos getBoundsMin() {
        return boundsMin;
    }

    /**
     * Returns this zone's maximum bound corner. Only valid when
     * {@link #hasBounds()} is true.
     */
    public BlockPos getBoundsMax() {
        return boundsMax;
    }

    /** Number of blocks inside this zone's bounds, or 0 when it has none. */
    public long volume() {
        if (boundsMin == null) {
            return 0;
        }
        return (long) (boundsMax.getX() - boundsMin.getX() + 1)
                * (boundsMax.getY() - boundsMin.getY() + 1)
                * (boundsMax.getZ() - boundsMin.getZ() + 1);
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

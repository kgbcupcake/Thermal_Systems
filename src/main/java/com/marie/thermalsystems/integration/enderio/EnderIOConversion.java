package com.marie.thermalsystems.integration.enderio;

/**
 * Pure, side-effect-free conversion between how full an Ender IO Stirling
 * Generator's native {@code IEnergyStorage} buffer currently is and Thermal
 * Systems' zone heat output. Independently testable; holds no state and
 * makes no calls into either Ender IO or Thermal Systems.
 *
 * <p>Uses the buffer's fill fraction ({@code energyStored / maxEnergyStored},
 * an intensive, storage-size-independent 0.0-1.0 value) rather than the raw
 * stored FE amount, so output stays bounded to {@code coefficient} (its value
 * at a full buffer) regardless of the generator's buffer capacity - unlike
 * raw stored FE, which grows unbounded with buffer size and made output spike
 * arbitrarily high.
 *
 * <p>Linear and coefficient-scaled, with no reference/baseline temperature
 * involved (unlike {@code MekanismHeatConversion}/{@code ThermalExchangerConversion})
 * - a non-empty buffer always adds heat, never cooling, matching
 * {@link com.marie.thermalsystems.api.heating.IHeatSource}.
 *
 * <p>{@code energyStored > maxEnergyStored} is clamped to a full buffer
 * rather than rejected: NeoForge's {@code IEnergyStorage} contract doesn't
 * actually guarantee the invariant holds at every instant for every
 * third-party implementation (e.g. a capacity-reducing augment removed
 * before the buffer itself is trimmed to match), and this class has no
 * business crashing a live tooltip/radiation read over a foreign mod's
 * momentary bookkeeping.
 */
public final class EnderIOConversion {

    private EnderIOConversion() {
    }

    /**
     * @throws IllegalArgumentException if energyStored or maxEnergyStored is negative, or
     *                                   coefficient is not a finite, non-negative number
     */
    public static double energyToHeat(long energyStored, long maxEnergyStored, double coefficient) {
        requireNonNegative(energyStored, "energyStored");
        requireNonNegative(maxEnergyStored, "maxEnergyStored");
        requireFiniteNonNegative(coefficient, "coefficient");
        if (maxEnergyStored == 0) {
            return 0.0;
        }
        double fillFraction = Math.min(1.0, (double) energyStored / (double) maxEnergyStored);
        return fillFraction * coefficient;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative, was: " + value);
        }
    }

    private static void requireFiniteNonNegative(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be a finite, non-negative number, was: " + value);
        }
    }
}

package com.marie.thermalsystems.integration.enderio;

/**
 * Pure, side-effect-free conversion between the FE an Ender IO Stirling
 * Generator currently holds in its native {@code IEnergyStorage} and
 * Thermal Systems' zone heat output. Independently testable; holds no state
 * and makes no calls into either Ender IO or Thermal Systems.
 *
 * <p>Linear and coefficient-scaled, with no reference/baseline temperature
 * involved (unlike {@code MekanismHeatConversion}/{@code ThermalExchangerConversion})
 * - stored FE always adds heat, never cooling, matching
 * {@link com.marie.thermalsystems.api.heating.IHeatSource}.
 */
public final class EnderIOConversion {

    private EnderIOConversion() {
    }

    /**
     * @throws IllegalArgumentException if energyStored is negative, or coefficient is not
     *                                   a finite, non-negative number
     */
    public static double energyToHeat(long energyStored, double coefficient) {
        requireNonNegative(energyStored, "energyStored");
        requireFiniteNonNegative(coefficient, "coefficient");
        return energyStored * coefficient;
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

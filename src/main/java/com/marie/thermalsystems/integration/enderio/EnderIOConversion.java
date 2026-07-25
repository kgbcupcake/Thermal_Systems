package com.marie.thermalsystems.integration.enderio;

/**
 * Pure, side-effect-free conversion between quantities Ender IO conduits
 * move (millibuckets of {@code thermalsystems:steam}, FE received per tick)
 * and Thermal Systems' zone heat output. Independently testable; holds no
 * state and makes no calls into either Ender IO or Thermal Systems.
 *
 * <p>Both directions are linear and coefficient-scaled, with no reference/
 * baseline temperature involved (unlike {@code MekanismHeatConversion}/
 * {@code ThermalExchangerConversion}) - a conduit delivering steam or FE
 * always adds heat, never cooling, matching {@link com.marie.thermalsystems.api.heating.IHeatSource}.
 */
public final class EnderIOConversion {

    private EnderIOConversion() {
    }

    /**
     * @throws IllegalArgumentException if amount is negative, or coefficient is not a
     *                                   finite, non-negative number
     */
    public static double fluidToHeat(int amount, double coefficient) {
        requireNonNegative(amount, "amount");
        requireFiniteNonNegative(coefficient, "coefficient");
        return amount * coefficient;
    }

    /**
     * Inverse of {@link #fluidToHeat(int, double)}: how many millibuckets of
     * steam a given heat quantity corresponds to. Returns {@code 0} when
     * {@code coefficient} is {@code 0}, since no finite amount of fluid could
     * ever produce heat through a zero coefficient.
     *
     * @throws IllegalArgumentException if heat is negative, NaN, or infinite, or
     *                                   coefficient is not a finite, non-negative number
     */
    public static int heatToFluidAmount(double heat, double coefficient) {
        requireFiniteNonNegative(heat, "heat");
        requireFiniteNonNegative(coefficient, "coefficient");
        if (coefficient == 0.0) {
            return 0;
        }
        return (int) (heat / coefficient);
    }

    /**
     * @throws IllegalArgumentException if energyReceived is negative, or coefficient is not
     *                                   a finite, non-negative number
     */
    public static double energyToHeat(long energyReceived, double coefficient) {
        requireNonNegative(energyReceived, "energyReceived");
        requireFiniteNonNegative(coefficient, "coefficient");
        return energyReceived * coefficient;
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

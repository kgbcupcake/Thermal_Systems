package com.marie.thermalsystems.integration.mekanism;

/**
 * Pure, side-effect-free conversion between a Mekanism heat capacitor's
 * absolute temperature (Kelvin, per {@code mekanism.api.heat.HeatAPI}) and
 * Thermal Systems' zone heat/cooling rate model. Independently testable;
 * holds no state and makes no calls into either Mekanism or Thermal Systems.
 *
 * <p>{@code temperatureDelta = mekanismTemperature - referenceTemperature}.
 * A positive delta produces heat only; a non-positive delta produces cooling
 * only. Heat and cooling are never both non-zero.
 */
public final class MekanismHeatConversion {

    private MekanismHeatConversion() {
    }

    public record Output(double heatOutput, double coolingOutput) {
    }

    /**
     * @throws IllegalArgumentException if any argument is NaN or infinite
     */
    public static Output convert(double mekanismTemperature, double referenceTemperature, double conversionCoefficient) {
        requireFinite(mekanismTemperature, "mekanismTemperature");
        requireFinite(referenceTemperature, "referenceTemperature");
        requireFinite(conversionCoefficient, "conversionCoefficient");

        double temperatureDelta = mekanismTemperature - referenceTemperature;
        if (temperatureDelta > 0) {
            return new Output(temperatureDelta * conversionCoefficient, 0.0);
        }
        return new Output(0.0, Math.abs(temperatureDelta) * conversionCoefficient);
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite number, was: " + value);
        }
    }
}

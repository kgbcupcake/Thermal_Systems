package com.marie.thermalsystems.integration.pneumaticcraft;

/**
 * Pure, side-effect-free conversion between a PneumaticCraft: Repressurized
 * heat exchanger's absolute temperature (Kelvin) and Thermal Systems'
 * zone heat/cooling rate model. Independently testable; holds no state and
 * makes no calls into either PNC:R or Thermal Systems.
 *
 * <p>{@code temperatureDelta = exchangerTemperature - referenceTemperature}.
 * A positive delta produces heat only; a non-positive delta produces cooling
 * only. Heat and cooling are never both non-zero.
 */
public final class ThermalExchangerConversion {

    private ThermalExchangerConversion() {
    }

    public record Output(double heatOutput, double coolingOutput) {
    }

    /**
     * @throws IllegalArgumentException if any argument is NaN or infinite
     */
    public static Output convert(double exchangerTemperature, double referenceTemperature, double conversionCoefficient) {
        requireFinite(exchangerTemperature, "exchangerTemperature");
        requireFinite(referenceTemperature, "referenceTemperature");
        requireFinite(conversionCoefficient, "conversionCoefficient");

        double temperatureDelta = exchangerTemperature - referenceTemperature;
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

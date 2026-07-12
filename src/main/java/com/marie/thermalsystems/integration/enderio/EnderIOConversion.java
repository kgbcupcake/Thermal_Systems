package com.marie.thermalsystems.integration.enderio;

/**
 * Pure, side-effect-free conversion from FE energy received through an Ender
 * IO Energy Conduit into Thermal Systems heat output.
 *
 * <p>Verified against the Ender IO 1.21.1 source: Ender IO has no native heat
 * concept and no heat capability of its own. Its "heat conduit" only exists
 * in the optional {@code enderio-modded-conduits} module and relays
 * Mekanism's own {@code IHeatHandler} capability - it requires Mekanism and
 * carries Mekanism's heat model, not Ender IO's. Ender IO's native Energy
 * Conduit ({@code EnergyConduit.canConnectToBlock}) instead connects to any
 * neighboring block that exposes the standard NeoForge
 * {@code Capabilities.EnergyStorage.BLOCK} capability. That is the real,
 * verified integration surface this adapter uses.
 *
 * <p>{@code heatOutput = energyReceivedThisTick * conversionCoefficient}.
 * There is no complementary signal to derive cooling from, so this
 * conversion only ever produces heat - see
 * {@link com.marie.thermalsystems.api.heating.IHeatSource}. No cooling
 * behavior is invented.
 */
public final class EnderIOConversion {

    private EnderIOConversion() {
    }

    /**
     * @throws IllegalArgumentException if energyReceivedThisTick is negative,
     *                                   or conversionCoefficient is not a
     *                                   finite, non-negative number
     */
    public static double convert(long energyReceivedThisTick, double conversionCoefficient) {
        if (energyReceivedThisTick < 0) {
            throw new IllegalArgumentException(
                    "energyReceivedThisTick must not be negative, was: " + energyReceivedThisTick);
        }
        if (Double.isNaN(conversionCoefficient) || Double.isInfinite(conversionCoefficient) || conversionCoefficient < 0) {
            throw new IllegalArgumentException(
                    "conversionCoefficient must be a finite, non-negative number, was: " + conversionCoefficient);
        }
        return energyReceivedThisTick * conversionCoefficient;
    }
}

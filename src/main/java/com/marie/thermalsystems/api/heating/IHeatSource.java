package com.marie.thermalsystems.api.heating;

/**
 * Contract for a source of heat that can be attached to a climate zone.
 */
public interface IHeatSource {

    /**
     * Returns the current heat output of this source, in degrees Celsius per
     * simulation second, before any coefficients are applied. Implementations
     * must compute this from real state rather than returning a constant.
     */
    double getHeatOutput();

    /**
     * Identifies the network this source's heat is attributed to, if any.
     * Callers that sum {@link #getHeatOutput()} across multiple sources
     * (e.g. {@code SourceRadiationTickHandler}, resolving several in-range
     * positions) must sum a given network's output only once, no matter how
     * many of its positions are in range - two sources with
     * {@link Object#equals} network ids are the same network. Returns
     * {@code null} by default, meaning this source has no network of its
     * own and should always be summed individually.
     */
    default Object getNetworkId() {
        return null;
    }
}

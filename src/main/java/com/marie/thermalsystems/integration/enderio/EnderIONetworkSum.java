package com.marie.thermalsystems.integration.enderio;

import java.util.List;

/**
 * Pure summation of already-collected heat outputs from every Thermal-
 * Systems-capable block reachable on an Ender IO conduit network.
 * Deliberately knows nothing about capabilities, positions, or the world -
 * kept separate from {@link EnderIONetworkDiscovery} so the arithmetic can
 * be unit tested against a fake list of outputs without a running game.
 */
final class EnderIONetworkSum {

    private EnderIONetworkSum() {
    }

    static double sum(List<Double> outputs) {
        double total = 0.0;
        for (double output : outputs) {
            total += output;
        }
        return total;
    }
}

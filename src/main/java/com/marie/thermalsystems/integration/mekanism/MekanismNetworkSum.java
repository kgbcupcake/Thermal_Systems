package com.marie.thermalsystems.integration.mekanism;

import java.util.List;

/**
 * Pure summation of already-collected heat/cooling outputs from every
 * Thermal-Systems-capable block reachable on a Mekanism cable network.
 * Deliberately knows nothing about capabilities, positions, or the world -
 * kept separate from {@link MekanismNetworkDiscovery} so the arithmetic can
 * be unit tested against a fake list of outputs without a running game.
 */
final class MekanismNetworkSum {

    private MekanismNetworkSum() {
    }

    static double sum(List<Double> outputs) {
        double total = 0.0;
        for (double output : outputs) {
            total += output;
        }
        return total;
    }
}

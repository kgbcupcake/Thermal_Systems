package com.marie.thermalsystems.integration.enderio;

/**
 * Whether an Ender IO Stirling Generator adapter currently feeds a zone's
 * heat or cooling side. Mutually exclusive by design, unlike Mekanism's
 * temperature-delta-driven split - see {@link EnderIOBlockHeatSource}.
 */
enum ThermalMode {
    HEAT,
    COOL
}

package com.marie.thermalsystems.integration.enderio;

import com.mojang.serialization.Codec;

/**
 * Whether an Ender IO Stirling Generator adapter currently feeds a zone's
 * heat or cooling side. Mutually exclusive by design, unlike Mekanism's
 * temperature-delta-driven split - see {@link EnderIOBlockHeatSource}.
 */
enum ThermalMode {
    HEAT,
    COOL;

    static final Codec<ThermalMode> CODEC = Codec.STRING.xmap(ThermalMode::valueOf, Enum::name);
}

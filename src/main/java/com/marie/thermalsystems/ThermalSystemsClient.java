package com.marie.thermalsystems;

import com.marie.thermalsystems.integration.enderio.EnderIOClientIntegration;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/**
 * Client-only entry point, a dist-specific {@code @Mod} class alongside the
 * common {@link ThermalSystemsMod} - the same pattern Marie's Lib itself
 * uses for {@code MariesLibClient} alongside {@code MarieCore}. Only ever
 * loaded on the physical client; {@code @Mod(dist = Dist.CLIENT)} guarantees
 * this class (and anything it references, like {@link EnderIOClientIntegration})
 * is never loaded on a dedicated server, so client-only types (screens,
 * render contexts) can be referenced freely from here without risking a
 * {@code NoClassDefFoundError} server-side.
 */
@Mod(value = ThermalSystemsMod.MOD_ID, dist = Dist.CLIENT)
public final class ThermalSystemsClient {

    public ThermalSystemsClient(IEventBus modEventBus) {
        if (ModList.get().isLoaded(EnderIOIntegration.ENDERIO_MOD_ID)) {
            EnderIOClientIntegration.init(modEventBus);
        }
    }
}

package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.persistence.MarieConfigPersistenceProvider;

/**
 * Single shared {@link PersistenceProvider} for every independently-positionable marie-ui
 * component across the whole mod - originally just {@link HeatCoolToggleComponent}, now also
 * {@code com.marie.thermalsystems.client.hud.ThermalSystemsControlPanel}. Mirrors Nourished's
 * {@code UiStatePersistence}: one {@code thermalsystems-ui-state.json} under config/, keyed by
 * component id, backed by {@link MarieConfigPersistenceProvider}'s own in-memory cache.
 *
 * <p>Public (despite living in {@code integration.enderio}, this class's original home) so other
 * packages can reuse this exact instance rather than constructing a second
 * {@link MarieConfigPersistenceProvider} against the same underlying file - two independent
 * instances would each hold their own lazily-loaded in-memory cache of that one file, and a
 * {@code save()} on one would silently drop whatever key the other had written but not yet
 * reloaded. Kept in place rather than moved/renamed to avoid unrelated churn on
 * {@link EnderIOClientIntegration}'s existing references.
 */
public final class EnderIOUiPersistence {

    private static final PersistenceProvider INSTANCE = new MarieConfigPersistenceProvider(ThermalSystemsMod.MOD_ID);

    private EnderIOUiPersistence() {
    }

    public static PersistenceProvider get() {
        return INSTANCE;
    }
}

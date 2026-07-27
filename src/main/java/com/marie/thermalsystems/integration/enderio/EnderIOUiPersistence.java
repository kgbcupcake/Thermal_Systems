package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.persistence.MarieConfigPersistenceProvider;

/**
 * Single shared {@link PersistenceProvider} for every independently-positionable marie-ui
 * component this integration renders - just {@link HeatCoolToggleComponent} today. Mirrors
 * Nourished's {@code UiStatePersistence}: one {@code thermalsystems-ui-state.json} under config/,
 * keyed by component id, backed by {@link MarieConfigPersistenceProvider}'s own in-memory cache.
 */
final class EnderIOUiPersistence {

    private static final PersistenceProvider INSTANCE = new MarieConfigPersistenceProvider(ThermalSystemsMod.MOD_ID);

    private EnderIOUiPersistence() {
    }

    static PersistenceProvider get() {
        return INSTANCE;
    }
}

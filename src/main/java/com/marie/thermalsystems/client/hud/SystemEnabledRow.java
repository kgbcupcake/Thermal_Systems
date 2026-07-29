package com.marie.thermalsystems.client.hud;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.hud.SystemToggleRequestPayload;

/**
 * The panel's first (and, for now, only) row - a toggle bound to {@link ThermalConfig#SYSTEM_ENABLED}.
 * Reads the client's own local config value directly rather than round-tripping a server query -
 * see {@link SystemToggleRequestPayload}'s javadoc for why that's an accepted simplification here.
 */
final class SystemEnabledRow implements ControlPanelRow {

    @Override
    public String label() {
        return "System Enabled";
    }

    @Override
    public boolean isEnabled() {
        return ThermalConfig.SYSTEM_ENABLED.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
        SystemToggleRequestPayload.sendToServer(enabled);
    }
}

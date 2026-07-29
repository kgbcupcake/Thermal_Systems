package com.marie.thermalsystems.client.hud;

/**
 * One toggleable row in {@link ThermalSystemsControlPanel}'s app list. {@code ThermalSystemsControlPanel}
 * holds a {@code List<ControlPanelRow>} rather than a single hardcoded row, so adding a second app
 * (e.g. a per-integration toggle) is just another implementation added to that list, not a
 * structural change to the panel itself.
 */
interface ControlPanelRow {

    /** Text drawn on this row - kept short, the row is not wide. */
    String label();

    /** Current on/off state, read fresh on every render/click - never cached by the row itself. */
    boolean isEnabled();

    /** Requests the new state. Implementations decide how (and whether) that's actually applied. */
    void setEnabled(boolean enabled);
}

package com.marie.thermalsystems.client.hud;

import com.marie.thermalsystems.integration.enderio.EnderIOUiPersistence;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Bounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent, always-available control panel HUD - independent of any machine screen, unlike
 * {@code HeatCoolToggleComponent}, which only exists while a Stirling Generator's own container is
 * open. Rendered as an app list: a title bar ("Thermal Systems") followed by one row per
 * {@link ControlPanelRow} in {@link #rows} - currently just {@link SystemEnabledRow}, but the row
 * list (not a single hardcoded row) is what a second app (e.g. a per-integration toggle) would be
 * added to.
 *
 * <p>Only ever visible/interactive while wrapped in a {@code dev.marie.framework.ui.edit.EditModeController}'s
 * overlay - see {@link ThermalSystemsHudClient} - since Minecraft has no on-screen mouse cursor
 * during ordinary gameplay (no {@code Screen} open) for a HUD element to be clicked or dragged
 * against in the first place. Composes a {@link DraggableResizable} field (per {@link
 * DraggableResizable}'s own class javadoc: a field any {@link MarieComponent} holds for gesture
 * support, not a base class to inherit from) for repositioning; {@link #constraint()} is
 * {@link Constraint#fixed}, so resize gestures are geometrically possible but clamped inert -
 * the panel's height is derived from {@link #rows}' size, not a user preference, mirroring how
 * {@code HeatCoolToggleComponent} also only ever supports repositioning, not resizing.
 *
 * <p>Row toggle boxes are hit-tested first in {@link #mouseClicked}; anything else within the
 * panel's bounds falls through to {@link #drag}, since {@link DraggableResizable#mouseClicked}
 * treats a click anywhere in the supplied bounds (not just its resize handle) as the start of a
 * whole-panel drag. That ordering is what lets clicking a toggle and dragging the panel body
 * coexist without a separate "reposition mode" flag like {@code EnderIOClientIntegration} needs -
 * there, the whole component's bounds were the click target, leaving nothing to fall through to.
 */
final class ThermalSystemsControlPanel implements MarieComponent {

    private static final int WIDTH = 140;
    private static final int TITLE_HEIGHT = 14;
    private static final int ROW_HEIGHT = 16;
    private static final int TOGGLE_WIDTH = 40;
    private static final int TOGGLE_HEIGHT = 12;
    private static final int PADDING = 3;

    /** Persistence key for this panel's {@link ComponentState} - stable across sessions/worlds. */
    private static final String PERSISTENCE_ID = "thermalsystems.control_panel";

    private static final int TITLE_BG_COLOR = 0xFF1A1A1A;
    private static final int TITLE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int ROW_BG_COLOR = 0xFF2A2A2A;
    private static final int ROW_ALT_BG_COLOR = 0xFF242424;
    private static final int LABEL_TEXT_COLOR = 0xFFDDDDDD;

    /** Matches {@code HeatCoolToggleComponent}'s HEAT/COOL palette shape: one active, one inactive half. */
    private static final int ON_ACTIVE_COLOR = 0xFF339944;
    private static final int ON_INACTIVE_COLOR = 0xFF223322;
    private static final int OFF_ACTIVE_COLOR = 0xFFCC4433;
    private static final int OFF_INACTIVE_COLOR = 0xFF442222;
    private static final int TOGGLE_BORDER_COLOR = 0xFF000000;
    private static final int TOGGLE_TEXT_COLOR = 0xFFFFFFFF;

    private final List<ControlPanelRow> rows = List.of(new SystemEnabledRow());

    private final DraggableResizable drag = new DraggableResizable(this, constraint(), this::onDragCommitted);

    @Nullable
    private Bounds lastBounds;
    private List<Bounds> lastToggleBounds = List.of();

    @Override
    public String id() {
        return "thermalsystems.hud.control_panel";
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(WIDTH, TITLE_HEIGHT + rows.size() * ROW_HEIGHT);
    }

    /**
     * {@code bounds} here is the full screen ({@code EditOverlayScreen} always passes {@code (0, 0,
     * screenWidth, screenHeight)} to its target, since it has no notion of the target's own
     * position/size) - the panel's actual on-screen {@link Bounds} are resolved internally by
     * {@link #resolveBounds}, exactly the geometry a {@code Container}/layout would otherwise own.
     */
    @Override
    public void render(RenderContext context, Bounds screenBounds) {
        drag.setParentBounds(screenBounds);
        Bounds bounds = resolveBounds(screenBounds);
        this.lastBounds = bounds;

        context.fillRect(bounds.x(), bounds.y(), bounds.width(), TITLE_HEIGHT, TITLE_BG_COLOR);
        context.drawText("Thermal Systems", bounds.x() + PADDING, bounds.y() + 3, TITLE_TEXT_COLOR, 0.8f);

        List<Bounds> toggleBoundsList = new ArrayList<>(rows.size());
        int rowY = bounds.y() + TITLE_HEIGHT;
        for (int i = 0; i < rows.size(); i++) {
            ControlPanelRow row = rows.get(i);
            int rowBg = (i % 2 == 0) ? ROW_BG_COLOR : ROW_ALT_BG_COLOR;
            context.fillRect(bounds.x(), rowY, bounds.width(), ROW_HEIGHT, rowBg);
            context.drawText(row.label(), bounds.x() + PADDING, rowY + 4, LABEL_TEXT_COLOR, 0.7f);

            int toggleX = bounds.x() + bounds.width() - PADDING - TOGGLE_WIDTH;
            int toggleY = rowY + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
            Bounds toggleBounds = new Bounds(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT);
            renderToggle(context, toggleBounds, row.isEnabled());
            toggleBoundsList.add(toggleBounds);

            rowY += ROW_HEIGHT;
        }
        this.lastToggleBounds = toggleBoundsList;

        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, TOGGLE_BORDER_COLOR);
    }

    /** Split ON/OFF box - deliberately the same two-halves-plus-border-plus-text shape as {@code HeatCoolToggleComponent}. */
    private void renderToggle(RenderContext context, Bounds bounds, boolean on) {
        int onWidth = bounds.width() / 2;
        int offWidth = bounds.width() - onWidth;

        context.fillRect(bounds.x(), bounds.y(), onWidth, bounds.height(), on ? ON_ACTIVE_COLOR : ON_INACTIVE_COLOR);
        context.fillRect(bounds.x() + onWidth, bounds.y(), offWidth, bounds.height(), on ? OFF_INACTIVE_COLOR : OFF_ACTIVE_COLOR);
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, TOGGLE_BORDER_COLOR);
        context.drawText("ON", bounds.x() + 2, bounds.y() + 2, TOGGLE_TEXT_COLOR, 0.6f);
        context.drawText("OFF", bounds.x() + onWidth + 2, bounds.y() + 2, TOGGLE_TEXT_COLOR, 0.6f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || lastBounds == null) {
            return false;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        for (int i = 0; i < lastToggleBounds.size(); i++) {
            if (lastToggleBounds.get(i).contains(mx, my)) {
                ControlPanelRow row = rows.get(i);
                row.setEnabled(!row.isEnabled());
                return true;
            }
        }
        return drag.mouseClicked(mx, my, lastBounds);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!drag.isDragging() && !drag.isResizing()) {
            return false;
        }
        Bounds preview = drag.mouseDragged((int) mouseX, (int) mouseY);
        if (preview != null) {
            lastBounds = preview;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!drag.isDragging() && !drag.isResizing()) {
            return false;
        }
        drag.mouseReleased((int) mouseX, (int) mouseY);
        return true;
    }

    /**
     * Live drag/resize preview (already tracked in {@link #lastBounds} by {@link #mouseDragged})
     * takes priority over the persisted {@link ComponentState}, else a small fixed default anchored
     * near the screen's top-left corner.
     */
    private Bounds resolveBounds(Bounds screenBounds) {
        if ((drag.isDragging() || drag.isResizing()) && lastBounds != null) {
            return lastBounds;
        }
        PersistenceProvider persistence = EnderIOUiPersistence.get();
        return persistence.load(PERSISTENCE_ID)
                .map(state -> new Bounds(state.x(), state.y(), state.width(), state.height()))
                .orElseGet(() -> new Bounds(screenBounds.x() + 8, screenBounds.y() + 8,
                        WIDTH, TITLE_HEIGHT + rows.size() * ROW_HEIGHT));
    }

    /** {@link DraggableResizable#mouseReleased}'s {@code onCommit} callback - persists absolute screen coordinates directly. */
    private void onDragCommitted(MarieComponent target, Bounds committedBounds) {
        EnderIOUiPersistence.get().save(PERSISTENCE_ID, new ComponentState(
                committedBounds.x(), committedBounds.y(), committedBounds.width(), committedBounds.height(),
                false, false, false, 0));
        lastBounds = committedBounds;
    }
}

package com.marie.thermalsystems.integration.enderio;

import dev.marie.framework.network.GenericStateSyncPayload;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.Nullable;

/**
 * A HEAT/COOL toggle rendered over Ender IO's Stirling Generator screen (see
 * {@link EnderIOClientIntegration}), replacing {@code /thermal enderio setMode}
 * as the primary control surface for a given generator - the command remains
 * as a fallback, unchanged.
 *
 * <p>{@link #mode} starts {@code null} - "not yet known" - rather than
 * guessing {@link ThermalMode#HEAT}, since a generator switched via the
 * command (or by a previous session) may genuinely be in either mode.
 * {@link EnderIOClientIntegration} sends an {@link EnderIOModeRequestPayload}
 * as soon as this component is created; {@link #applyServerMode} is called
 * once the {@link EnderIOModeResponsePayload} reply arrives. Until then,
 * {@link #render} draws a neutral loading state rather than a
 * confidently-wrong HEAT or COOL, and {@link #mouseClicked} absorbs clicks
 * without acting on them - there's nothing to toggle from an unknown state.
 *
 * <p>Not driven through a {@code Container}/{@code Layout} - {@link
 * EnderIOClientIntegration} recomputes this component's {@link Bounds} from
 * the tracked screen's own panel geometry every frame and passes them
 * directly to {@link #render}, which caches them as a fallback for {@code
 * MarieComponent}'s parameterless {@link #mouseClicked(double, double, int)};
 * the primary click path, {@link #mouseClicked(Bounds, double, double, int)},
 * instead takes bounds recomputed fresh for that click.
 */
final class HeatCoolToggleComponent implements MarieComponent {

    static final int WIDTH = 60;
    static final int HEIGHT = 16;

    private static final int HEAT_ACTIVE_COLOR = 0xFFCC4433;
    private static final int HEAT_INACTIVE_COLOR = 0xFF442222;
    private static final int COOL_ACTIVE_COLOR = 0xFF3366CC;
    private static final int COOL_INACTIVE_COLOR = 0xFF223344;
    private static final int LOADING_COLOR = 0xFF3A3A3A;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int LOADING_TEXT_COLOR = 0xFFAAAAAA;

    private final BlockPos pos;
    @Nullable
    private ThermalMode mode;
    @Nullable
    private Bounds lastBounds;

    HeatCoolToggleComponent(BlockPos pos) {
        this.pos = pos.immutable();
    }

    BlockPos pos() {
        return pos;
    }

    /**
     * Applies the mode reported by {@link EnderIOModeResponsePayload}. Only
     * ever called once, by {@link EnderIOClientIntegration}, after
     * confirming the response is actually for this component's {@link #pos}.
     */
    void applyServerMode(ThermalMode mode) {
        this.mode = mode;
    }

    @Override
    public String id() {
        return "thermalsystems.enderio.heat_cool_toggle";
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(WIDTH, HEIGHT);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        this.lastBounds = bounds;

        if (mode == null) {
            context.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), LOADING_COLOR);
            context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, BORDER_COLOR);
            context.drawText("...", bounds.x() + 3, bounds.y() + 4, LOADING_TEXT_COLOR, 0.7f);
            return;
        }

        int heatWidth = bounds.width() / 2;
        int coolWidth = bounds.width() - heatWidth;
        boolean heatActive = mode == ThermalMode.HEAT;

        context.fillRect(bounds.x(), bounds.y(), heatWidth, bounds.height(),
                heatActive ? HEAT_ACTIVE_COLOR : HEAT_INACTIVE_COLOR);
        context.fillRect(bounds.x() + heatWidth, bounds.y(), coolWidth, bounds.height(),
                heatActive ? COOL_INACTIVE_COLOR : COOL_ACTIVE_COLOR);
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, BORDER_COLOR);
        context.drawText("HEAT", bounds.x() + 3, bounds.y() + 4, TEXT_COLOR, 0.7f);
        context.drawText("COOL", bounds.x() + heatWidth + 3, bounds.y() + 4, TEXT_COLOR, 0.7f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(lastBounds, mouseX, mouseY, button);
    }

    /**
     * Hit-tests against caller-supplied {@code bounds} rather than {@link
     * #lastBounds} - {@link EnderIOClientIntegration} recomputes the
     * toggle's position from the screen's current panel geometry on every
     * click, since that geometry (unlike a fixed HUD coordinate) could in
     * principle change between the last render and this click.
     */
    boolean mouseClicked(@Nullable Bounds bounds, double mouseX, double mouseY, int button) {
        if (button != 0 || bounds == null || !bounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        if (mode == null) {
            return true;
        }
        boolean clickedHeatHalf = mouseX < bounds.x() + bounds.width() / 2.0;
        ThermalMode clicked = clickedHeatHalf ? ThermalMode.HEAT : ThermalMode.COOL;
        if (clicked != mode) {
            mode = clicked;
            sendModeChange(clicked);
        }
        return true;
    }

    private void sendModeChange(ThermalMode mode) {
        CompoundTag data = new CompoundTag();
        data.putString("mode", mode.name());
        GenericStateSyncPayload.sendToServer(pos, data);
    }
}

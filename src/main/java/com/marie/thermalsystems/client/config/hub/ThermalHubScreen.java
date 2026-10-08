package com.marie.thermalsystems.client.config.hub;

import com.marie.thermalsystems.integration.enderio.EnderIOUiPersistence;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.hub.HubPanel;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Thermal Systems' config editor: a Marie's lib {@link HubPanel} (sidebar of module boxes, tabbed
 * settings, draggable/resizable window) over every {@code ThermalConfig} value - the same window
 * Nourished's Diet editor uses. Opened from the Mods list's Config button, Marie's lib's context and
 * the {@code key.thermalsystems.openConfig} keybind.
 *
 * <p>The hub and its panels are built once per screen, never per frame: rebuilding them would drop
 * each panel's scroll position and the hub's color-picker wiring.
 */
public final class ThermalHubScreen extends Screen {

    private static final String HUB_ID = "thermalsystems.hub";

    private final Screen parent;
    private final ThermalConfigClientModel model;
    private final HubPanel hub;

    /**
     * Returns {@link Screen} rather than this type so common-side lambdas calling it (see
     * {@code ThermalSystemsMod}) never make the verifier load this class on a dedicated server.
     */
    public static Screen create(Screen parent) {
        return new ThermalHubScreen(parent);
    }

    private ThermalHubScreen(Screen parent) {
        super(Component.translatable("gui.thermalsystems.hub.title"));
        this.parent = parent;
        this.model = ThermalConfigClientModel.open();
        this.hub = new HubPanel(getTitle(), HUB_ID, EnderIOUiPersistence.get(), ThermalHubPages.entries(model, this, this::reopen));
    }

    /** Replaces this screen with a freshly built one, e.g. so every slider picks up a new display unit. */
    private void reopen() {
        model.close();
        minecraft.setScreen(new ThermalHubScreen(parent));
    }

    @Override
    protected void init() {
        super.init();
        // Also runs on return from the Export/Import/classic screens, which may have changed the config.
        model.reload();
    }

    @Override
    public void tick() {
        model.flushIfCommitted();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(graphics, minecraft, Theme.DARK, partialTick);
        try {
            hub.render(context, new Bounds(0, 0, width, height));
        } finally {
            context.resetClip();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return hub.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return hub.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return hub.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return hub.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        model.flush();
    }

    @Override
    public void onClose() {
        model.close();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        // A paused singleplayer server wouldn't apply edits until unpaused.
        return false;
    }
}

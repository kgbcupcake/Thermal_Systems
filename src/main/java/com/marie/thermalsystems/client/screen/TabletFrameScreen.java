package com.marie.thermalsystems.client.screen;

import com.marie.thermalsystems.client.ClientTemperatureUnit;
import com.marie.thermalsystems.client.zone.ClientZones;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.ui.toolbox.OptionStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared chrome for the thermostat tablet and Zone Gadget screens: a tablet bezel with a header
 * (title, optional back arrow) around a content area, plus lightweight buttons that subclasses
 * declare while drawing - {@link #button} draws one and remembers where it went for this frame's
 * clicks. Keeps the zone list fresh while open via {@link ClientZones#poll}. The header's right-hand
 * button switches the display unit between Celsius and Fahrenheit.
 */
abstract class TabletFrameScreen extends Screen {

    protected static final int FRAME_WIDTH = 248;
    protected static final int FRAME_HEIGHT = 186;
    private static final int BEZEL = 7;
    private static final int HEADER_HEIGHT = 16;
    private static final int UNIT_BUTTON_WIDTH = 18;
    private static final long POLL_MS = 1000;

    protected static final int HOT = 0xFFFF8A4C;
    protected static final int COLD = 0xFF5AB4FF;
    protected static final int GOOD = 0xFF5BD47A;
    protected static final int BAD = 0xFFE05A5A;
    protected static final int MUTED = 0xFF8A8F99;

    private static final int BEZEL_FILL = 0xFF1B1E24;
    private static final int BEZEL_BORDER = 0xFF3A3F4A;
    private static final int GLASS = 0xFF0E1014;
    private static final int BUTTON_FILL = 0xFF232831;
    private static final int BUTTON_HOVER = 0xFF2E3542;
    private static final int BUTTON_DISABLED = 0xFF181B20;

    private record Hit(Bounds bounds, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();

    protected TabletFrameScreen(Component title) {
        super(title);
    }

    protected int frameWidth() {
        return FRAME_WIDTH;
    }

    protected int frameHeight() {
        return FRAME_HEIGHT;
    }

    protected int frameLeft() {
        return (width - frameWidth()) / 2;
    }

    protected int frameTop() {
        return (height - frameHeight()) / 2;
    }

    /** The content area inside the bezel, below the header. */
    protected Bounds contentBounds() {
        return new Bounds(frameLeft() + BEZEL + 4, frameTop() + BEZEL + HEADER_HEIGHT + 2,
                frameWidth() - 2 * BEZEL - 8, frameHeight() - 2 * BEZEL - HEADER_HEIGHT - 6);
    }

    /** Header title; defaults to the screen title. */
    protected Component header() {
        return getTitle();
    }

    /** What the header's back arrow does, or {@code null} to hide it. */
    @Nullable
    protected Runnable backAction() {
        return null;
    }

    protected abstract void renderContent(RenderContext context, Bounds area, int mouseX, int mouseY);

    @Override
    protected void init() {
        super.init();
        ClientZones.requestNow();
    }

    @Override
    public void tick() {
        ClientZones.poll(POLL_MS);
    }

    /**
     * The tablet is drawn here rather than in {@link #render} because {@code Screen#render} paints
     * the background and then its widgets (the gadget menu's name box) in one call - drawing after it
     * would cover those widgets.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(graphics, minecraft, Theme.DARK, partialTick);
        hits.clear();
        try {
            drawFrame(context, mouseX, mouseY);
            renderContent(context, contentBounds(), mouseX, mouseY);
        } finally {
            context.resetClip();
        }
    }

    private void drawFrame(RenderContext context, int mouseX, int mouseY) {
        int x = frameLeft();
        int y = frameTop();
        int frameWidth = frameWidth();
        context.drawRoundedRect(x, y, frameWidth, frameHeight(), 2, 6, BEZEL_FILL, BEZEL_BORDER);
        context.fillRect(x + BEZEL, y + BEZEL, frameWidth - 2 * BEZEL, frameHeight() - 2 * BEZEL, GLASS);

        Theme theme = context.theme();
        int headerY = y + BEZEL;
        int titleX = x + BEZEL + 5;
        Runnable back = backAction();
        if (back != null) {
            button(context, x + BEZEL + 2, headerY + 2, 14, 12, Component.literal("<"), true, false, 0, mouseX, mouseY, back);
            titleX += 16;
        }
        int unitX = x + frameWidth - BEZEL - 2 - UNIT_BUTTON_WIDTH;
        button(context, unitX, headerY + 2, UNIT_BUTTON_WIDTH, 12, Component.literal(ClientTemperatureUnit.get().symbol()),
                true, false, 0, mouseX, mouseY, ClientTemperatureUnit::toggle);
        String title = OptionStyle.fit(context, header().getString(), 1f, unitX - titleX - 4);
        context.drawText(title, titleX, headerY + 4, theme.color(ThemeKey.TEXT_PRIMARY), 1f);
        context.fillRect(x + BEZEL, headerY + HEADER_HEIGHT, frameWidth - 2 * BEZEL, 1, theme.color(ThemeKey.BORDER));
    }

    /**
     * Draws a button and registers it for clicks this frame. {@code active} fills it with
     * {@code accent} (a selected tab or mode); disabled buttons are dimmed and ignore clicks.
     */
    protected void button(RenderContext context, int x, int y, int w, int h, Component label, boolean enabled,
                          boolean active, int accent, int mouseX, int mouseY, Runnable action) {
        Bounds bounds = new Bounds(x, y, w, h);
        boolean hovered = enabled && bounds.contains(mouseX, mouseY);
        int fill = !enabled ? BUTTON_DISABLED : active ? darken(accent) : hovered ? BUTTON_HOVER : BUTTON_FILL;
        int border = active ? accent : hovered ? context.theme().color(ThemeKey.BORDER_HOVER) : context.theme().color(ThemeKey.BORDER);
        context.drawRoundedRect(x, y, w, h, 1, 2, fill, border);
        String text = OptionStyle.fit(context, label.getString(), 0.8f, w - 4);
        int textColor = !enabled ? MUTED : active ? 0xFFFFFFFF : context.theme().color(ThemeKey.TEXT_PRIMARY);
        context.drawText(text, x + (w - context.textWidth(text, 0.8f)) / 2, y + (h - 6) / 2, textColor, 0.8f);
        if (enabled) {
            hits.add(new Hit(bounds, action));
        }
    }

    /** Registers a click target for this frame without drawing anything. */
    protected void clickable(Bounds bounds, Runnable action) {
        hits.add(new Hit(bounds, action));
    }

    protected static void centered(RenderContext context, String text, int centerX, int y, int color, float scale) {
        context.drawText(text, centerX - context.textWidth(text, scale) / 2, y, color, scale);
    }

    /** {@code celsius} in the player's chosen display unit. */
    protected static String temperature(double celsius) {
        return ClientTemperatureUnit.format(celsius);
    }

    /** {@code targetCelsius} one +/- press away, in whole steps of the display unit (Shift for big steps). */
    protected static double stepTarget(double targetCelsius, int direction) {
        return ClientTemperatureUnit.get().step(targetCelsius, direction, hasShiftDown());
    }

    protected static String text(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static int darken(int argb) {
        int r = ((argb >> 16) & 0xFF) * 2 / 5;
        int g = ((argb >> 8) & 0xFF) * 2 / 5;
        int b = (argb & 0xFF) * 2 / 5;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Hit hit : List.copyOf(hits)) {
                if (hit.bounds().contains((int) mouseX, (int) mouseY)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        // A paused singleplayer server wouldn't apply edits until unpaused.
        return false;
    }
}

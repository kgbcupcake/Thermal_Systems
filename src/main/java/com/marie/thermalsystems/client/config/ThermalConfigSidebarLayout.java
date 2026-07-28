package com.marie.thermalsystems.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repositions Cloth's own category tabs into a left sidebar and routes
 * clicks through a dedicated widget. Cloth Config has no built-in
 * sidebar/nav shell to build on (nor does Marie's Lib - MarieComponent /
 * RenderContext power a separate HUD-overlay system, unrelated to config
 * screens), so this reaches into {@link ClothConfigScreen}'s private fields
 * via reflection, same approach used by other Marie mods' config screens.
 *
 * <p>Every Cloth field entry ({@code TextFieldListEntry}, {@code
 * IntegerSliderEntry}, etc.) hardcodes its widget at a fixed
 * {@code x + entryWidth - 148} (text fields) or {@code - 150} (sliders)
 * offset and draws its label unclipped at {@code x} - there is no
 * width-aware truncation hook to inject into. Shrinking the row width for
 * the sidebar therefore risks the label overlapping the widget once a
 * label's pixel width plus that fixed reservation exceeds the row width.
 * Rather than fight Cloth's fixed per-entry layout, {@link
 * #minSafeContentWidth} measures the widest field label actually present
 * across every category and floors the content panel's width there, so the
 * fixed reservation always has room and the overlap can't occur at any
 * window size.
 */
final class ThermalConfigSidebarLayout {

    private static final int NAV_LEFT = 10;
    private static final int NAV_TOP = 38;
    private static final int NAV_WIDTH = 120;
    private static final int NAV_WIDTH_MIN = 84;
    private static final int NAV_ITEM_H = 20;
    private static final int NAV_GAP = 4;
    private static final int CONTENT_GAP = 8;
    private static final int CONTENT_RIGHT_PAD = 12;
    private static final int CONTENT_ABSOLUTE_MIN_WIDTH = 180;

    /**
     * Cloth's largest fixed right-side reservation across the entry types
     * this screen uses (150 for sliders, 148 for text fields) plus a couple
     * pixels of slack before the label's last glyph would touch it.
     */
    private static final int FIELD_RESERVED_WIDTH = 154;

    private static final int COL_SELECTED = 0xFF3A6EA5;
    private static final int COL_UNSELECTED = 0xFF1A1A1A;
    private static final int COL_UNSELECTED_BORDER = 0xFF333333;
    private static final int COL_TEXT = 0xFFE0E0E0;

    private ThermalConfigSidebarLayout() {}

    static void apply(Screen screen) {
        if (!(screen instanceof ClothConfigScreen cloth)) {
            return;
        }
        layoutLeftCards(cloth);
    }

    private static void layoutLeftCards(ClothConfigScreen cloth) {
        List<ClothConfigTabButton> tabs = getField(cloth, "tabButtons");
        if (tabs == null || tabs.isEmpty()) {
            return;
        }

        int minContentWidth = minSafeContentWidth(cloth);
        LayoutMetrics metrics = computeMetrics(cloth.width, minContentWidth);
        applyTabLayout(tabs, metrics);

        AbstractWidget left = getField(cloth, "buttonLeftTab");
        AbstractWidget right = getField(cloth, "buttonRightTab");
        hideWidget(left);
        hideWidget(right);
        ensureSidebarWidget(cloth, tabs, metrics, minContentWidth);
        ensureLayoutKeeper(cloth, tabs, minContentWidth);

        applyContentLayout(cloth, metrics);
    }

    /**
     * Widest field label across every category, converted into the
     * smallest content-panel width Cloth can render without its fixed
     * widget offset overlapping that label. Computed once per screen (the
     * label set is fixed for the screen's lifetime), not per frame.
     */
    private static int minSafeContentWidth(ClothConfigScreen cloth) {
        Map<Component, List<Object>> categorizedEntries = getField(cloth, "categorizedEntries");
        if (categorizedEntries == null) {
            return CONTENT_ABSOLUTE_MIN_WIDTH;
        }
        Font font = Minecraft.getInstance().font;
        int maxLabelWidth = 0;
        for (List<Object> entries : categorizedEntries.values()) {
            for (Object entry : entries) {
                if (entry instanceof AbstractConfigListEntry<?> listEntry) {
                    maxLabelWidth = Math.max(maxLabelWidth, font.width(listEntry.getDisplayedFieldName()));
                }
            }
        }
        return Math.max(CONTENT_ABSOLUTE_MIN_WIDTH, maxLabelWidth + FIELD_RESERVED_WIDTH);
    }

    private static void ensureLayoutKeeper(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, int minContentWidth) {
        List<Renderable> renderables = getField(cloth, "renderables");
        if (renderables == null) {
            return;
        }
        for (Renderable renderable : renderables) {
            if (renderable instanceof LayoutKeeperWidget) {
                return;
            }
        }
        LayoutKeeperWidget keeper = new LayoutKeeperWidget(cloth, tabs, minContentWidth);
        renderables.add(0, keeper);
    }

    private static void hideWidget(AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        widget.visible = false;
        widget.active = false;
        widget.setX(-2000);
        widget.setY(-2000);
    }

    private static void ensureSidebarWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, LayoutMetrics metrics, int minContentWidth) {
        List<Renderable> renderables = getField(cloth, "renderables");
        if (renderables == null) {
            return;
        }
        for (Renderable renderable : renderables) {
            if (renderable instanceof SidebarNavWidget sidebar) {
                sidebar.updateMetrics(metrics);
                return;
            }
        }
        SidebarNavWidget sidebar = new SidebarNavWidget(cloth, tabs, metrics, minContentWidth);
        renderables.add(sidebar);
        List<GuiEventListener> children = getField(cloth, "children");
        if (children != null) {
            children.add(sidebar);
        }
        List<NarratableEntry> narratables = getField(cloth, "narratables");
        if (narratables != null) {
            narratables.add(sidebar);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static LayoutMetrics computeMetrics(int screenWidth, int minContentWidth) {
        int navWidth = screenWidth < 500 ? NAV_WIDTH_MIN : NAV_WIDTH;
        int contentLeft = NAV_LEFT + navWidth + CONTENT_GAP;
        int contentWidth = Math.max(minContentWidth, screenWidth - contentLeft - CONTENT_RIGHT_PAD);
        return new LayoutMetrics(NAV_LEFT, NAV_TOP, navWidth, NAV_ITEM_H, NAV_GAP, contentLeft, contentWidth);
    }

    private static void applyTabLayout(List<ClothConfigTabButton> tabs, LayoutMetrics metrics) {
        int y = metrics.navTop();
        for (ClothConfigTabButton tab : tabs) {
            if (tab == null) {
                continue;
            }
            tab.setX(metrics.navLeft());
            tab.setY(y);
            tab.setWidth(metrics.navWidth());
            tab.visible = false;
            y += metrics.navItemHeight() + metrics.navGap();
        }
    }

    private static void applyContentLayout(ClothConfigScreen cloth, LayoutMetrics metrics) {
        if (cloth.listWidget == null) {
            return;
        }
        cloth.listWidget.updateSize(metrics.contentWidth(), cloth.height, cloth.listWidget.top, cloth.listWidget.bottom);
        cloth.listWidget.setLeftPos(metrics.contentLeft());
    }

    private record LayoutMetrics(
            int navLeft,
            int navTop,
            int navWidth,
            int navItemHeight,
            int navGap,
            int contentLeft,
            int contentWidth
    ) {}

    private static final class LayoutKeeperWidget implements Renderable {
        private final ClothConfigScreen cloth;
        private final List<ClothConfigTabButton> tabs;
        private final int minContentWidth;

        private LayoutKeeperWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, int minContentWidth) {
            this.cloth = cloth;
            this.tabs = tabs;
            this.minContentWidth = minContentWidth;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            LayoutMetrics liveMetrics = computeMetrics(cloth.width, minContentWidth);
            applyTabLayout(tabs, liveMetrics);
            applyContentLayout(cloth, liveMetrics);
        }
    }

    private static final class SidebarNavWidget extends AbstractWidget {
        private final ClothConfigScreen cloth;
        private final List<ClothConfigTabButton> tabs;
        private final int minContentWidth;
        private LayoutMetrics metrics;

        private SidebarNavWidget(ClothConfigScreen cloth, List<ClothConfigTabButton> tabs, LayoutMetrics metrics, int minContentWidth) {
            super(metrics.navLeft(), metrics.navTop(), metrics.navWidth(), 1, Component.empty());
            this.cloth = cloth;
            this.tabs = tabs;
            this.metrics = metrics;
            this.minContentWidth = minContentWidth;
        }

        private void updateMetrics(LayoutMetrics metrics) {
            this.metrics = metrics;
            setX(metrics.navLeft());
            setY(metrics.navTop());
            setWidth(metrics.navWidth());
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            updateDynamicLayout();
            int idx = indexAt(mouseX, mouseY);
            if (idx < 0 || idx >= tabs.size()) {
                return;
            }
            ClothConfigTabButton tab = tabs.get(idx);
            if (tab != null) {
                tab.onPress();
            }
        }

        @Override
        protected void updateWidgetNarration(@Nonnull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {}

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            updateDynamicLayout();
            int y = metrics.navTop();
            var font = Objects.requireNonNull(Minecraft.getInstance().font);
            for (ClothConfigTabButton tab : tabs) {
                if (tab == null) {
                    y += metrics.navItemHeight() + metrics.navGap();
                    continue;
                }
                boolean selected = !tab.active;
                int fill = selected ? COL_SELECTED : COL_UNSELECTED;
                graphics.fill(metrics.navLeft(), y, metrics.navLeft() + metrics.navWidth(), y + metrics.navItemHeight(), fill);
                graphics.renderOutline(metrics.navLeft(), y, metrics.navWidth(), metrics.navItemHeight(), COL_UNSELECTED_BORDER);
                Component label = Objects.requireNonNull(tab.getMessage());
                int maxTextWidth = metrics.navWidth() - 12;
                String text = Objects.requireNonNull(font.plainSubstrByWidth(Objects.requireNonNull(label.getString()), maxTextWidth));
                int textY = y + (metrics.navItemHeight() - font.lineHeight) / 2;
                graphics.drawString(font, text, metrics.navLeft() + 6, textY, COL_TEXT, false);
                y += metrics.navItemHeight() + metrics.navGap();
            }
            setHeight(Math.max(1, y - metrics.navTop()));
        }

        private int indexAt(double mouseX, double mouseY) {
            if (mouseX < metrics.navLeft() || mouseX > metrics.navLeft() + metrics.navWidth()) {
                return -1;
            }
            int y = metrics.navTop();
            for (int i = 0; i < tabs.size(); i++) {
                if (mouseY >= y && mouseY < y + metrics.navItemHeight()) {
                    return i;
                }
                y += metrics.navItemHeight() + metrics.navGap();
            }
            return -1;
        }

        private void updateDynamicLayout() {
            LayoutMetrics liveMetrics = computeMetrics(cloth.width, minContentWidth);
            updateMetrics(liveMetrics);
            applyTabLayout(tabs, liveMetrics);
            applyContentLayout(cloth, liveMetrics);
        }
    }
}

package com.marie.thermalsystems.client.config.hub;

import com.marie.thermalsystems.client.ClientTemperatureUnit;
import com.marie.thermalsystems.data.config.ThermalConfigEntries;
import com.marie.thermalsystems.data.config.ThermalConfigEntries.Entry;
import com.marie.thermalsystems.data.config.sync.HubStatusPayload;
import com.marie.thermalsystems.data.config.sync.HubStatusRequestPayload;
import com.marie.thermalsystems.data.config.sync.ThermalConfigSync;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.marie.framework.ui.toolbox.OptionStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hub's first page: the player rendered in 3D inside a framed box (turning to follow the mouse,
 * like the inventory screen's), beside a scrollable summary of their live climate, the core settings
 * and every integration's state. Live climate comes from the server as a {@link HubStatusPayload},
 * re-requested every {@link #POLL_INTERVAL_MS} while this page is showing.
 */
final class ThermalHomePage implements MarieComponent {

    /** An integration's config section and the mod it hooks into. */
    record Integration(String section, String modId) {

        boolean installed() {
            return ModList.get().isLoaded(modId);
        }
    }

    private static final long POLL_INTERVAL_MS = 1000;
    private static final int PAD = 6;
    private static final int ROW_HEIGHT = 12;
    private static final int HEADER_HEIGHT = 16;
    private static final int SECTION_GAP = 6;
    private static final int SCROLL_STEP = 20;
    private static final int NAME_STRIP_HEIGHT = 14;
    private static final float MODEL_Z_LIFT = 200f;

    private static final int BOX_BACKGROUND = 0xFF15171C;
    private static final int FLOOR_COLOR = 0xFF22262E;
    private static final int GOOD = 0xFF5BD47A;
    private static final int BAD = 0xFFE05A5A;
    private static final int HOT = 0xFFFF8A4C;
    private static final int COLD = 0xFF5AB4FF;
    private static final double COMFORT_BAND = 2.0;

    private final ThermalConfigClientModel model;
    private final List<Integration> integrations;
    private HubStatusPayload status;
    private long lastRequestMs;
    private int scroll;
    private Bounds lastInfoBounds = new Bounds(0, 0, 0, 0);

    ThermalHomePage(ThermalConfigClientModel model, List<Integration> integrations) {
        this.model = model;
        this.integrations = integrations;
        ThermalConfigSync.setStatusListener(payload -> this.status = payload);
    }

    @Override
    public String id() {
        return "thermalsystems.hub.home";
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(320, 260);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        pollStatus(player);

        int boxWidth = Math.max(70, Math.min(110, bounds.width() / 3));
        int boxHeight = Math.min(bounds.height(), Math.max(110, boxWidth * 17 / 10));
        Bounds box = new Bounds(bounds.x(), bounds.y(), boxWidth, boxHeight);
        drawPlayerBox(context, box, player, minecraft);

        int infoX = box.x() + box.width() + PAD * 2;
        lastInfoBounds = new Bounds(infoX, bounds.y(), Math.max(0, bounds.x() + bounds.width() - infoX), bounds.height());
        drawInfo(context, lastInfoBounds, player);
    }

    private void pollStatus(LocalPlayer player) {
        if (player == null || Minecraft.getInstance().getConnection() == null) {
            status = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRequestMs >= POLL_INTERVAL_MS) {
            lastRequestMs = now;
            PacketDistributor.sendToServer(HubStatusRequestPayload.INSTANCE);
        }
    }

    private void drawPlayerBox(RenderContext context, Bounds box, LocalPlayer player, Minecraft minecraft) {
        Theme theme = context.theme();
        int accent = theme.color(ThemeKey.BORDER_HOVER);
        context.drawRoundedRect(box.x(), box.y(), box.width(), box.height(), 1, BOX_BACKGROUND, accent);

        int innerX = box.x() + 2;
        int innerY = box.y() + 2;
        int innerW = box.width() - 4;
        int innerH = box.height() - 4 - NAME_STRIP_HEIGHT;
        int floorY = innerY + innerH - innerH / 6;
        context.fillRect(innerX, floorY, innerW, innerY + innerH - floorY, FLOOR_COLOR);
        context.fillRect(innerX, innerY + innerH, innerW, 1, theme.color(ThemeKey.BORDER));

        String name = player != null ? player.getGameProfile().getName() : text("gui.thermalsystems.hub.home.no_player");
        String fitted = OptionStyle.fit(context, name, 1f, innerW - 4);
        context.drawText(fitted, box.x() + (box.width() - context.textWidth(fitted, 1f)) / 2,
                innerY + innerH + (NAME_STRIP_HEIGHT - 8) / 2 + 1, theme.color(ThemeKey.TEXT_PRIMARY), 1f);

        if (player == null || !(context instanceof GuiGraphicsRenderContext graphicsContext)) {
            String hint = text("gui.thermalsystems.hub.home.join_world");
            String fittedHint = OptionStyle.fit(context, hint, 0.8f, innerW - 4);
            context.drawText(fittedHint, box.x() + (box.width() - context.textWidth(fittedHint, 0.8f)) / 2,
                    innerY + innerH / 2 - 4, theme.color(ThemeKey.TEXT_SECONDARY), 0.8f);
            return;
        }

        double guiScale = (double) minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        float mouseX = (float) (minecraft.mouseHandler.xpos() * guiScale);
        float mouseY = (float) (minecraft.mouseHandler.ypos() * guiScale);
        int scale = Math.max(10, (int) (innerH * 0.4f));
        GuiGraphics graphics = graphicsContext.graphics();
        // Vanilla draws the model at z=50, sized for the inventory's scale of 30. At this larger
        // scale a tilted head reaches below z=0 and fails the depth test against the box fill.
        graphics.pose().pushPose();
        graphics.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);
        try {
            graphics.pose().translate(0, 0, MODEL_Z_LIFT);
            renderPlayerFollowingMouse(graphics, innerX, innerY, innerX + innerW, innerY + innerH, scale, mouseX, mouseY, player);
        } finally {
            graphics.disableScissor();
            graphics.pose().popPose();
        }
    }

    /**
     * {@link InventoryScreen#renderEntityInInventoryFollowsMouse}, but also overriding the previous-tick
     * rotations ({@code xRotO}, {@code yRotO}, {@code yBodyRotO}). Vanilla only sets the current ones,
     * which is enough for its own renderer (it's called with partial tick 1), but anything that
     * interpolates with the real frame's partial tick would otherwise see the player's actual in-world
     * look direction instead of the posed one. Drawn through {@link EmfCompat} so an EMF animation pack
     * can't turn the head without its hat layer.
     */
    private static void renderPlayerFollowingMouse(GuiGraphics graphics, int x1, int y1, int x2, int y2, int scale,
                                                   float mouseX, float mouseY, LivingEntity entity) {
        float centerX = (x1 + x2) / 2f;
        float centerY = (y1 + y2) / 2f;
        float yawInput = (float) Math.atan((centerX - mouseX) / 40f);
        float pitchInput = (float) Math.atan((centerY - mouseY) / 40f);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitchInput * 20f * Mth.DEG_TO_RAD);
        pose.mul(camera);

        float bodyRot = entity.yBodyRot;
        float bodyRotO = entity.yBodyRotO;
        float yRot = entity.getYRot();
        float yRotO = entity.yRotO;
        float xRot = entity.getXRot();
        float xRotO = entity.xRotO;
        float headRot = entity.yHeadRot;
        float headRotO = entity.yHeadRotO;
        try {
            float posedBody = 180f + yawInput * 20f;
            float posedYaw = 180f + yawInput * 40f;
            float posedPitch = -pitchInput * 20f;
            entity.yBodyRot = posedBody;
            entity.yBodyRotO = posedBody;
            entity.setYRot(posedYaw);
            entity.yRotO = posedYaw;
            entity.setXRot(posedPitch);
            entity.xRotO = posedPitch;
            entity.yHeadRot = posedYaw;
            entity.yHeadRotO = posedYaw;

            float entityScale = entity.getScale();
            Vector3f offset = new Vector3f(0f, entity.getBbHeight() / 2f + 0.0625f * entityScale, 0f);
            EmfCompat.withVanillaModel(entity, () -> InventoryScreen.renderEntityInInventory(
                    graphics, centerX, centerY, scale / entityScale, offset, pose, camera, entity));
        } finally {
            entity.yBodyRot = bodyRot;
            entity.yBodyRotO = bodyRotO;
            entity.setYRot(yRot);
            entity.yRotO = yRotO;
            entity.setXRot(xRot);
            entity.xRotO = xRotO;
            entity.yHeadRot = headRot;
            entity.yHeadRotO = headRotO;
        }
    }

    private void drawInfo(RenderContext context, Bounds area, LocalPlayer player) {
        List<Section> sections = List.of(climate(player), system(), integrations());
        int contentHeight = 0;
        for (Section section : sections) {
            contentHeight += HEADER_HEIGHT + section.rows().size() * ROW_HEIGHT + SECTION_GAP;
        }
        scroll = Math.max(0, Math.min(scroll, contentHeight - area.height()));

        Theme theme = context.theme();
        int accent = theme.color(ThemeKey.BORDER_HOVER);
        int labelColor = theme.color(ThemeKey.TEXT_SECONDARY);
        context.pushClip(area.x(), area.y(), area.width(), area.height());
        try {
            int y = area.y() - scroll;
            for (Section section : sections) {
                context.drawText(section.title(), area.x(), y + 3, accent, 1f);
                context.fillRect(area.x(), y + 13, area.width(), 1, theme.color(ThemeKey.BORDER));
                y += HEADER_HEIGHT;
                for (Row row : section.rows()) {
                    int valueWidth = context.textWidth(row.value(), 0.9f);
                    int labelMax = Math.max(0, area.width() - valueWidth - PAD);
                    context.drawText(OptionStyle.fit(context, row.label(), 0.9f, labelMax), area.x(), y + 2, labelColor, 0.9f);
                    int valueColor = row.color() != 0 ? row.color() : theme.color(ThemeKey.TEXT_PRIMARY);
                    context.drawText(row.value(), area.x() + area.width() - valueWidth, y + 2, valueColor, 0.9f);
                    y += ROW_HEIGHT;
                }
                y += SECTION_GAP;
            }
        } finally {
            context.popClip();
        }
    }

    private Section climate(LocalPlayer player) {
        List<Row> rows = new ArrayList<>();
        if (player == null) {
            rows.add(new Row(text("gui.thermalsystems.hub.home.status"), text("gui.thermalsystems.hub.home.not_in_world"), 0));
        } else if (status == null) {
            rows.add(new Row(text("gui.thermalsystems.hub.home.status"), text("gui.thermalsystems.hub.home.loading"), 0));
        } else {
            double ambient = model.get(entry("integration", "defaultAmbientTemperature"));
            rows.add(new Row(text("gui.thermalsystems.hub.home.outside"),
                    temperature(status.outsideTemperature()), temperatureColor(status.outsideTemperature(), ambient)));
            if (status.inZone()) {
                rows.add(new Row(text("gui.thermalsystems.hub.home.zone"), status.zoneName(), 0));
                rows.add(new Row(text("gui.thermalsystems.hub.home.zone_temperature"),
                        temperature(status.zoneTemperature()), temperatureColor(status.zoneTemperature(), ambient)));
                rows.add(new Row(text("gui.thermalsystems.hub.home.zone_target"), temperature(status.zoneTarget()), 0));
                rows.add(new Row(text("gui.thermalsystems.hub.home.zone_mode"), status.zoneMode(), 0));
            } else {
                rows.add(new Row(text("gui.thermalsystems.hub.home.zone"), text("gui.thermalsystems.hub.home.no_zone"), 0));
            }
            if (status.sourceInRange() && !Double.isNaN(status.appliedTemperature())) {
                rows.add(new Row(text("gui.thermalsystems.hub.home.near_sources"),
                        temperature(status.appliedTemperature()), temperatureColor(status.appliedTemperature(), ambient)));
            } else {
                rows.add(new Row(text("gui.thermalsystems.hub.home.near_sources"), text("gui.thermalsystems.hub.home.none_in_range"), 0));
            }
        }
        return new Section(text("gui.thermalsystems.hub.home.climate"), rows);
    }

    private Section system() {
        List<Row> rows = new ArrayList<>();
        boolean enabled = model.getBool(entry("simulation", "systemEnabled"));
        rows.add(new Row(text("gui.thermalsystems.hub.home.simulation"),
                text(enabled ? "gui.thermalsystems.hub.status.on" : "gui.thermalsystems.hub.status.off"), enabled ? GOOD : BAD));
        rows.add(new Row(text("config.thermalsystems.defaultAmbientTemperature"),
                temperature(model.get(entry("integration", "defaultAmbientTemperature"))), 0));
        rows.add(new Row(text("gui.thermalsystems.hub.home.temperature_range"),
                temperature(model.get(entry("simulation", "minimumTemperature"))) + " / "
                        + temperature(model.get(entry("simulation", "maximumTemperature"))), 0));
        if (integrations.stream().anyMatch(i -> i.section().equals("enderio") && i.installed())) {
            boolean cooling = model.getBool(entry("enderio", "coolingMode"));
            rows.add(new Row(text("gui.thermalsystems.hub.home.enderio_generators"),
                    text(cooling ? "gui.thermalsystems.hub.home.cooling" : "gui.thermalsystems.hub.home.heating"), cooling ? COLD : HOT));
        }
        rows.add(new Row(text("gui.thermalsystems.hub.tools.access"),
                text(model.canEdit() ? "gui.thermalsystems.hub.access.edit" : "gui.thermalsystems.hub.access.read_only"),
                model.canEdit() ? GOOD : 0));
        return new Section(text("gui.thermalsystems.hub.home.system"), rows);
    }

    private Section integrations() {
        List<Row> rows = new ArrayList<>();
        for (Integration integration : integrations) {
            String section = integration.section();
            boolean enabled = model.getBool(entry(section, "enabled"));
            String value;
            int color;
            if (!integration.installed()) {
                value = text("gui.thermalsystems.hub.status.not_installed");
                color = 0xFF777777;
            } else if (enabled) {
                value = text("gui.thermalsystems.hub.home.active");
                color = GOOD;
            } else {
                value = text("gui.thermalsystems.hub.status.off");
                color = BAD;
            }
            rows.add(new Row(text("config.thermalsystems.category." + section), value, color));
        }
        return new Section(text("gui.thermalsystems.hub.home.integrations"), rows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || !lastInfoBounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        scroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
        return true;
    }

    private static int temperatureColor(double celsius, double ambient) {
        if (celsius > ambient + COMFORT_BAND) {
            return HOT;
        }
        if (celsius < ambient - COMFORT_BAND) {
            return COLD;
        }
        return 0;
    }

    private static String temperature(double celsius) {
        return ClientTemperatureUnit.format(celsius);
    }

    private static Entry entry(String section, String key) {
        return ThermalConfigEntries.get(section, key);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private record Section(String title, List<Row> rows) {
    }

    /** {@code color} 0 means the theme's primary text color. */
    private record Row(String label, String value, int color) {
    }
}

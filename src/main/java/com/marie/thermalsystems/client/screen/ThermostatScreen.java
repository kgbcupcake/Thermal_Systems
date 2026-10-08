package com.marie.thermalsystems.client.screen;

import com.marie.thermalsystems.client.zone.ClientZones;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.toolbox.OptionStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The thermostat tablet app. The list page shows every zone the server lets this player see,
 * nearest first; picking one opens its thermostat page - live temperature, target, mode, bound
 * heaters/coolers, and (for the owner) the public-control lock and delete. Opened from a wall
 * thermostat, it's pinned to that room: no list, no back arrow.
 *
 * <p>Edits are sent immediately and shown optimistically until the server's next zone list
 * confirms them, so repeated +/- clicks don't snap back while the round trip is in flight.
 */
public final class ThermostatScreen extends TabletFrameScreen {

    private static final long PENDING_MS = 2000;
    private static final long MISSING_GRACE_MS = 2000;
    private static final long DELETE_CONFIRM_MS = 3000;
    private static final int ROW_HEIGHT = 24;

    @Nullable
    private final BlockPos wallPos;
    @Nullable
    private UUID selected;
    private long openedMs;
    private int scroll;

    private record Pending(UUID zoneId, double target, ClimateMode mode, boolean publicControl, long untilMs) {
    }

    @Nullable
    private Pending pending;
    private long deleteArmedUntil;

    public ThermostatScreen(@Nullable UUID zoneId, @Nullable BlockPos wallPos) {
        super(Component.translatable("gui.thermalsystems.thermostat.title"));
        this.selected = zoneId;
        this.wallPos = wallPos;
    }

    @Override
    protected void init() {
        super.init();
        openedMs = System.currentTimeMillis();
    }

    @Override
    protected Component header() {
        if (selected != null) {
            Optional<ZoneInfo> zone = ZoneInfo.find(ClientZones.latest().zones(), selected);
            if (zone.isPresent()) {
                return Component.literal(zone.get().name());
            }
        }
        return wallPos != null
                ? Component.translatable("gui.thermalsystems.thermostat.wall_title")
                : getTitle();
    }

    @Nullable
    @Override
    protected Runnable backAction() {
        if (wallPos != null || selected == null) {
            return null;
        }
        return () -> {
            selected = null;
            pending = null;
        };
    }

    @Override
    protected void renderContent(RenderContext context, Bounds area, int mouseX, int mouseY) {
        if (selected == null) {
            if (wallPos != null) {
                drawMessage(context, area, text("gui.thermalsystems.thermostat.wall_no_zone"),
                        text("gui.thermalsystems.thermostat.wall_no_zone_hint"));
            } else {
                drawList(context, area, mouseX, mouseY);
            }
            return;
        }
        Optional<ZoneInfo> zone = ZoneInfo.find(ClientZones.latest().zones(), selected);
        if (zone.isEmpty()) {
            if (System.currentTimeMillis() - openedMs < MISSING_GRACE_MS) {
                drawMessage(context, area, text("gui.thermalsystems.hub.home.loading"), "");
            } else {
                drawMessage(context, area, text("gui.thermalsystems.thermostat.zone_gone"), "");
            }
            return;
        }
        drawThermostat(context, area, withPending(zone.get()), mouseX, mouseY);
    }

    private void drawMessage(RenderContext context, Bounds area, String line, String hint) {
        int centerX = area.x() + area.width() / 2;
        int y = area.y() + area.height() / 2 - 10;
        centered(context, line, centerX, y, context.theme().color(ThemeKey.TEXT_PRIMARY), 1f);
        if (!hint.isEmpty()) {
            centered(context, OptionStyle.fit(context, hint, 0.8f, area.width() - 8), centerX, y + 14, MUTED, 0.8f);
        }
    }

    private void drawList(RenderContext context, Bounds area, int mouseX, int mouseY) {
        List<ZoneInfo> zones = ClientZones.latest().zones();
        if (zones.isEmpty()) {
            drawMessage(context, area, text("gui.thermalsystems.thermostat.no_zones"),
                    text("gui.thermalsystems.thermostat.no_zones_hint"));
            return;
        }
        int maxScroll = Math.max(0, zones.size() * ROW_HEIGHT - area.height());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        context.pushClip(area.x(), area.y(), area.width(), area.height());
        try {
            int y = area.y() - scroll;
            for (ZoneInfo zone : zones) {
                if (y + ROW_HEIGHT >= area.y() && y <= area.y() + area.height()) {
                    drawRow(context, zone, area.x(), y, area.width(), mouseX, mouseY, area);
                }
                y += ROW_HEIGHT;
            }
        } finally {
            context.popClip();
        }
    }

    private void drawRow(RenderContext context, ZoneInfo zone, int x, int y, int w, int mouseX, int mouseY, Bounds clip) {
        Bounds row = new Bounds(x, y, w, ROW_HEIGHT - 2);
        boolean hovered = row.contains(mouseX, mouseY) && clip.contains(mouseX, mouseY);
        context.fillRect(x, y, w, ROW_HEIGHT - 2, hovered ? 0xFF1F242D : 0xFF161A20);
        context.fillRect(x, y, 3, ROW_HEIGHT - 2, demandColor(zone));
        String name = OptionStyle.fit(context, zone.name(), 1f, w - 90);
        context.drawText(name, x + 7, y + 3, context.theme().color(ThemeKey.TEXT_PRIMARY), 1f);
        String detail = text("gui.thermalsystems.thermostat.row_detail", temperature(zone.targetTemp()), modeText(zone.mode()));
        context.drawText(OptionStyle.fit(context, detail, 0.75f, w - 90), x + 7, y + 13, MUTED, 0.75f);
        String temp = temperature(zone.currentTemp());
        context.drawText(temp, x + w - 6 - context.textWidth(temp, 1.25f), y + 3, temperatureColor(zone), 1.25f);
        String demand = demandText(zone);
        context.drawText(demand, x + w - 6 - context.textWidth(demand, 0.7f), y + 14, demandColor(zone), 0.7f);
        if (hovered) {
            clickable(row, () -> {
                selected = zone.id();
                openedMs = System.currentTimeMillis();
            });
        }
    }

    private void drawThermostat(RenderContext context, Bounds area, ZoneInfo zone, int mouseX, int mouseY) {
        int centerX = area.x() + area.width() / 2;
        int y = area.y();
        String owner = zone.ownerName().isEmpty()
                ? text("gui.thermalsystems.thermostat.owner_ops")
                : text("gui.thermalsystems.thermostat.owner", zone.ownerName());
        context.drawText(OptionStyle.fit(context, owner, 0.75f, area.width()), area.x(), y, MUTED, 0.75f);
        String access = text(zone.canEdit() ? "gui.thermalsystems.thermostat.access_owner"
                : zone.canControl() ? "gui.thermalsystems.thermostat.access_public" : "gui.thermalsystems.thermostat.access_locked");
        context.drawText(access, area.x() + area.width() - context.textWidth(access, 0.75f), y, zone.canControl() ? GOOD : BAD, 0.75f);

        y += 14;
        centered(context, temperature(zone.currentTemp()), centerX, y, temperatureColor(zone), 2.5f);
        y += 26;
        centered(context, statusText(zone), centerX, y, demandColor(zone), 0.85f);

        y += 16;
        boolean control = zone.canControl();
        int stepW = 24;
        button(context, area.x() + 30, y, stepW, 16, Component.literal("-"), control, false, 0, mouseX, mouseY,
                () -> send(zone, stepTarget(zone.targetTemp(), -1), zone.mode(), zone.publicControl()));
        button(context, area.x() + area.width() - 30 - stepW, y, stepW, 16, Component.literal("+"), control, false, 0, mouseX, mouseY,
                () -> send(zone, stepTarget(zone.targetTemp(), 1), zone.mode(), zone.publicControl()));
        centered(context, text("gui.thermalsystems.thermostat.target", temperature(zone.targetTemp())), centerX, y + 4,
                context.theme().color(ThemeKey.TEXT_PRIMARY), 1f);

        y += 22;
        ClimateMode[] modes = ClimateMode.values();
        int gap = 4;
        int modeW = (area.width() - gap * (modes.length - 1)) / modes.length;
        for (int i = 0; i < modes.length; i++) {
            ClimateMode mode = modes[i];
            button(context, area.x() + i * (modeW + gap), y, modeW, 16, Component.literal(modeText(mode)), control,
                    zone.mode() == mode, modeColor(mode), mouseX, mouseY,
                    () -> send(zone, zone.targetTemp(), mode, zone.publicControl()));
        }

        y += 22;
        String heaters = text("gui.thermalsystems.thermostat.heaters", zone.heatSources(),
                String.format(Locale.ROOT, "%.1f", zone.heatOutput()));
        String coolers = text("gui.thermalsystems.thermostat.coolers", zone.coolingSources(),
                String.format(Locale.ROOT, "%.1f", zone.coolingOutput()));
        context.drawText(heaters, area.x(), y, HOT, 0.75f);
        context.drawText(coolers, area.x() + area.width() - context.textWidth(coolers, 0.75f), y, COLD, 0.75f);

        y += 14;
        if (zone.canEdit()) {
            int half = (area.width() - gap) / 2;
            String lock = text(zone.publicControl() ? "gui.thermalsystems.thermostat.public_on" : "gui.thermalsystems.thermostat.public_off");
            button(context, area.x(), y, half, 16, Component.literal(lock), true, zone.publicControl(), GOOD, mouseX, mouseY,
                    () -> send(zone, zone.targetTemp(), zone.mode(), !zone.publicControl()));
            boolean armed = System.currentTimeMillis() < deleteArmedUntil;
            String delete = text(armed ? "gui.thermalsystems.thermostat.delete_confirm" : "gui.thermalsystems.thermostat.delete");
            button(context, area.x() + half + gap, y, half, 16, Component.literal(delete), true, armed, BAD, mouseX, mouseY,
                    () -> {
                        if (System.currentTimeMillis() < deleteArmedUntil) {
                            PacketDistributor.sendToServer(new ZonePayloads.Delete(zone.id()));
                            deleteArmedUntil = 0;
                            if (wallPos == null) {
                                selected = null;
                            }
                        } else {
                            deleteArmedUntil = System.currentTimeMillis() + DELETE_CONFIRM_MS;
                        }
                    });
        } else if (!control) {
            centered(context, OptionStyle.fit(context, text("gui.thermalsystems.thermostat.locked_hint"), 0.75f, area.width()),
                    centerX, y + 4, MUTED, 0.75f);
        }
    }

    private void send(ZoneInfo zone, double target, ClimateMode mode, boolean publicControl) {
        pending = new Pending(zone.id(), target, mode, publicControl, System.currentTimeMillis() + PENDING_MS);
        PacketDistributor.sendToServer(new ZonePayloads.Settings(zone.id(), target, mode, publicControl));
    }

    private ZoneInfo withPending(ZoneInfo zone) {
        Pending edit = pending;
        if (edit == null || !edit.zoneId().equals(zone.id()) || System.currentTimeMillis() > edit.untilMs()) {
            return zone;
        }
        return new ZoneInfo(zone.id(), zone.name(), zone.min(), zone.max(), zone.currentTemp(), edit.target(),
                edit.mode(), zone.demand(), zone.ownerName(), zone.canEdit(), zone.canControl(), edit.publicControl(),
                zone.heatSources(), zone.coolingSources(), zone.heatOutput(), zone.coolingOutput());
    }

    private static String statusText(ZoneInfo zone) {
        if (zone.mode() == ClimateMode.OFF) {
            return text("gui.thermalsystems.thermostat.status.off");
        }
        return switch (zone.demand()) {
            case HEATING -> text("gui.thermalsystems.thermostat.status.heating", temperature(zone.targetTemp()));
            case COOLING -> text("gui.thermalsystems.thermostat.status.cooling", temperature(zone.targetTemp()));
            case IDLE -> text("gui.thermalsystems.thermostat.status.idle");
        };
    }

    private static String demandText(ZoneInfo zone) {
        return text("gui.thermalsystems.thermostat.demand." + zone.demand().name().toLowerCase(Locale.ROOT));
    }

    static String modeText(ClimateMode mode) {
        return text("gui.thermalsystems.thermostat.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    static int modeColor(ClimateMode mode) {
        return switch (mode) {
            case OFF -> MUTED;
            case HEAT -> HOT;
            case COOL -> COLD;
            case AUTO -> GOOD;
        };
    }

    private static int demandColor(ZoneInfo zone) {
        if (zone.demand() == ClimateDemand.HEATING) {
            return HOT;
        }
        if (zone.demand() == ClimateDemand.COOLING) {
            return COLD;
        }
        return MUTED;
    }

    private static int temperatureColor(ZoneInfo zone) {
        double delta = zone.currentTemp() - zone.targetTemp();
        if (zone.mode() == ClimateMode.OFF || Math.abs(delta) < 0.5) {
            return 0xFFE8E8E8;
        }
        return delta < 0 ? COLD : HOT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selected == null && scrollY != 0) {
            scroll -= (int) Math.signum(scrollY) * ROW_HEIGHT;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

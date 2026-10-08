package com.marie.thermalsystems.client.screen;

import com.marie.thermalsystems.client.zone.ClientZones;
import com.marie.thermalsystems.client.zone.GadgetSelection;
import com.marie.thermalsystems.client.zone.ZoneSelectionRenderer;
import com.marie.thermalsystems.item.CopiedSettings;
import com.marie.thermalsystems.item.GadgetMode;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import com.marie.thermalsystems.registry.ThermalRegistries;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.toolbox.OptionStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Zone Gadget's menu (sneak + use): switch modes, see the current selection, reopen the
 * {@link ZoneEditorScreen} on it, stop editing or delete a zone, or clear what's been copied.
 * Everything is read live from the held stack, which only the server changes, so the menu always
 * reflects what the server will act on.
 */
public final class ZoneGadgetScreen extends TabletFrameScreen {

    private static final long DELETE_CONFIRM_MS = 3000;

    private long deleteArmedUntil;

    public ZoneGadgetScreen() {
        super(Component.translatable("gui.thermalsystems.gadget.title"));
    }

    @Nullable
    private ItemStack gadget() {
        return minecraft.player == null ? null : ZoneGadgetItem.heldGadget(minecraft.player);
    }

    @Override
    public void tick() {
        super.tick();
        if (gadget() == null) {
            onClose();
        }
    }

    @Override
    protected void renderContent(RenderContext context, Bounds area, int mouseX, int mouseY) {
        ItemStack stack = gadget();
        if (stack == null) {
            return;
        }
        GadgetMode mode = ZoneGadgetItem.mode(stack);

        GadgetMode[] modes = GadgetMode.values();
        int gap = 4;
        int tabW = (area.width() - gap * (modes.length - 1)) / modes.length;
        for (int i = 0; i < modes.length; i++) {
            GadgetMode tab = modes[i];
            button(context, area.x() + i * (tabW + gap), area.y(), tabW, 16, Component.translatable(tab.translationKey()),
                    true, tab == mode, COLD, mouseX, mouseY,
                    () -> PacketDistributor.sendToServer(new ZonePayloads.GadgetUpdate(tab, false, false, false)));
        }
        int y = area.y() + 22;
        context.drawText(OptionStyle.fit(context, text(mode.translationKey() + ".hint"), 0.75f, area.width()),
                area.x(), y, MUTED, 0.75f);
        y += 12;

        GadgetSelection selection = GadgetSelection.of(stack, ClientZones.latest());
        switch (mode) {
            case CREATE -> drawCreate(context, area, y, stack, selection, mouseX, mouseY);
            case EDIT -> drawEdit(context, area, y, stack, selection, mouseX, mouseY);
            case INSPECT -> drawLine(context, area, y, text("gui.thermalsystems.gadget.inspect_help"), MUTED);
            case COPY -> drawCopy(context, area, y, stack, mouseX, mouseY);
        }
    }

    private void drawCreate(RenderContext context, Bounds area, int y, ItemStack stack, @Nullable GadgetSelection selection,
                            int mouseX, int mouseY) {
        drawSelection(context, area, y, stack, selection);
        int bottom = area.y() + area.height() - 16;
        int half = (area.width() - 4) / 2;
        openEditorButton(context, area.x(), bottom, half, stack, selection, null, mouseX, mouseY);
        clearSelectionButton(context, area.x() + half + 4, bottom, half, stack, selection != null, mouseX, mouseY);
    }

    private void drawEdit(RenderContext context, Bounds area, int y, ItemStack stack, @Nullable GadgetSelection selection,
                          int mouseX, int mouseY) {
        UUID editing = stack.get(ThermalRegistries.EDIT_TARGET.get());
        if (editing == null) {
            drawLine(context, area, y, text("gui.thermalsystems.gadget.edit_help"), MUTED);
            return;
        }
        String name = ZoneInfo.find(ClientZones.latest().zones(), editing).map(ZoneInfo::name)
                .orElse(text("gui.thermalsystems.hub.home.loading"));
        drawLine(context, area, y, text("gui.thermalsystems.gadget.editing", name), context.theme().color(ThemeKey.TEXT_PRIMARY));
        drawSelection(context, area, y + 12, stack, selection);

        int bottom = area.y() + area.height() - 16;
        int third = (area.width() - 8) / 3;
        openEditorButton(context, area.x(), bottom - 20, area.width(), stack, selection, editing, mouseX, mouseY);
        clearSelectionButton(context, area.x(), bottom, third, stack, selection != null, mouseX, mouseY);
        button(context, area.x() + third + 4, bottom, third, 16, Component.translatable("gui.thermalsystems.gadget.stop_editing"),
                true, false, 0, mouseX, mouseY,
                () -> PacketDistributor.sendToServer(new ZonePayloads.GadgetUpdate(GadgetMode.EDIT, false, false, true)));
        boolean armed = System.currentTimeMillis() < deleteArmedUntil;
        button(context, area.x() + 2 * (third + 4), bottom, third, 16,
                Component.translatable(armed ? "gui.thermalsystems.thermostat.delete_confirm" : "gui.thermalsystems.thermostat.delete"),
                true, armed, BAD, mouseX, mouseY, () -> {
                    if (System.currentTimeMillis() < deleteArmedUntil) {
                        PacketDistributor.sendToServer(new ZonePayloads.Delete(editing));
                        PacketDistributor.sendToServer(new ZonePayloads.GadgetUpdate(GadgetMode.EDIT, true, false, true));
                        deleteArmedUntil = 0;
                    } else {
                        deleteArmedUntil = System.currentTimeMillis() + DELETE_CONFIRM_MS;
                    }
                });
    }

    private void drawCopy(RenderContext context, Bounds area, int y, ItemStack stack, int mouseX, int mouseY) {
        CopiedSettings copied = stack.get(ThermalRegistries.COPIED_SETTINGS.get());
        if (copied == null) {
            drawLine(context, area, y, text("gui.thermalsystems.gadget.copy_empty"), MUTED);
            return;
        }
        drawLine(context, area, y, text("gui.thermalsystems.gadget.copy_from", copied.sourceName()),
                context.theme().color(ThemeKey.TEXT_PRIMARY));
        drawLine(context, area, y + 12, text("gui.thermalsystems.gadget.copy_values", temperature(copied.target()),
                ThermostatScreen.modeText(copied.mode())), ThermostatScreen.modeColor(copied.mode()));
        drawLine(context, area, y + 24, text("gui.thermalsystems.gadget.copy_help"), MUTED);
        int bottom = area.y() + area.height() - 16;
        button(context, area.x(), bottom, area.width(), 16, Component.translatable("gui.thermalsystems.gadget.clear_copy"),
                true, false, 0, mouseX, mouseY,
                () -> PacketDistributor.sendToServer(new ZonePayloads.GadgetUpdate(GadgetMode.COPY, false, true, false)));
    }

    /** Corner summary plus size/problem line; returns the y below it. */
    private int drawSelection(RenderContext context, Bounds area, int y, ItemStack stack, @Nullable GadgetSelection selection) {
        BlockPos first = stack.get(ThermalRegistries.CORNER_ONE.get());
        BlockPos second = stack.get(ThermalRegistries.CORNER_TWO.get());
        drawLine(context, area, y, text("gui.thermalsystems.gadget.corner_one", corner(first)), context.theme().color(ThemeKey.TEXT_PRIMARY));
        drawLine(context, area, y + 10, text("gui.thermalsystems.gadget.corner_two", corner(second)), context.theme().color(ThemeKey.TEXT_PRIMARY));
        if (selection != null && !selection.preview()) {
            String size = text("gui.thermalsystems.gadget.size", selection.sizeX(), selection.sizeY(), selection.sizeZ(), selection.volume());
            boolean ok = selection.problem() == null;
            String line = ok ? size : size + "  " + ZoneSelectionRenderer.problemText(selection).getString();
            drawLine(context, area, y + 20, line, ok ? GOOD : BAD);
        }
        return y + 30;
    }

    private void openEditorButton(RenderContext context, int x, int y, int w, ItemStack stack,
                                  @Nullable GadgetSelection selection, @Nullable UUID editing, int mouseX, int mouseY) {
        boolean ready = selection != null && !selection.preview();
        button(context, x, y, w, 16, Component.translatable("gui.thermalsystems.gadget.open_editor"), ready, ready, GOOD,
                mouseX, mouseY, () -> {
                    String name = editing == null ? ""
                            : ZoneInfo.find(ClientZones.latest().zones(), editing).map(ZoneInfo::name).orElse("");
                    minecraft.setScreen(new ZoneEditorScreen(selection.first(), selection.second(), editing, name));
                });
    }

    private void clearSelectionButton(RenderContext context, int x, int y, int w, ItemStack stack, boolean enabled,
                                      int mouseX, int mouseY) {
        GadgetMode mode = ZoneGadgetItem.mode(stack);
        button(context, x, y, w, 16, Component.translatable("gui.thermalsystems.gadget.clear_selection"), enabled, false, 0,
                mouseX, mouseY, () -> PacketDistributor.sendToServer(new ZonePayloads.GadgetUpdate(mode, true, false, false)));
    }

    private static String corner(@Nullable BlockPos pos) {
        return pos == null ? "-" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void drawLine(RenderContext context, Bounds area, int y, String line, int color) {
        context.drawText(OptionStyle.fit(context, line, 0.8f, area.width()), area.x(), y, color, 0.8f);
    }
}

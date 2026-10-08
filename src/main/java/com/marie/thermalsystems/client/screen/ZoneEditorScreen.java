package com.marie.thermalsystems.client.screen;

import com.marie.thermalsystems.client.zone.ClientZones;
import com.marie.thermalsystems.client.zone.GadgetSelection;
import com.marie.thermalsystems.client.zone.ZonePreview;
import com.marie.thermalsystems.client.zone.ZoneSelectionRenderer;
import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.item.GadgetMode;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.toolbox.OptionStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * The Zone Gadget's zone editor, opened once both corners are picked (or a zone is picked up in Edit
 * mode): a rotatable 3D model of the area on the left, and on the right the zone's name, its target
 * (new zones only) and From/To steppers that grow or shrink the box along X, Y and Z. Save creates
 * the zone, or renames and resizes the one being edited. The box lives only in this screen until
 * Save; the server re-checks it.
 */
public final class ZoneEditorScreen extends TabletFrameScreen {

    private static final int PREFERRED_WIDTH = 384;
    private static final int PREFERRED_HEIGHT = 236;
    private static final int PANEL_WIDTH = 176;
    private static final int STEPPER_WIDTH = 64;
    private static final int[] AXIS_COLORS = {BAD, GOOD, COLD};
    private static final String[] AXIS_NAMES = {"X", "Y", "Z"};

    @Nullable
    private final UUID editing;
    private final String originalName;
    private final int[] originalMin;
    private final int[] originalMax;
    private final int[] min;
    private final int[] max;
    private final ZonePreview preview = new ZonePreview();

    private EditBox nameBox;
    /** NaN until the server's zone list arrives with its default target. */
    private double target = Double.NaN;
    private boolean dragging;

    public ZoneEditorScreen(BlockPos first, BlockPos second, @Nullable UUID editing, String name) {
        super(Component.translatable(editing == null ? "gui.thermalsystems.editor.title_new" : "gui.thermalsystems.editor.title_edit"));
        this.editing = editing;
        this.originalName = name;
        this.originalMin = new int[]{Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())};
        this.originalMax = new int[]{Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())};
        this.min = originalMin.clone();
        this.max = originalMax.clone();
    }

    @Override
    protected int frameWidth() {
        return Math.min(PREFERRED_WIDTH, width - 16);
    }

    @Override
    protected int frameHeight() {
        return Math.min(PREFERRED_HEIGHT, height - 16);
    }

    @Override
    protected void init() {
        super.init();
        String previous = nameBox != null ? nameBox.getValue() : originalName;
        int panelX = panelX();
        nameBox = new EditBox(font, panelX + 36, contentBounds().y(), PANEL_WIDTH - 36, 14,
                Component.translatable("gui.thermalsystems.gadget.name"));
        nameBox.setMaxLength(ClimateController.MAX_NAME_LENGTH);
        nameBox.setValue(previous);
        nameBox.setHint(Component.translatable("gui.thermalsystems.gadget.name_hint"));
        addRenderableWidget(nameBox);
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.player == null || ZoneGadgetItem.heldGadget(minecraft.player) == null) {
            onClose();
        }
    }

    private int panelX() {
        Bounds area = contentBounds();
        return area.x() + area.width() - PANEL_WIDTH;
    }

    private Bounds viewport() {
        Bounds area = contentBounds();
        return new Bounds(area.x(), area.y(), area.width() - PANEL_WIDTH - 8, area.height() - 12);
    }

    private BlockPos minPos() {
        return new BlockPos(min[0], min[1], min[2]);
    }

    private BlockPos maxPos() {
        return new BlockPos(max[0], max[1], max[2]);
    }

    private GadgetSelection selection() {
        return GadgetSelection.forBox(editing == null ? GadgetMode.CREATE : GadgetMode.EDIT, minPos(), maxPos(), false,
                editing, ClientZones.latest());
    }

    private boolean nameTaken(String name) {
        for (ZoneInfo zone : ClientZones.latest().zones()) {
            if (zone.name().equals(name) && !zone.id().equals(editing)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderContent(RenderContext context, Bounds area, int mouseX, int mouseY) {
        int primary = context.theme().color(ThemeKey.TEXT_PRIMARY);
        GadgetSelection selection = selection();

        Bounds view = viewport();
        context.drawRoundedRect(view.x(), view.y(), view.width(), view.height(), 1, 3, 0xFF07080B,
                context.theme().color(ThemeKey.BORDER));
        if (selection.volume() > ZonePreview.MAX_PREVIEW_VOLUME) {
            centered(context, text("gui.thermalsystems.editor.too_large"), view.x() + view.width() / 2,
                    view.y() + view.height() - 10, MUTED, 0.7f);
        }
        context.drawText(OptionStyle.fit(context, text("gui.thermalsystems.editor.hint"), 0.7f, view.width()),
                view.x(), view.y() + view.height() + 4, MUTED, 0.7f);

        int x = panelX();
        int y = area.y();
        context.drawText(text("gui.thermalsystems.gadget.name"), x, y + 4, primary, 0.85f);
        y += 20;

        if (editing == null) {
            if (Double.isNaN(target)) {
                target = ClientZones.latest().defaultTarget();
            }
            boolean haveTarget = !Double.isNaN(target);
            context.drawText(text("gui.thermalsystems.gadget.target"), x, y + 4, primary, 0.85f);
            button(context, x + 36, y, 16, 14, Component.literal("-"), haveTarget, false, 0, mouseX, mouseY,
                    () -> target = ClimateController.clampTarget(stepTarget(target, -1)));
            centered(context, haveTarget ? temperature(target) : "...", x + 36 + 16 + 30, y + 3, primary, 1f);
            button(context, x + 36 + 16 + 60, y, 16, 14, Component.literal("+"), haveTarget, false, 0, mouseX, mouseY,
                    () -> target = ClimateController.clampTarget(stepTarget(target, 1)));
        } else {
            context.drawText(OptionStyle.fit(context, text("gui.thermalsystems.editor.editing_hint"), 0.75f, PANEL_WIDTH),
                    x, y + 4, MUTED, 0.75f);
        }
        y += 22;

        int fromX = x + 14;
        int toX = fromX + STEPPER_WIDTH + 6;
        int sizeX = toX + STEPPER_WIDTH + (x + PANEL_WIDTH - toX - STEPPER_WIDTH) / 2;
        centered(context, text("gui.thermalsystems.editor.from"), fromX + STEPPER_WIDTH / 2, y, MUTED, 0.7f);
        centered(context, text("gui.thermalsystems.editor.to"), toX + STEPPER_WIDTH / 2, y, MUTED, 0.7f);
        centered(context, text("gui.thermalsystems.editor.size"), sizeX, y, MUTED, 0.7f);
        y += 10;
        for (int axis = 0; axis < 3; axis++) {
            int rowY = y + axis * 18;
            context.drawText(AXIS_NAMES[axis], x + 2, rowY + 4, AXIS_COLORS[axis], 1f);
            stepper(context, fromX, rowY, axis, true, mouseX, mouseY);
            stepper(context, toX, rowY, axis, false, mouseX, mouseY);
            centered(context, Integer.toString(max[axis] - min[axis] + 1), sizeX, rowY + 3, primary, 1f);
        }
        y += 3 * 18 + 4;

        context.drawText(OptionStyle.fit(context, text("gui.thermalsystems.gadget.size", selection.sizeX(), selection.sizeY(),
                selection.sizeZ(), selection.volume()), 0.8f, PANEL_WIDTH), x, y, primary, 0.8f);
        String name = nameBox.getValue().strip();
        String status;
        int statusColor;
        if (selection.problem() != null) {
            status = ZoneSelectionRenderer.problemText(selection).getString();
            statusColor = BAD;
        } else if (name.isEmpty()) {
            status = text("message.thermalsystems.zone.name_empty");
            statusColor = MUTED;
        } else if (nameTaken(name)) {
            status = text("message.thermalsystems.zone.name_taken", name);
            statusColor = BAD;
        } else {
            status = text("gui.thermalsystems.editor.ready");
            statusColor = GOOD;
        }
        context.drawText(OptionStyle.fit(context, status, 0.8f, PANEL_WIDTH), x, y + 11, statusColor, 0.8f);

        boolean ready = statusColor == GOOD && (editing != null || !Double.isNaN(target));
        int bottom = area.y() + area.height() - 16;
        int third = (PANEL_WIDTH - 8) / 3;
        button(context, x, bottom, third, 16, Component.translatable("gui.thermalsystems.editor.save"), ready, ready, GOOD,
                mouseX, mouseY, () -> save(name));
        button(context, x + third + 4, bottom, third, 16, Component.translatable("gui.thermalsystems.editor.reset"), true, false, 0,
                mouseX, mouseY, this::reset);
        button(context, x + 2 * (third + 4), bottom, third, 16, Component.translatable("gui.thermalsystems.editor.cancel"), true,
                false, 0, mouseX, mouseY, this::onClose);
    }

    /** One From/To value with -/+ either side; Shift steps by 5. */
    private void stepper(RenderContext context, int x, int y, int axis, boolean from, int mouseX, int mouseY) {
        int value = from ? min[axis] : max[axis];
        button(context, x, y, 14, 14, Component.literal("-"), true, false, 0, mouseX, mouseY, () -> step(axis, from, -1));
        centered(context, Integer.toString(value), x + STEPPER_WIDTH / 2, y + 3,
                context.theme().color(ThemeKey.TEXT_PRIMARY), 0.9f);
        button(context, x + STEPPER_WIDTH - 14, y, 14, 14, Component.literal("+"), true, false, 0, mouseX, mouseY,
                () -> step(axis, from, 1));
    }

    private void step(int axis, boolean from, int direction) {
        int delta = direction * (hasShiftDown() ? 5 : 1);
        if (from) {
            min[axis] = Math.min(min[axis] + delta, max[axis]);
        } else {
            max[axis] = Math.max(max[axis] + delta, min[axis]);
        }
    }

    private void reset() {
        System.arraycopy(originalMin, 0, min, 0, 3);
        System.arraycopy(originalMax, 0, max, 0, 3);
        nameBox.setValue(originalName);
        preview.resetCamera();
    }

    private void save(String name) {
        PacketDistributor.sendToServer(new ZonePayloads.GadgetSave(Optional.ofNullable(editing), name,
                Double.isNaN(target) ? 0.0 : target, minPos(), maxPos()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Bounds view = viewport();
        int outline = selection().problem() == null ? GOOD : BAD;
        preview.render(graphics, minPos(), maxPos(), view.x() + 1, view.y() + 1, view.width() - 2, view.height() - 2,
                outline, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (viewport().contains((int) mouseX, (int) mouseY)) {
            setFocused(null);
            if (button == 0) {
                dragging = true;
                return true;
            }
            if (button == 1) {
                preview.resetCamera();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            preview.rotate(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (viewport().contains((int) mouseX, (int) mouseY)) {
            preview.zoom(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

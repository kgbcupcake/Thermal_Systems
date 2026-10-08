package com.marie.thermalsystems.client.zone;

import com.marie.thermalsystems.ThermalSystemsMod;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

/**
 * While a Zone Gadget is held: draws every nearby zone as a wireframe (cyan for zones you can edit,
 * grey for others), the gadget's own selection on top (green when acceptable, red when it's too
 * big or overlaps another zone), and a one-line readout above the hotbar with the selection's size.
 */
public final class ZoneSelectionRenderer {

    private static final long POLL_MS = 2000;
    private static final double INFLATE = 0.005;

    private ZoneSelectionRenderer() {
    }

    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ZoneSelectionRenderer::onRenderLevel);
        modEventBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ThermalSystemsMod.MOD_ID, "zone_gadget"),
                ZoneSelectionRenderer::renderHud));
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack gadget = ZoneGadgetItem.heldGadget(minecraft.player);
        if (gadget == null) {
            return;
        }
        ClientZones.poll(POLL_MS);
        ZonePayloads.ZoneList zones = ClientZones.latest();
        GadgetSelection selection = GadgetSelection.of(gadget, zones);
        UUID editing = selection != null ? selection.editing() : null;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        try {
            for (ZoneInfo zone : zones.zones()) {
                if (!zone.hasBounds() || zone.id().equals(editing)) {
                    continue;
                }
                AABB box = box(zone.min(), zone.max());
                if (zone.canEdit()) {
                    LevelRenderer.renderLineBox(pose, lines, box, 0.3F, 0.8F, 1.0F, 1.0F);
                } else {
                    LevelRenderer.renderLineBox(pose, lines, box, 0.6F, 0.6F, 0.6F, 1.0F);
                }
            }
            if (selection != null) {
                boolean ok = selection.problem() == null;
                LevelRenderer.renderLineBox(pose, lines, box(selection.first(), selection.second()),
                        ok ? 0.3F : 1.0F, ok ? 1.0F : 0.3F, ok ? 0.4F : 0.3F, 1.0F);
                LevelRenderer.renderLineBox(pose, lines, box(selection.first(), selection.first()),
                        1.0F, 0.9F, 0.2F, 1.0F);
            }
        } finally {
            buffers.endBatch(RenderType.lines());
            pose.popPose();
        }
    }

    private static AABB box(BlockPos a, BlockPos b) {
        return new AABB(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1)
                .inflate(INFLATE);
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        ItemStack gadget = ZoneGadgetItem.heldGadget(minecraft.player);
        if (gadget == null) {
            return;
        }
        GadgetSelection selection = GadgetSelection.of(gadget, ClientZones.latest());
        Component line;
        int color;
        if (selection == null) {
            line = Component.translatable(ZoneGadgetItem.mode(gadget).translationKey() + ".hint");
            color = 0xFFBBBBBB;
        } else {
            Component size = Component.translatable("gui.thermalsystems.gadget.size",
                    selection.sizeX(), selection.sizeY(), selection.sizeZ(), selection.volume());
            if (selection.problem() != null) {
                line = Component.translatable("gui.thermalsystems.gadget.hud_problem", size, problemText(selection));
                color = 0xFFFF6B6B;
            } else {
                line = Component.translatable(selection.preview()
                        ? "gui.thermalsystems.gadget.hud_preview" : "gui.thermalsystems.gadget.hud_ready", size);
                color = 0xFF7BE38A;
            }
        }
        int width = minecraft.font.width(line);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 84;
        graphics.fill(x - 3, y - 2, x + width + 3, y + 10, 0x90000000);
        graphics.drawString(minecraft.font, line, x, y, color, false);
    }

    public static Component problemText(GadgetSelection selection) {
        if ("overlap".equals(selection.problem())) {
            return Component.translatable("message.thermalsystems.zone.overlap", selection.overlapName());
        }
        if ("too_far".equals(selection.problem())) {
            return Component.translatable("message.thermalsystems.zone.too_far");
        }
        return Component.translatable("message.thermalsystems.zone.too_big", selection.volume(), ClientZones.latest().maxVolume());
    }
}

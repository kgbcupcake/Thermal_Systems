package com.marie.thermalsystems.client.zone;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.opengl.GL11;

/**
 * Draws a box of the world as a small rotatable 3D model inside a screen: the real blocks (and
 * block entities) inside it, cut away from their surroundings, with the box outlined on top. Holds
 * only the camera - drag with {@link #rotate}, scroll with {@link #zoom} - and re-reads the world
 * every frame, so the model always matches the box being edited.
 */
public final class ZonePreview {

    /** Above this many blocks only the outline is drawn; tesselating every block each frame gets too slow. */
    public static final long MAX_PREVIEW_VOLUME = 16_384;
    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 4f;
    /** GUI depth the model is centred on: in front of the screen's panels, well inside the GUI's depth range. */
    private static final float DEPTH = 400f;

    private final RandomSource random = RandomSource.create();
    private float yaw = 225f;
    private float pitch = 30f;
    private float zoom = 1f;

    public void rotate(double dx, double dy) {
        yaw = (float) ((yaw + dx * 0.8) % 360.0);
        pitch = Mth.clamp((float) (pitch + dy * 0.8), -89f, 89f);
    }

    public void zoom(double scroll) {
        zoom = Mth.clamp(zoom * (float) Math.pow(1.15, scroll), MIN_ZOOM, MAX_ZOOM);
    }

    public void resetCamera() {
        yaw = 225f;
        pitch = 30f;
        zoom = 1f;
    }

    /**
     * Draws the box spanning {@code a}/{@code b} into the given screen rectangle, outlined in
     * {@code outline} (ARGB).
     */
    public void render(GuiGraphics graphics, BlockPos a, BlockPos b, int x, int y, int width, int height,
                       int outline, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        float diagonal = Mth.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ);
        float scale = Math.min(width, height) * 0.9f / diagonal * zoom;

        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x + width / 2f, y + height / 2f, DEPTH);
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.translate(-sizeX / 2f, -sizeY / 2f, -sizeZ / 2f);
            Lighting.setupFor3DItems();

            MultiBufferSource.BufferSource buffers = graphics.bufferSource();
            if ((long) sizeX * sizeY * sizeZ <= MAX_PREVIEW_VOLUME) {
                drawBlocks(minecraft, level, new ZonePreviewView(level, min, max), min, max, pose, buffers, partialTick);
            }
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), 0, 0, 0, sizeX, sizeY, sizeZ,
                    ((outline >> 16) & 0xFF) / 255f, ((outline >> 8) & 0xFF) / 255f, (outline & 0xFF) / 255f, 1f);
            graphics.flush();
        } finally {
            pose.popPose();
            graphics.disableScissor();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }

    private void drawBlocks(Minecraft minecraft, ClientLevel level, ZonePreviewView view, BlockPos min, BlockPos max,
                            PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick) {
        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = view.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX() - min.getX(), pos.getY() - min.getY(), pos.getZ() - min.getZ());
            if (state.getRenderShape() == RenderShape.MODEL) {
                BakedModel model = blocks.getBlockModel(state);
                ModelData data = model.getModelData(view, pos, state, view.getModelData(pos));
                random.setSeed(state.getSeed(pos));
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    blocks.renderBatched(state, pos, view, pose, buffers.getBuffer(RenderTypeHelper.getEntityRenderType(type, false)),
                            true, random, data, type);
                }
            }
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            if (blockEntity != null) {
                BlockEntityRenderer<BlockEntity> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                if (renderer != null) {
                    renderer.render(blockEntity, partialTick, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            }
            pose.popPose();
        }
    }
}

package com.marie.thermalsystems.client.render;

import com.marie.thermalsystems.block.WallThermostatBlock;
import com.marie.thermalsystems.block.WallThermostatBlockEntity;
import com.marie.thermalsystems.block.WallThermostatTouch;
import com.marie.thermalsystems.client.ClientTemperatureUnit;
import com.marie.thermalsystems.controller.ClimateDemand;
import com.marie.thermalsystems.controller.ClimateMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Draws the wall thermostat's live display onto its front face: zone name, current temperature,
 * target, mode and what the zone is calling for, plus labels over the three touch buttons (placed
 * from {@link WallThermostatTouch} so they line up with what a click hits). Text is full-bright so
 * the screen reads as lit in a dark room.
 *
 * <p>Layout is in face pixels: after the transform one face pixel is 8 units, the origin is the
 * face's centre, +x is the viewer's right and +y is down.
 */
public final class WallThermostatRenderer implements BlockEntityRenderer<WallThermostatBlockEntity> {

    private static final float UNITS_PER_BLOCK = 128f;
    private static final float PIXEL = UNITS_PER_BLOCK / 16f;
    /** Distance from the block centre back to the front face (2 px out from the wall), less a hair so text isn't z-fighting it. */
    private static final float FACE_DEPTH = 0.5f - 2f / 16f - 0.005f;

    private static final int WHITE = 0xFFE8E8E8;
    private static final int MUTED = 0xFF8A8F99;
    private static final int HOT = 0xFFFF8A4C;
    private static final int COLD = 0xFF5AB4FF;
    private static final int GOOD = 0xFF5BD47A;

    private final Font font;

    public WallThermostatRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(WallThermostatBlockEntity thermostat, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Direction facing = thermostat.getBlockState().getValue(WallThermostatBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0, 0, -FACE_DEPTH);
        float scale = 1f / UNITS_PER_BLOCK;
        pose.scale(scale, -scale, scale);

        if (thermostat.hasZone()) {
            drawZone(thermostat, pose, buffers);
        } else {
            draw(pose, buffers, Component.translatable("gui.thermalsystems.wall.no_zone").getString(), 0, py(4.5f), MUTED, 0.7f);
            draw(pose, buffers, Component.translatable("gui.thermalsystems.wall.no_zone_hint").getString(), 0, py(6f), MUTED, 0.5f);
        }
        pose.popPose();
    }

    private void drawZone(WallThermostatBlockEntity thermostat, PoseStack pose, MultiBufferSource buffers) {
        draw(pose, buffers, fit(thermostat.zoneName(), 0.6f), 0, py(2.3f), WHITE, 0.6f);
        draw(pose, buffers, ClientTemperatureUnit.format(thermostat.currentTemp()), 0, py(3.6f), temperatureColor(thermostat), 1.8f);
        String target = Component.translatable("gui.thermalsystems.wall.target",
                ClientTemperatureUnit.format(thermostat.targetTemp())).getString();
        draw(pose, buffers, target, 0, py(6.4f), WHITE, 0.6f);
        String status = Component.translatable("gui.thermalsystems.thermostat.mode." + lower(thermostat.mode())).getString()
                + " \u00B7 "
                + Component.translatable("gui.thermalsystems.thermostat.demand." + lower(thermostat.demand())).getString();
        draw(pose, buffers, fit(status, 0.5f), 0, py(7.8f), demandColor(thermostat), 0.5f);

        float buttonY = py(16f - (float) (WallThermostatTouch.BUTTON_STRIP_TOP * 16f) + 1f);
        float minusX = faceX((float) (2 + WallThermostatTouch.MINUS_RIGHT * 16f) / 2f);
        float plusX = faceX((float) (WallThermostatTouch.PLUS_LEFT * 16f + 14) / 2f);
        draw(pose, buffers, "-", minusX, buttonY, WHITE, 1f);
        draw(pose, buffers, Component.translatable("gui.thermalsystems.wall.mode").getString(), 0, buttonY + 2.25f, modeColor(thermostat.mode()), 0.5f);
        draw(pose, buffers, "+", plusX, buttonY, WHITE, 1f);
    }

    /** Face pixel row (from the top) to layout y. */
    private static float py(float row) {
        return (row - 8f) * PIXEL;
    }

    /** Face pixel column (from the viewer's left) to layout x. */
    private static float faceX(float column) {
        return (column - 8f) * PIXEL;
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, String text, float centerX, float y, int color, float scale) {
        pose.pushPose();
        pose.translate(centerX, y, 0);
        pose.scale(scale, scale, 1f);
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** Trims {@code text} to the screen's 12-pixel width at {@code scale}. */
    private String fit(String text, float scale) {
        int max = (int) (11f * PIXEL / scale);
        if (font.width(text) <= max) {
            return text;
        }
        return font.plainSubstrByWidth(text, max - font.width("...")) + "...";
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static int temperatureColor(WallThermostatBlockEntity thermostat) {
        double delta = thermostat.currentTemp() - thermostat.targetTemp();
        if (thermostat.mode() == ClimateMode.OFF || Math.abs(delta) < 0.5) {
            return WHITE;
        }
        return delta < 0 ? COLD : HOT;
    }

    private static int demandColor(WallThermostatBlockEntity thermostat) {
        if (thermostat.demand() == ClimateDemand.HEATING) {
            return HOT;
        }
        if (thermostat.demand() == ClimateDemand.COOLING) {
            return COLD;
        }
        return MUTED;
    }

    private static int modeColor(ClimateMode mode) {
        return switch (mode) {
            case OFF -> MUTED;
            case HEAT -> HOT;
            case COOL -> COLD;
            case AUTO -> GOOD;
        };
    }
}

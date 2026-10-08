package com.marie.thermalsystems.client.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The client level cut down to one box, for drawing a zone as a diorama: everything outside the box
 * reads as air (so the box's outer faces render, like a cut-away), and light is always full so a
 * dark room still reads clearly. Face shading and tints still come from the real level.
 */
public final class ZonePreviewView implements BlockAndTintGetter {

    private final BlockAndTintGetter level;
    private final BlockPos min;
    private final BlockPos max;

    public ZonePreviewView(BlockAndTintGetter level, BlockPos min, BlockPos max) {
        this.level = level;
        this.min = min;
        this.max = max;
    }

    private boolean inside(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return inside(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return inside(pos) ? level.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return inside(pos) ? level.getBlockEntity(pos) : null;
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        return inside(pos) ? level.getModelData(pos) : ModelData.EMPTY;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(pos, resolver);
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}

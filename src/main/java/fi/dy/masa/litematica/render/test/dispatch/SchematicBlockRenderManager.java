package fi.dy.masa.litematica.render.test.dispatch;

import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import fi.dy.masa.litematica.render.test.data.SchematicRendererRegion;

public class SchematicBlockRenderManager
{
    private final LocalRandom random = new LocalRandom(0);
    private final BlockColors colorMap;
    private final BlockRenderManager vanillaRenderer;

    public SchematicBlockRenderManager(BlockColors blockColorsIn, BlockRenderManager vanillaRenderer)
    {
        this.colorMap = blockColorsIn;
        this.vanillaRenderer = vanillaRenderer;
    }

    public void renderFluid(BlockPos blockPos, SchematicRendererRegion renderRegion, BufferBuilder bufferBuilder, BlockState blockState, FluidState fluidState)
    {
        this.vanillaRenderer.renderFluid(blockPos, renderRegion, bufferBuilder, blockState, fluidState);
    }

    public void renderFluid(BlockRenderView world, BlockPos blockPos, BufferBuilder bufferBuilder, BlockState blockState, FluidState fluidState)
    {
        this.vanillaRenderer.renderFluid(blockPos, world, bufferBuilder, blockState, fluidState);
    }

    public void renderBlock(BlockState blockState, BlockPos blockPos, SchematicRendererRegion renderRegion, MatrixStack matrixStack, BufferBuilder bufferBuilder, boolean cull, Random random)
    {
        this.vanillaRenderer.renderBlock(blockState, blockPos, renderRegion, matrixStack, bufferBuilder, cull, random);
    }

    public void renderBlock(BlockRenderView world, BlockState blockState, BlockPos blockPos, MatrixStack matrixStack, BufferBuilder bufferBuilder, boolean cull, Random random)
    {
        this.vanillaRenderer.renderBlock(blockState, blockPos, world, matrixStack, bufferBuilder, cull, random);
    }
}

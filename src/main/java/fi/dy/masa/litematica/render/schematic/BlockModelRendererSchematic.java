package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.BaseRandom;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.schematic.ao.AOProcessor;
import fi.dy.masa.litematica.render.schematic.ao.AOProcessorModern;

public class BlockModelRendererSchematic
{
    private final LocalRandom random = new LocalRandom(0);
    private final BlockColors colorMap;
    private final FluidRenderer liquidRenderer;
    private BakedModelManager bakedManager;
    public static final ThreadLocal<AOProcessorModern.BC> CACHE = ThreadLocal.withInitial(AOProcessorModern.BC::new);

    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
        this.colorMap = blockColorsIn;
        this.liquidRenderer = new FluidRenderer();
    }

    public void setBakedManager(BakedModelManager manager)
    {
        this.bakedManager = manager;
    }

    public static void enableCache()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            CACHE.get().enable();
        }
    }

    public static void disableCache()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            CACHE.get().disable();
        }
    }

    public boolean renderModel(BlockRenderView worldIn, List<BlockModelPart> modelParts, BlockState stateIn,
                               BlockPos posIn, MatrixStack matrixStack,
                               VertexConsumer vertexConsumer, long rand)
    {
        boolean ao = MinecraftClient.isAmbientOcclusionEnabled() && stateIn.getLuminance() == 0 && modelParts.getFirst().useAmbientOcclusion();

        Vec3d offset = stateIn.getModelOffset(posIn);
        matrixStack.translate((float) offset.x, (float) offset.y, (float) offset.z);
        int overlay = OverlayTexture.DEFAULT_UV;

        try
        {
            if (ao)
            {
                //System.out.printf("renderModelSmooth(): pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                return this.renderModelSmooth(worldIn, modelParts, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
            else
            {
                //System.out.printf("renderModelFlat(): pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                return this.renderModelFlat(worldIn, modelParts, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
        }
        catch (Throwable throwable)
        {
            //Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block model");
            CrashReportSection crashreportcategory = crashreport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, worldIn, posIn, stateIn);
            crashreportcategory.add("Using AO", ao);
            throw new CrashException(crashreport);
            //return false;
        }
    }

    public boolean renderModelSmooth(BlockRenderView worldIn, List<BlockModelPart> modelParts, BlockState stateIn, BlockPos posIn, MatrixStack matrixStack,
                                      VertexConsumer vertexConsumer, BaseRandom random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        float[] quadBounds = new float[PositionUtils.ALL_DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        AOProcessor aoFace = AOProcessor.get();
        BlockPos.Mutable mutablePos = posIn.mutableCopy();

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                random.setSeed(seedIn);
                // modelIn.getQuads(stateIn, side, random)
                List<BakedQuad> quads = part.getQuads(side);

                if (!quads.isEmpty())
                {
                    mutablePos.set(posIn, side);
                    if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                    {
                        //System.out.printf("renderQuadsSmooth():1: pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                        this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                        renderedSomething = true;
                    }
                }
            }

            random.setSeed(seedIn);
            // modelIn.getQuads(stateIn, null, random);
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                //System.out.printf("renderQuadsSmooth():2: pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

    public boolean renderModelFlat(BlockRenderView worldIn, List<BlockModelPart> modelParts, BlockState stateIn,
                                    BlockPos posIn, MatrixStack matrixStack,
                                    VertexConsumer vertexConsumer, BaseRandom random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        BitSet bitset = new BitSet(3);
        BlockPos.Mutable mutablePos = posIn.mutableCopy();

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                random.setSeed(seedIn);
                // modelIn.getQuads(stateIn, side, random)
                List<BakedQuad> quads = part.getQuads(side);

                if (!quads.isEmpty())
                {
                    mutablePos.set(posIn, side);
                    if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                    {
                        //int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
                        int light = WorldRenderer.getLightmapCoordinates(worldIn, mutablePos);
                        this.renderQuadsFlat(worldIn, stateIn, posIn, light, overlay, false, matrixStack, vertexConsumer, quads, bitset);
                        renderedSomething = true;
                    }
                }
            }

            random.setSeed(seedIn);
            // modelIn.getQuads(stateIn, null, random)
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                this.renderQuadsFlat(worldIn, stateIn, posIn, -1, overlay, true, matrixStack, vertexConsumer, quads, bitset);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

    public boolean shouldRenderModelSide(BlockRenderView worldIn, BlockState stateIn, BlockPos posIn, Direction side, BlockPos mutable)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
                (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
                //Block.shouldDrawSide(stateIn, worldIn, posIn, side, posIn.offset(side));
                Block.shouldDrawSide(stateIn, worldIn.getBlockState(mutable), side);
        // TODO --> check
    }

    private void renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrixStack,
                                   VertexConsumer vertexConsumer, List<BakedQuad> list, float[] box, BitSet flags, AOProcessor aoCalc, int overlay)
    {
        final int size = list.size();

        //System.out.printf("renderQuad(): pos [%s] / state [%s] / quad size [%d]\n", pos.toShortString(), state, size);

        for (BakedQuad bakedQuad : list)
        {
            this.getQuadDimensions(world, state, pos, bakedQuad.vertexData(), bakedQuad.face(), box, flags);
            aoCalc.apply(world, state, pos, bakedQuad.face(), box, flags, bakedQuad.shade());

            //System.out.printf("renderQuad(): pos [%s] / state [%s] / quad face [%s]\n", pos.toShortString(), state, bakedQuad.getFace().getName());

            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad, aoCalc.brightness, aoCalc.light, overlay);
        }
    }

    private void renderQuadsFlat(BlockRenderView world, BlockState state, BlockPos pos,
                                 int light, int overlay, boolean useWorldLight, MatrixStack matrixStack, VertexConsumer vertexConsumer, List<BakedQuad> list, BitSet flags)
    {
        //final int size = list.size();

        for (BakedQuad bakedQuad : list)
        {
            if (useWorldLight)
            {
                this.getQuadDimensions(world, state, pos, bakedQuad.vertexData(), bakedQuad.face(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.offset(bakedQuad.face()) : pos;
                light = WorldRenderer.getLightmapCoordinates(world, blockPos);
            }

            float b = world.getBrightness(bakedQuad.face(), bakedQuad.shade());
            int[] lo = new int[]{light, light, light, light};
            float[] bo = new float[]{b, b, b, b};
            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad, bo, lo, overlay);
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack matrixStack,
                            BakedQuad quad, float[] brightness, int[] light, int overlay)
    {
        float r;
        float g;
        float b;

        if (quad.hasTint())
        {
            int color = this.colorMap.getColor(state, world, pos, quad.tintIndex());
            r = (float) (color >> 16 & 0xFF) / 255.0F;
            g = (float) (color >> 8 & 0xFF) / 255.0F;
            b = (float) (color & 0xFF) / 255.0F;
        }
        else
        {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        //System.out.printf("quad(): pos [%s] / state [%s] --> SPRITE [%s]\n", pos.toShortString(), state, quad.getSprite().toString());
        vertexConsumer.quad(matrixStack.peek(), quad, brightness, r, g, b, 1.0f, light, overlay, true);
    }

    private void getQuadDimensions(BlockRenderView world, BlockState state, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] box, BitSet flags)
    {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;
        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;
        final int vertexSize = vertexData.length / 4;

        for (int index = 0; index < 4; ++index)
        {
            float x = Float.intBitsToFloat(vertexData[index * vertexSize]);
            float y = Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
            float z = Float.intBitsToFloat(vertexData[index * vertexSize + 2]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (box != null)
        {
            box[Direction.WEST.getIndex()] = minX;
            box[Direction.EAST.getIndex()] = maxX;
            box[Direction.DOWN.getIndex()] = minY;
            box[Direction.UP.getIndex()] = maxY;
            box[Direction.NORTH.getIndex()] = minZ;
            box[Direction.SOUTH.getIndex()] = maxZ;

            box[Direction.WEST.getIndex() + 6] = 1.0F - minX;
            box[Direction.EAST.getIndex() + 6] = 1.0F - maxX;
            box[Direction.DOWN.getIndex() + 6] = 1.0F - minY;
            box[Direction.UP.getIndex() + 6] = 1.0F - maxY;
            box[Direction.NORTH.getIndex() + 6] = 1.0F - minZ;
            box[Direction.SOUTH.getIndex() + 6] = 1.0F - maxZ;
        }

        float min = 1.0E-4F;
        float max = 0.9999F;

        switch (face)
        {
            case DOWN:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (minY < min || state.isFullCube(world, pos)));
                break;
            case UP:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (maxY > max || state.isFullCube(world, pos)));
                break;
            case NORTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (minZ < min || state.isFullCube(world, pos)));
                break;
            case SOUTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (maxZ > max || state.isFullCube(world, pos)));
                break;
            case WEST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (minX < min || state.isFullCube(world, pos)));
                break;
            case EAST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (maxX > max || state.isFullCube(world, pos)));
        }
    }

    /*
    private void fillQuadBounds(BlockRenderView world, BlockState stateIn, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] quadBounds, BitSet boundsFlags)
    {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; ++i)
        {
            float f6 = Float.intBitsToFloat(vertexData[i * 7]);
            float f7 = Float.intBitsToFloat(vertexData[i * 7 + 1]);
            float f8 = Float.intBitsToFloat(vertexData[i * 7 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (quadBounds != null)
        {
            quadBounds[Direction.WEST.getIndex()] = f;
            quadBounds[Direction.EAST.getIndex()] = f3;
            quadBounds[Direction.DOWN.getIndex()] = f1;
            quadBounds[Direction.UP.getIndex()] = f4;
            quadBounds[Direction.NORTH.getIndex()] = f2;
            quadBounds[Direction.SOUTH.getIndex()] = f5;
            int j = Direction.values().length;
            quadBounds[Direction.WEST.getIndex() + j] = 1.0F - f;
            quadBounds[Direction.EAST.getIndex() + j] = 1.0F - f3;
            quadBounds[Direction.DOWN.getIndex() + j] = 1.0F - f1;
            quadBounds[Direction.UP.getIndex() + j] = 1.0F - f4;
            quadBounds[Direction.NORTH.getIndex() + j] = 1.0F - f2;
            quadBounds[Direction.SOUTH.getIndex() + j] = 1.0F - f5;
        }

        switch (face)
        {
            case DOWN:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f1 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case UP:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f4 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case NORTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f2 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case SOUTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f5 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case WEST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
                break;
            case EAST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f3 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
        }
    }
    */

    @ApiStatus.Experimental
    public void renderBlockEntity(VertexConsumer vertexConsumer, MatrixStack matrixStack, BlockStateModel modelIn,
                             float red, float green, float blue, int light, int overlay)
    {
        List<BlockModelPart> parts = modelIn.getParts(Random.create(42L));

        for (BlockModelPart part : parts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                this.renderBlockEntityQuads(vertexConsumer, matrixStack, red, green, blue, part.getQuads(side), light, overlay);
            }

            // modelIn.getQuads(stateIn, null, rand)
            this.renderBlockEntityQuads(vertexConsumer, matrixStack, red, green, blue, part.getQuads(null), light, overlay);
        }
    }

    @ApiStatus.Experimental
    private void renderBlockEntityQuads(VertexConsumer vertexConsumer, MatrixStack matrixStack,
                             float red, float green, float blue, List<BakedQuad> quads, int light, int overlay)
    {
        for (BakedQuad quad : quads)
        {
            float h;
            float g;
            float f;

            if (quad.hasTint())
            {
                f = MathHelper.clamp(red, 0.0f, 1.0f);
                g = MathHelper.clamp(green, 0.0f, 1.0f);
                h = MathHelper.clamp(blue, 0.0f, 1.0f);
            }
            else
            {
                h = 1.0F;
                g = 1.0F;
                f = 1.0F;
            }
            vertexConsumer.quad(matrixStack.peek(), quad, f, g, h, 1.0f, light, overlay);
        }
    }

    @ApiStatus.Experimental
    public void renderLiquid(VertexConsumer consumer, BlockRenderView world, BlockPos pos, BlockState stateIn, FluidState fluid)
    {
        try
        {
            this.liquidRenderer.render(world, pos, consumer, stateIn, fluid);
        }
        catch (Throwable var9)
        {
            CrashReport crashReport = CrashReport.create(var9, "Tesselating liquid in world");
            CrashReportSection crashReportSection = crashReport.addElement("Block being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, world, pos, null);
            throw new CrashException(crashReport);
        }
    }

    public BlockStateModel getBakedModel(BlockState stateIn)
    {
        return this.bakedManager.getBlockModels().getModel(stateIn);
    }

    @ApiStatus.Experimental
    public boolean renderBlockEntity(VertexConsumerProvider consumer, MatrixStack matrixStack, BlockState stateIn, int light, int overlay)
    {
        BlockRenderType blockRenderType = stateIn.getRenderType();

        if (blockRenderType == BlockRenderType.INVISIBLE)
        {
            return false;
        }

        BlockStateModel bakedModel = this.getBakedModel(stateIn);
        int i = this.colorMap.getColor(stateIn, null, null, 0);
        float red = (float) (i >> 16 & 0xFF) / 255.0f;
        float green = (float) (i >> 8 & 0xFF) / 255.0f;
        float blue = (float) (i & 0xFF) / 255.0f;

        this.renderBlockEntity(consumer.getBuffer(RenderLayers.getEntityBlockLayer(stateIn)), matrixStack, bakedModel, red, green, blue, light, overlay);
        this.bakedManager.getBlockEntityModelsSupplier().get()
                    .render(stateIn.getBlock(), ItemDisplayContext.NONE, matrixStack, consumer, light, overlay);

        return true;
    }
}

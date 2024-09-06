package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.BaseRandom;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;

public class BlockModelRendererSchematic
{
    private static final Direction[] DIRECTIONS = Direction.values();
    private final LocalRandom random = new LocalRandom(0);
    private final BlockColors colorMap;
    static final ThreadLocal<BC> CACHE = ThreadLocal.withInitial(BC::new);


    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
        this.colorMap = blockColorsIn;
    }

    public boolean renderModel(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn,
                               BlockPos posIn, MatrixStack matrixStack,
                               VertexConsumer vertexConsumer, long rand)
    {
        boolean ao = MinecraftClient.isAmbientOcclusionEnabled() && stateIn.getLuminance() == 0 && modelIn.useAmbientOcclusion();

        Vec3d offset = stateIn.getModelOffset(posIn);
        matrixStack.translate((float) offset.x, (float) offset.y, (float) offset.z);
        int overlay = OverlayTexture.DEFAULT_UV;

        //try
        //{
            if (ao)
            {
                return this.renderModelSmooth(worldIn, modelIn, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
            else
            {
                return this.renderModelFlat(worldIn, modelIn, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
        //}
        //catch (Throwable throwable)
        //{
            //Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
            /*
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block model");
            CrashReportSection crashreportcategory = crashreport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, worldIn, posIn, stateIn);
            crashreportcategory.add("Using AO", ao);
            throw new CrashException(crashreport);
            */
            //return false;
        //}
    }

    private boolean renderModelSmooth(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrixStack,
                                      VertexConsumer vertexConsumer, BaseRandom random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        float[] quadBounds = new float[PositionUtils.ALL_DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        AmbientOcclusionCalculator aoFace = new AmbientOcclusionCalculator();
        BlockPos.Mutable mutablePos = posIn.mutableCopy();

        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (!quads.isEmpty())
            {
                mutablePos.set(posIn, side);
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                {
                    this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, null, random);

        if (!quads.isEmpty())
        {
            this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    private boolean renderModelFlat(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn,
                                    BlockPos posIn, MatrixStack matrixStack,
                                    VertexConsumer vertexConsumer, BaseRandom random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        BitSet bitset = new BitSet(3);
        BlockPos.Mutable mutablePos = posIn.mutableCopy();

        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (!quads.isEmpty())
            {
                mutablePos.set(posIn, side);
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                {
                    //int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
                    int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, mutablePos);
                    this.renderQuadsFlat(worldIn, stateIn, posIn, light, overlay, false, matrixStack, vertexConsumer, quads, bitset);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, null, random);

        if (!quads.isEmpty())
        {
            this.renderQuadsFlat(worldIn, stateIn, posIn, -1, overlay, true, matrixStack, vertexConsumer, quads, bitset);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    private boolean shouldRenderModelSide(BlockRenderView worldIn, BlockState stateIn, BlockPos posIn, Direction side, BlockPos mutable)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
                (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
                //Block.shouldDrawSide(stateIn, worldIn, posIn, side, posIn.offset(side));
                Block.shouldDrawSide(stateIn, worldIn.getBlockState(mutable), side);
        // TODO --> check
    }

    private void renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrixStack,
                                   VertexConsumer vertexConsumer, List<BakedQuad> list, float[] box, BitSet flags, AmbientOcclusionCalculator ambientOcclusionCalculator, int overlay)
    {
        //final int size = list.size();

        for (BakedQuad bakedQuad : list)
        {
            this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), box, flags);
            ambientOcclusionCalculator.apply(world, state, pos, bakedQuad.getFace(), box, flags, bakedQuad.hasShade());

            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad,
                    ambientOcclusionCalculator.brightness[0],
                    ambientOcclusionCalculator.brightness[1],
                    ambientOcclusionCalculator.brightness[2],
                    ambientOcclusionCalculator.brightness[3],
                    ambientOcclusionCalculator.light[0],
                    ambientOcclusionCalculator.light[1],
                    ambientOcclusionCalculator.light[2],
                    ambientOcclusionCalculator.light[3], overlay);
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
                this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.offset(bakedQuad.getFace()) : pos;
                light = WorldRenderer.getLightmapCoordinates(world, state, blockPos);
            }

            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad, 1.0F, 1.0F, 1.0F, 1.0F, light, light, light, light, overlay);
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack matrixStack,
                            BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3,
                            int light0, int light1, int light2, int light3, int overlay)
    {
        float r;
        float g;
        float b;

        if (quad.hasColor())
        {
            int color = this.colorMap.getColor(state, world, pos, quad.getColorIndex());
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
        vertexConsumer.quad(matrixStack.peek(), quad, new float[]{brightness0, brightness1, brightness2, brightness3},
                r, g, b, 1.0f, new int[]{light0, light1, light2, light3}, overlay, true);
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
            box[Direction.WEST.getId()] = minX;
            box[Direction.EAST.getId()] = maxX;
            box[Direction.DOWN.getId()] = minY;
            box[Direction.UP.getId()] = maxY;
            box[Direction.NORTH.getId()] = minZ;
            box[Direction.SOUTH.getId()] = maxZ;

            box[Direction.WEST.getId() + 6] = 1.0F - minX;
            box[Direction.EAST.getId() + 6] = 1.0F - maxX;
            box[Direction.DOWN.getId() + 6] = 1.0F - minY;
            box[Direction.UP.getId() + 6] = 1.0F - maxY;
            box[Direction.NORTH.getId() + 6] = 1.0F - minZ;
            box[Direction.SOUTH.getId() + 6] = 1.0F - maxZ;
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
            quadBounds[Direction.WEST.getId()] = f;
            quadBounds[Direction.EAST.getId()] = f3;
            quadBounds[Direction.DOWN.getId()] = f1;
            quadBounds[Direction.UP.getId()] = f4;
            quadBounds[Direction.NORTH.getId()] = f2;
            quadBounds[Direction.SOUTH.getId()] = f5;
            int j = Direction.values().length;
            quadBounds[Direction.WEST.getId() + j] = 1.0F - f;
            quadBounds[Direction.EAST.getId() + j] = 1.0F - f3;
            quadBounds[Direction.DOWN.getId() + j] = 1.0F - f1;
            quadBounds[Direction.UP.getId() + j] = 1.0F - f4;
            quadBounds[Direction.NORTH.getId() + j] = 1.0F - f2;
            quadBounds[Direction.SOUTH.getId() + j] = 1.0F - f5;
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

    public void renderEntity(VertexConsumer vertexConsumer, MatrixStack matrixStack, @Nullable BlockState stateIn, BakedModel modelIn,
                             float red, float green, float blue, int light, int overlay)
    {
        Random rand = Random.create();
        long life = 42L;
        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            rand.setSeed(life);
            this.renderQuads(vertexConsumer, matrixStack, red, green, blue, modelIn.getQuads(stateIn, side, rand), light, overlay);
        }
        rand.setSeed(life);
        this.renderQuads(vertexConsumer, matrixStack, red, green, blue, modelIn.getQuads(stateIn, null, rand), light, overlay);
    }

    private void renderQuads(VertexConsumer vertexConsumer, MatrixStack matrixStack,
                             float red, float green, float blue, List<BakedQuad> quads, int light, int overlay)
    {
        for (BakedQuad quad : quads)
        {
            float h;
            float g;
            float f;

            if (quad.hasColor())
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

    /*
    public boolean renderBlockEntity(VertexConsumerProvider consumer, MatrixStack matrixStack, BlockState stateIn, int light, int overlay)
    {
        BlockRenderType blockRenderType = stateIn.getRenderType();
        if (blockRenderType == BlockRenderType.INVISIBLE)
        {
            return false;
        }
        switch (blockRenderType)
        {
            case MODEL:
            {
                BakedModel bakedModel = this.getModel(state);
                int i = this.blockColors.getColor(state, null, null, 0);
                float f = (float) (i >> 16 & 0xFF) / 255.0f;
                float g = (float) (i >> 8 & 0xFF) / 255.0f;
                float h = (float) (i & 0xFF) / 255.0f;
            }
            case ENTITYBLOCK_ANIMATED: {
                this.builtinModelItemRenderer.render(new ItemStack(stateIn.getBlock()), ModelTransformationMode.NONE, matrixStack, consumer, light, overlay);
            }
        }

        return false;
    }
     */

    public static void enableCache()
    {
        CACHE.get().enable();
    }

    public static void disableCache()
    {
        CACHE.get().disable();
    }

    static class AmbientOcclusionCalculator
    {
        private final float[] brightness = new float[4];
        private final int[] light = new int[4];

        public void apply(BlockRenderView world, BlockState state, BlockPos pos, Direction direction, float[] box, BitSet shapeState, boolean hasShade)
        {
            // 2024
            /*
            EnumNeighborInfo neighborInfo = EnumNeighborInfo.getNeighbourInfo(direction);
            VertexTranslations vertexTranslations = VertexTranslations.getVertexTranslations(direction);
            int i, j, k, l, i1, i3, j1, k1, l1;
            i = j = k = l = i1 = i3 = j1 = k1 = l1 = ((15 << 20) | (15 << 4));
            float b1 = 1.0F;
            float b2 = 1.0F;
            float b3 = 1.0F;
            float b4 = 1.0F;

            if (shapeState.get(1) && neighborInfo.doNonCubicWeight)
            {
                float f13 = box[neighborInfo.vert0Weights[0].shape] * box[neighborInfo.vert0Weights[1].shape];
                float f14 = box[neighborInfo.vert0Weights[2].shape] * box[neighborInfo.vert0Weights[3].shape];
                float f15 = box[neighborInfo.vert0Weights[4].shape] * box[neighborInfo.vert0Weights[5].shape];
                float f16 = box[neighborInfo.vert0Weights[6].shape] * box[neighborInfo.vert0Weights[7].shape];
                float f17 = box[neighborInfo.vert1Weights[0].shape] * box[neighborInfo.vert1Weights[1].shape];
                float f18 = box[neighborInfo.vert1Weights[2].shape] * box[neighborInfo.vert1Weights[3].shape];
                float f19 = box[neighborInfo.vert1Weights[4].shape] * box[neighborInfo.vert1Weights[5].shape];
                float f20 = box[neighborInfo.vert1Weights[6].shape] * box[neighborInfo.vert1Weights[7].shape];
                float f21 = box[neighborInfo.vert2Weights[0].shape] * box[neighborInfo.vert2Weights[1].shape];
                float f22 = box[neighborInfo.vert2Weights[2].shape] * box[neighborInfo.vert2Weights[3].shape];
                float f23 = box[neighborInfo.vert2Weights[4].shape] * box[neighborInfo.vert2Weights[5].shape];
                float f24 = box[neighborInfo.vert2Weights[6].shape] * box[neighborInfo.vert2Weights[7].shape];
                float f25 = box[neighborInfo.vert3Weights[0].shape] * box[neighborInfo.vert3Weights[1].shape];
                float f26 = box[neighborInfo.vert3Weights[2].shape] * box[neighborInfo.vert3Weights[3].shape];
                float f27 = box[neighborInfo.vert3Weights[4].shape] * box[neighborInfo.vert3Weights[5].shape];
                float f28 = box[neighborInfo.vert3Weights[6].shape] * box[neighborInfo.vert3Weights[7].shape];
                this.brightness[vertexTranslations.vert0] = b1 * f13 + b2 * f14 + b3 * f15 + b4 * f16;
                this.brightness[vertexTranslations.vert1] = b1 * f17 + b2 * f18 + b3 * f19 + b4 * f20;
                this.brightness[vertexTranslations.vert2] = b1 * f21 + b2 * f22 + b3 * f23 + b4 * f24;
                this.brightness[vertexTranslations.vert3] = b1 * f25 + b2 * f26 + b3 * f27 + b4 * f28;
                int i2 = this.getAoBrightness(l, i, j1, i3);
                int j2 = this.getAoBrightness(k, i, i1, i3);
                int k2 = this.getAoBrightness(k, j, k1, i3);
                int l2 = this.getAoBrightness(l, j, l1, i3);
                this.light[vertexTranslations.vert0] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
                this.light[vertexTranslations.vert1] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
                this.light[vertexTranslations.vert2] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
                this.light[vertexTranslations.vert3] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
            }
            else
            {
                this.light[vertexTranslations.vert0] = this.getAoBrightness(l, i, j1, i3);
                this.light[vertexTranslations.vert1] = this.getAoBrightness(k, i, i1, i3);
                this.light[vertexTranslations.vert2] = this.getAoBrightness(k, j, k1, i3);
                this.light[vertexTranslations.vert3] = this.getAoBrightness(l, j, l1, i3);
                this.brightness[vertexTranslations.vert0] = b1;
                this.brightness[vertexTranslations.vert1] = b2;
                this.brightness[vertexTranslations.vert2] = b3;
                this.brightness[vertexTranslations.vert3] = b4;
            }

            float b = world.getBrightness(direction, hasShade);

            for (int index = 0; index < this.brightness.length; ++index)
            {
                this.brightness[index] *= b;
            }
             */

            // 24w36a
            BlockPos blockPos = shapeState.get(0) ? pos.offset(direction) : pos;
            ND neighborData = ND.getData(direction);
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            BC brightnessCache = BlockModelRendererSchematic.CACHE.get();
            mutable.set(blockPos, neighborData.faces[0]);
            BlockState blockState = world.getBlockState(mutable);
            int i = brightnessCache.getInt(blockState, world, mutable);
            float f = brightnessCache.getFloat(blockState, world, mutable);
            mutable.set(blockPos, neighborData.faces[1]);
            BlockState blockState2 = world.getBlockState(mutable);
            int j = brightnessCache.getInt(blockState2, world, mutable);
            float g = brightnessCache.getFloat(blockState2, world, mutable);
            mutable.set(blockPos, neighborData.faces[2]);
            BlockState blockState3 = world.getBlockState(mutable);
            int k = brightnessCache.getInt(blockState3, world, mutable);
            float h = brightnessCache.getFloat(blockState3, world, mutable);
            mutable.set(blockPos, neighborData.faces[3]);
            BlockState blockState4 = world.getBlockState(mutable);
            int l = brightnessCache.getInt(blockState4, world, mutable);
            float m = brightnessCache.getFloat(blockState4, world, mutable);
            BlockState blockState5 = world.getBlockState(mutable.set(blockPos, neighborData.faces[0]).move(direction));
            boolean bl2 = !blockState5.shouldBlockVision(world, mutable) || blockState5.getOpacity() == 0;
            BlockState blockState6 = world.getBlockState(mutable.set(blockPos, neighborData.faces[1]).move(direction));
            boolean bl3 = !blockState6.shouldBlockVision(world, mutable) || blockState6.getOpacity() == 0;
            BlockState blockState7 = world.getBlockState(mutable.set(blockPos, neighborData.faces[2]).move(direction));
            boolean bl4 = !blockState7.shouldBlockVision(world, mutable) || blockState7.getOpacity() == 0;
            BlockState blockState8 = world.getBlockState(mutable.set(blockPos, neighborData.faces[3]).move(direction));
            boolean bl5 = !blockState8.shouldBlockVision(world, mutable) || blockState8.getOpacity() == 0;
            float n;
            int o;
            BlockState blockState9;
            if (!bl4 && !bl2)
            {
                n = f;
                o = i;
            }
            else
            {
                mutable.set(blockPos, neighborData.faces[0]).move(neighborData.faces[2]);
                blockState9 = world.getBlockState(mutable);
                n = brightnessCache.getFloat(blockState9, world, mutable);
                o = brightnessCache.getInt(blockState9, world, mutable);
            }

            float p;
            int q;
            if (!bl5 && !bl2)
            {
                p = f;
                q = i;
            }
            else
            {
                mutable.set(blockPos, neighborData.faces[0]).move(neighborData.faces[3]);
                blockState9 = world.getBlockState(mutable);
                p = brightnessCache.getFloat(blockState9, world, mutable);
                q = brightnessCache.getInt(blockState9, world, mutable);
            }

            float r;
            int s;
            if (!bl4 && !bl3)
            {
                r = f;
                s = i;
            }
            else
            {
                mutable.set(blockPos, neighborData.faces[1]).move(neighborData.faces[2]);
                blockState9 = world.getBlockState(mutable);
                r = brightnessCache.getFloat(blockState9, world, mutable);
                s = brightnessCache.getInt(blockState9, world, mutable);
            }

            float t;
            int u;
            if (!bl5 && !bl3)
            {
                t = f;
                u = i;
            }
            else
            {
                mutable.set(blockPos, neighborData.faces[1]).move(neighborData.faces[3]);
                blockState9 = world.getBlockState(mutable);
                t = brightnessCache.getFloat(blockState9, world, mutable);
                u = brightnessCache.getInt(blockState9, world, mutable);
            }

            int v = brightnessCache.getInt(state, world, pos);
            mutable.set(pos, direction);
            BlockState blockState10 = world.getBlockState(mutable);
            if (shapeState.get(0) || !blockState10.isOpaqueFullCube())
            {
                v = brightnessCache.getInt(blockState10, world, mutable);
            }

            float w = shapeState.get(0) ? brightnessCache.getFloat(world.getBlockState(blockPos), world, blockPos) : brightnessCache.getFloat(world.getBlockState(pos), world, pos);
            Tl translation = Tl.getTranslations(direction);
            float x;
            float y;
            float z;
            float aa;
            if (shapeState.get(1) && neighborData.nonCubicWeight)
            {
                x = (m + f + p + w) * 0.25F;
                y = (h + f + n + w) * 0.25F;
                z = (h + g + r + w) * 0.25F;
                aa = (m + g + t + w) * 0.25F;
                float ab = box[neighborData.field_4192[0].shape] * box[neighborData.field_4192[1].shape];
                float ac = box[neighborData.field_4192[2].shape] * box[neighborData.field_4192[3].shape];
                float ad = box[neighborData.field_4192[4].shape] * box[neighborData.field_4192[5].shape];
                float ae = box[neighborData.field_4192[6].shape] * box[neighborData.field_4192[7].shape];
                float af = box[neighborData.field_4185[0].shape] * box[neighborData.field_4185[1].shape];
                float ag = box[neighborData.field_4185[2].shape] * box[neighborData.field_4185[3].shape];
                float ah = box[neighborData.field_4185[4].shape] * box[neighborData.field_4185[5].shape];
                float ai = box[neighborData.field_4185[6].shape] * box[neighborData.field_4185[7].shape];
                float aj = box[neighborData.field_4180[0].shape] * box[neighborData.field_4180[1].shape];
                float ak = box[neighborData.field_4180[2].shape] * box[neighborData.field_4180[3].shape];
                float al = box[neighborData.field_4180[4].shape] * box[neighborData.field_4180[5].shape];
                float am = box[neighborData.field_4180[6].shape] * box[neighborData.field_4180[7].shape];
                float an = box[neighborData.field_4188[0].shape] * box[neighborData.field_4188[1].shape];
                float ao = box[neighborData.field_4188[2].shape] * box[neighborData.field_4188[3].shape];
                float ap = box[neighborData.field_4188[4].shape] * box[neighborData.field_4188[5].shape];
                float aq = box[neighborData.field_4188[6].shape] * box[neighborData.field_4188[7].shape];
                this.brightness[translation.firstCorner] = Math.clamp(x * ab + y * ac + z * ad + aa * ae, 0.0F, 1.0F);
                this.brightness[translation.secondCorner] = Math.clamp(x * af + y * ag + z * ah + aa * ai, 0.0F, 1.0F);
                this.brightness[translation.thirdCorner] = Math.clamp(x * aj + y * ak + z * al + aa * am, 0.0F, 1.0F);
                this.brightness[translation.fourthCorner] = Math.clamp(x * an + y * ao + z * ap + aa * aq, 0.0F, 1.0F);
                int ar = this.getAmbientOcclusionBrightness(l, i, q, v);
                int as = this.getAmbientOcclusionBrightness(k, i, o, v);
                int at = this.getAmbientOcclusionBrightness(k, j, s, v);
                int au = this.getAmbientOcclusionBrightness(l, j, u, v);
                this.light[translation.firstCorner] = this.getBrightness(ar, as, at, au, ab, ac, ad, ae);
                this.light[translation.secondCorner] = this.getBrightness(ar, as, at, au, af, ag, ah, ai);
                this.light[translation.thirdCorner] = this.getBrightness(ar, as, at, au, aj, ak, al, am);
                this.light[translation.fourthCorner] = this.getBrightness(ar, as, at, au, an, ao, ap, aq);
            }
            else
            {
                x = (m + f + p + w) * 0.25F;
                y = (h + f + n + w) * 0.25F;
                z = (h + g + r + w) * 0.25F;
                aa = (m + g + t + w) * 0.25F;
                this.light[translation.firstCorner] = this.getAmbientOcclusionBrightness(l, i, q, v);
                this.light[translation.secondCorner] = this.getAmbientOcclusionBrightness(k, i, o, v);
                this.light[translation.thirdCorner] = this.getAmbientOcclusionBrightness(k, j, s, v);
                this.light[translation.fourthCorner] = this.getAmbientOcclusionBrightness(l, j, u, v);
                this.brightness[translation.firstCorner] = x;
                this.brightness[translation.secondCorner] = y;
                this.brightness[translation.thirdCorner] = z;
                this.brightness[translation.fourthCorner] = aa;
            }

            x = world.getBrightness(direction, hasShade);

            for (int av = 0; av < this.brightness.length; ++av)
            {
                this.brightness[av] *= x;
            }
        }

        private int getAmbientOcclusionBrightness(int i, int j, int k, int l)
        {
            if (i == 0)
            {
                i = l;
            }

            if (j == 0)
            {
                j = l;
            }

            if (k == 0)
            {
                k = l;
            }

            return i + j + k + l >> 2 & 16711935;
        }

        private int getBrightness(int i, int j, int k, int l, float f, float g, float h, float m)
        {
            int n = (int) ((float) (i >> 16 & 255) * f + (float) (j >> 16 & 255) * g + (float) (k >> 16 & 255) * h + (float) (l >> 16 & 255) * m) & 255;
            int o = (int) ((float) (i & 255) * f + (float) (j & 255) * g + (float) (k & 255) * h + (float) (l & 255) * m) & 255;
            return n << 16 | o;
        }
    }

    // 2018
        /*
        private int getAoBrightness(int br1, int br2, int br3, int br4)
        {
            if (br1 == 0)
            {
                br1 = br4;
            }

            if (br2 == 0)
            {
                br2 = br4;
            }

            if (br3 == 0)
            {
                br3 = br4;
            }

            return br1 + br2 + br3 + br4 >> 2 & 16711935;
        }

        private int getVertexBrightness(int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_, float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_)
        {
            int i = (int)((float)(p_178203_1_ >> 16 & 255) * p_178203_5_ + (float)(p_178203_2_ >> 16 & 255) * p_178203_6_ + (float)(p_178203_3_ >> 16 & 255) * p_178203_7_ + (float)(p_178203_4_ >> 16 & 255) * p_178203_8_) & 255;
            int j = (int)((float)(p_178203_1_ & 255) * p_178203_5_ + (float)(p_178203_2_ & 255) * p_178203_6_ + (float)(p_178203_3_ & 255) * p_178203_7_ + (float)(p_178203_4_ & 255) * p_178203_8_) & 255;
            return i << 16 | j;
        }
    }

    public enum EnumNeighborInfo
    {
        DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true, new Orientation[]{Orientation.FLIP_WEST, Orientation.SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_WEST, Orientation.NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.SOUTH}),
        UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true, new Orientation[]{Orientation.EAST, Orientation.SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.SOUTH}, new Orientation[]{Orientation.EAST, Orientation.NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.SOUTH}),
        NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST}),
        SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.UP, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.DOWN, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.DOWN, Orientation.EAST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.UP, Orientation.EAST}),
        WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.UP, Orientation.SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.SOUTH}, new Orientation[]{Orientation.UP, Orientation.NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.SOUTH}),
        EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.FLIP_DOWN, Orientation.SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_DOWN, Orientation.NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.SOUTH});

        //private final Direction[] corners;
        //private final float shadeWeight;
        private final boolean doNonCubicWeight;
        private final Orientation[] vert0Weights;
        private final Orientation[] vert1Weights;
        private final Orientation[] vert2Weights;
        private final Orientation[] vert3Weights;
        private static final EnumNeighborInfo[] VALUES = new EnumNeighborInfo[6];

        EnumNeighborInfo(Direction[] p_i46236_3_, float p_i46236_4_, boolean p_i46236_5_, Orientation[] p_i46236_6_, Orientation[] p_i46236_7_, Orientation[] p_i46236_8_, Orientation[] p_i46236_9_)
        {
            //this.corners = p_i46236_3_;
            //this.shadeWeight = p_i46236_4_;
            this.doNonCubicWeight = p_i46236_5_;
            this.vert0Weights = p_i46236_6_;
            this.vert1Weights = p_i46236_7_;
            this.vert2Weights = p_i46236_8_;
            this.vert3Weights = p_i46236_9_;
        }

        public static EnumNeighborInfo getNeighbourInfo(Direction p_178273_0_)
        {
            return VALUES[p_178273_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }

    public enum Orientation
    {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        private final int shape;

        Orientation(Direction p_i46233_3_, boolean p_i46233_4_)
        {
            this.shape = p_i46233_3_.getId() + (p_i46233_4_ ? Direction.values().length : 0);
        }
    }

    enum VertexTranslations
    {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        private final int vert0;
        private final int vert1;
        private final int vert2;
        private final int vert3;
        private static final VertexTranslations[] VALUES = new VertexTranslations[6];

        VertexTranslations(int p_i46234_3_, int p_i46234_4_, int p_i46234_5_, int p_i46234_6_)
        {
            this.vert0 = p_i46234_3_;
            this.vert1 = p_i46234_4_;
            this.vert2 = p_i46234_5_;
            this.vert3 = p_i46234_6_;
        }

        public static VertexTranslations getVertexTranslations(Direction p_178184_0_)
        {
            return VALUES[p_178184_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }
         */

    private static class BC
    {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap intCache = Util.make(() ->
        {
            Long2IntLinkedOpenHashMap long2IntLinkedOpenHashMap = new Long2IntLinkedOpenHashMap(100, 0.25F)
            {
                protected void rehash(int newN)
                {
                }
            };
            long2IntLinkedOpenHashMap.defaultReturnValue(Integer.MAX_VALUE);
            return long2IntLinkedOpenHashMap;
        });
        private final Long2FloatLinkedOpenHashMap floatCache = Util.make(() ->
        {
            Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(100, 0.25F)
            {
                protected void rehash(int newN)
                {
                }
            };
            long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
            return long2FloatLinkedOpenHashMap;
        });

        private BC()
        {
        }

        public void enable()
        {
            this.enabled = true;
        }

        public void disable()
        {
            this.enabled = false;
            this.intCache.clear();
            this.floatCache.clear();
        }

        public int getInt(BlockState state, BlockRenderView world, BlockPos pos)
        {
            long l = pos.asLong();
            int i;
            if (this.enabled)
            {
                i = this.intCache.get(l);
                if (i != Integer.MAX_VALUE)
                {
                    return i;
                }
            }

            i = WorldRenderer.getLightmapCoordinates(world, state, pos);
            if (this.enabled)
            {
                if (this.intCache.size() == 100)
                {
                    this.intCache.removeFirstInt();
                }

                this.intCache.put(l, i);
            }

            return i;
        }

        public float getFloat(BlockState state, BlockRenderView blockView, BlockPos pos)
        {
            long l = pos.asLong();
            float f;
            if (this.enabled)
            {
                f = this.floatCache.get(l);
                if (!Float.isNaN(f))
                {
                    return f;
                }
            }

            f = state.getAmbientOcclusionLightLevel(blockView, pos);
            if (this.enabled)
            {
                if (this.floatCache.size() == 100)
                {
                    this.floatCache.removeFirstFloat();
                }

                this.floatCache.put(l, f);
            }

            return f;
        }
    }

    protected enum ND
    {
        DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true, new NO[]{NO.FLIP_WEST, NO.SOUTH, NO.FLIP_WEST, NO.FLIP_SOUTH, NO.WEST, NO.FLIP_SOUTH, NO.WEST, NO.SOUTH}, new NO[]{NO.FLIP_WEST, NO.NORTH, NO.FLIP_WEST, NO.FLIP_NORTH, NO.WEST, NO.FLIP_NORTH, NO.WEST, NO.NORTH}, new NO[]{NO.FLIP_EAST, NO.NORTH, NO.FLIP_EAST, NO.FLIP_NORTH, NO.EAST, NO.FLIP_NORTH, NO.EAST, NO.NORTH}, new NO[]{NO.FLIP_EAST, NO.SOUTH, NO.FLIP_EAST, NO.FLIP_SOUTH, NO.EAST, NO.FLIP_SOUTH, NO.EAST, NO.SOUTH}),
        UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true, new NO[]{NO.EAST, NO.SOUTH, NO.EAST, NO.FLIP_SOUTH, NO.FLIP_EAST, NO.FLIP_SOUTH, NO.FLIP_EAST, NO.SOUTH}, new NO[]{NO.EAST, NO.NORTH, NO.EAST, NO.FLIP_NORTH, NO.FLIP_EAST, NO.FLIP_NORTH, NO.FLIP_EAST, NO.NORTH}, new NO[]{NO.WEST, NO.NORTH, NO.WEST, NO.FLIP_NORTH, NO.FLIP_WEST, NO.FLIP_NORTH, NO.FLIP_WEST, NO.NORTH}, new NO[]{NO.WEST, NO.SOUTH, NO.WEST, NO.FLIP_SOUTH, NO.FLIP_WEST, NO.FLIP_SOUTH, NO.FLIP_WEST, NO.SOUTH}),
        NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true, new NO[]{NO.UP, NO.FLIP_WEST, NO.UP, NO.WEST, NO.FLIP_UP, NO.WEST, NO.FLIP_UP, NO.FLIP_WEST}, new NO[]{NO.UP, NO.FLIP_EAST, NO.UP, NO.EAST, NO.FLIP_UP, NO.EAST, NO.FLIP_UP, NO.FLIP_EAST}, new NO[]{NO.DOWN, NO.FLIP_EAST, NO.DOWN, NO.EAST, NO.FLIP_DOWN, NO.EAST, NO.FLIP_DOWN, NO.FLIP_EAST}, new NO[]{NO.DOWN, NO.FLIP_WEST, NO.DOWN, NO.WEST, NO.FLIP_DOWN, NO.WEST, NO.FLIP_DOWN, NO.FLIP_WEST}),
        SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true, new NO[]{NO.UP, NO.FLIP_WEST, NO.FLIP_UP, NO.FLIP_WEST, NO.FLIP_UP, NO.WEST, NO.UP, NO.WEST}, new NO[]{NO.DOWN, NO.FLIP_WEST, NO.FLIP_DOWN, NO.FLIP_WEST, NO.FLIP_DOWN, NO.WEST, NO.DOWN, NO.WEST}, new NO[]{NO.DOWN, NO.FLIP_EAST, NO.FLIP_DOWN, NO.FLIP_EAST, NO.FLIP_DOWN, NO.EAST, NO.DOWN, NO.EAST}, new NO[]{NO.UP, NO.FLIP_EAST, NO.FLIP_UP, NO.FLIP_EAST, NO.FLIP_UP, NO.EAST, NO.UP, NO.EAST}),
        WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new NO[]{NO.UP, NO.SOUTH, NO.UP, NO.FLIP_SOUTH, NO.FLIP_UP, NO.FLIP_SOUTH, NO.FLIP_UP, NO.SOUTH}, new NO[]{NO.UP, NO.NORTH, NO.UP, NO.FLIP_NORTH, NO.FLIP_UP, NO.FLIP_NORTH, NO.FLIP_UP, NO.NORTH}, new NO[]{NO.DOWN, NO.NORTH, NO.DOWN, NO.FLIP_NORTH, NO.FLIP_DOWN, NO.FLIP_NORTH, NO.FLIP_DOWN, NO.NORTH}, new NO[]{NO.DOWN, NO.SOUTH, NO.DOWN, NO.FLIP_SOUTH, NO.FLIP_DOWN, NO.FLIP_SOUTH, NO.FLIP_DOWN, NO.SOUTH}),
        EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new NO[]{NO.FLIP_DOWN, NO.SOUTH, NO.FLIP_DOWN, NO.FLIP_SOUTH, NO.DOWN, NO.FLIP_SOUTH, NO.DOWN, NO.SOUTH}, new NO[]{NO.FLIP_DOWN, NO.NORTH, NO.FLIP_DOWN, NO.FLIP_NORTH, NO.DOWN, NO.FLIP_NORTH, NO.DOWN, NO.NORTH}, new NO[]{NO.FLIP_UP, NO.NORTH, NO.FLIP_UP, NO.FLIP_NORTH, NO.UP, NO.FLIP_NORTH, NO.UP, NO.NORTH}, new NO[]{NO.FLIP_UP, NO.SOUTH, NO.FLIP_UP, NO.FLIP_SOUTH, NO.UP, NO.FLIP_SOUTH, NO.UP, NO.SOUTH});

        final Direction[] faces;
        final boolean nonCubicWeight;
        final NO[] field_4192;
        final NO[] field_4185;
        final NO[] field_4180;
        final NO[] field_4188;
        private static final ND[] VALUES = Util.make(new ND[6], (values) ->
        {
            values[Direction.DOWN.getId()] = DOWN;
            values[Direction.UP.getId()] = UP;
            values[Direction.NORTH.getId()] = NORTH;
            values[Direction.SOUTH.getId()] = SOUTH;
            values[Direction.WEST.getId()] = WEST;
            values[Direction.EAST.getId()] = EAST;
        });

        ND(final Direction[] faces, final float f, final boolean nonCubicWeight, final NO[] neighborOrientations, final NO[] neighborOrientations2, final NO[] neighborOrientations3, final NO[] neighborOrientations4)
        {
            this.faces = faces;
            this.nonCubicWeight = nonCubicWeight;
            this.field_4192 = neighborOrientations;
            this.field_4185 = neighborOrientations2;
            this.field_4180 = neighborOrientations3;
            this.field_4188 = neighborOrientations4;
        }

        public static ND getData(Direction direction)
        {
            return VALUES[direction.getId()];
        }
    }

    protected enum NO
    {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        final int shape;

        NO(final Direction direction, final boolean flip)
        {
            this.shape = direction.getId() + (flip ? DIRECTIONS.length : 0);
        }
    }

    private enum Tl
    {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int firstCorner;
        final int secondCorner;
        final int thirdCorner;
        final int fourthCorner;
        private static final Tl[] VALUES = Util.make(new Tl[6], (values) ->
        {
            values[Direction.DOWN.getId()] = DOWN;
            values[Direction.UP.getId()] = UP;
            values[Direction.NORTH.getId()] = NORTH;
            values[Direction.SOUTH.getId()] = SOUTH;
            values[Direction.WEST.getId()] = WEST;
            values[Direction.EAST.getId()] = EAST;
        });

        Tl(final int firstCorner, final int secondCorner, final int thirdCorner, final int fourthCorner)
        {
            this.firstCorner = firstCorner;
            this.secondCorner = secondCorner;
            this.thirdCorner = thirdCorner;
            this.fourthCorner = fourthCorner;
        }

        public static Tl getTranslations(Direction direction)
        {
            return VALUES[direction.getId()];
        }
    }
}

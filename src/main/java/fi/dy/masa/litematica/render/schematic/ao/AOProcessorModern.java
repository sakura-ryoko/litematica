package fi.dy.masa.litematica.render.schematic.ao;

import java.util.BitSet;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;

public class AOProcessorModern extends AOProcessor
{
    private static final Direction[] DIRECTIONS = Direction.values();

    @Override
    public void apply(BlockRenderView world, BlockState state, BlockPos pos, Direction direction, float[] box, BitSet shapeState, boolean hasShade)
    {
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
            float ab = box[neighborData.neighbor1[0].shape] * box[neighborData.neighbor1[1].shape];
            float ac = box[neighborData.neighbor1[2].shape] * box[neighborData.neighbor1[3].shape];
            float ad = box[neighborData.neighbor1[4].shape] * box[neighborData.neighbor1[5].shape];
            float ae = box[neighborData.neighbor1[6].shape] * box[neighborData.neighbor1[7].shape];
            float af = box[neighborData.neighbor2[0].shape] * box[neighborData.neighbor2[1].shape];
            float ag = box[neighborData.neighbor2[2].shape] * box[neighborData.neighbor2[3].shape];
            float ah = box[neighborData.neighbor2[4].shape] * box[neighborData.neighbor2[5].shape];
            float ai = box[neighborData.neighbor2[6].shape] * box[neighborData.neighbor2[7].shape];
            float aj = box[neighborData.neighbor3[0].shape] * box[neighborData.neighbor3[1].shape];
            float ak = box[neighborData.neighbor3[2].shape] * box[neighborData.neighbor3[3].shape];
            float al = box[neighborData.neighbor3[4].shape] * box[neighborData.neighbor3[5].shape];
            float am = box[neighborData.neighbor3[6].shape] * box[neighborData.neighbor3[7].shape];
            float an = box[neighborData.neighbor4[0].shape] * box[neighborData.neighbor4[1].shape];
            float ao = box[neighborData.neighbor4[2].shape] * box[neighborData.neighbor4[3].shape];
            float ap = box[neighborData.neighbor4[4].shape] * box[neighborData.neighbor4[5].shape];
            float aq = box[neighborData.neighbor4[6].shape] * box[neighborData.neighbor4[7].shape];
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

    public static class BC
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

        public BC() { }

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

            i = WorldRenderer.getLightmapCoordinates(world, pos);
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
        final NO[] neighbor1;
        final NO[] neighbor2;
        final NO[] neighbor3;
        final NO[] neighbor4;
        private static final ND[] VALUES = Util.make(new ND[6], (values) ->
        {
            values[Direction.DOWN.getIndex()] = DOWN;
            values[Direction.UP.getIndex()] = UP;
            values[Direction.NORTH.getIndex()] = NORTH;
            values[Direction.SOUTH.getIndex()] = SOUTH;
            values[Direction.WEST.getIndex()] = WEST;
            values[Direction.EAST.getIndex()] = EAST;
        });

        ND(final Direction[] faces, final float f, final boolean nonCubicWeight, final NO[] neighbor1, final NO[] neighbor2, final NO[] neighbor3, final NO[] neighbor4)
        {
            this.faces = faces;
            this.nonCubicWeight = nonCubicWeight;
            this.neighbor1 = neighbor1;
            this.neighbor2 = neighbor2;
            this.neighbor3 = neighbor3;
            this.neighbor4 = neighbor4;
        }

        public static ND getData(Direction direction)
        {
            return VALUES[direction.getIndex()];
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
            this.shape = direction.getIndex() + (flip ? DIRECTIONS.length : 0);
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
            values[Direction.DOWN.getIndex()] = DOWN;
            values[Direction.UP.getIndex()] = UP;
            values[Direction.NORTH.getIndex()] = NORTH;
            values[Direction.SOUTH.getIndex()] = SOUTH;
            values[Direction.WEST.getIndex()] = WEST;
            values[Direction.EAST.getIndex()] = EAST;
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
            return VALUES[direction.getIndex()];
        }
    }
}

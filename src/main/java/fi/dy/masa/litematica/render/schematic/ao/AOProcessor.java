package fi.dy.masa.litematica.render.schematic.ao;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.config.Configs;

public abstract class AOProcessor
{
    protected final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    protected AOLightmap lightmap;
    protected boolean cubic;
    protected boolean hasNeighbors;

    public static AOProcessor get(AOLightmap lightmap)
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            AOProcessor ao = new AOProcessorModern();
            ao.lightmap = lightmap;
            return ao;
        }
        else
        {
            AOProcessor ao = new AOProcessorLegacy();
            ao.lightmap = lightmap;
            return ao;
        }
    }

    public int getLight(final BlockAndTintGetter world, final BlockState state, final BlockPos pos)
    {
        return this.lightmap.brightnessCache.getLight(state, world, pos);
    }

    public abstract void prepareSmooth(final BlockAndTintGetter world, final BlockState state, final BlockPos pos,
                                       final BakedQuad quad, final QuadInstance instance);

    public abstract void prepareFlat(final BlockAndTintGetter world, final BlockState state, final BlockPos pos,
                                     final int light, final BakedQuad quad, final QuadInstance instance);

    public abstract void prepareShape(final BlockAndTintGetter world, final BlockState state, final BlockPos pos,
                                      final BakedQuad quad, final boolean useAO);
}

package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;

import fi.dy.masa.malilib.render.MaLiLibPipelines;

public record ChunkRenderLayers()
{
    public static final List<ChunkSectionLayer> BLOCK_RENDER_LAYERS = getBlockRenderLayers();
    public static final List<OverlayRenderType> TYPES = getTypes();
    public static final HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> PIPELINE_MAP = getBlockRenderPipelineMap();

    private static List<ChunkSectionLayer> getBlockRenderLayers()
    {
	    // I know that there is the BlockRenderLayer.values(), but this is to customize this.
	    return new ArrayList<>(List.of(ChunkSectionLayer.values()));
    }

    private static HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> getBlockRenderPipelineMap()
    {
        HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> map = new HashMap<>();

        map.put(ChunkSectionLayer.SOLID,         Pair.of(MaLiLibPipelines.LEGACY_SOLID_TERRAIN,         MaLiLibPipelines.LEGACY_SOLID_TERRAIN_OFFSET));
        map.put(ChunkSectionLayer.CUTOUT,        Pair.of(MaLiLibPipelines.LEGACY_CUTOUT_TERRAIN,        MaLiLibPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET));
        map.put(ChunkSectionLayer.TRANSLUCENT,   Pair.of(MaLiLibPipelines.LEGACY_TRANSLUCENT,           MaLiLibPipelines.LEGACY_TRANSLUCENT_OFFSET));
//        map.put(ChunkSectionLayer.SOLID,         Pair.of(LitematicaPipelines.LEGACY_SOLID_TERRAIN,          LitematicaPipelines.LEGACY_SOLID_TERRAIN_OFFSET));
//        map.put(ChunkSectionLayer.CUTOUT,        Pair.of(LitematicaPipelines.LEGACY_CUTOUT_TERRAIN,         LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET));
//        map.put(ChunkSectionLayer.TRANSLUCENT,   Pair.of(LitematicaPipelines.LEGACY_TRANSLUCENT,            LitematicaPipelines.LEGACY_TRANSLUCENT_OFFSET));

        return map;
    }

    public static Pair<RenderPipeline, RenderPipeline> getWireframe()
    {
        return Pair.of(MaLiLibPipelines.LEGACY_WIREFRAME, MaLiLibPipelines.LEGACY_WIREFRAME_OFFSET);
//        return Pair.of(LitematicaPipelines.LEGACY_WIREFRAME, LitematicaPipelines.LEGACY_WIREFRAME_OFFSET);
    }

    private static List<OverlayRenderType> getTypes()
    {
        // In case we need to add additional Types in the future
        return Arrays.stream(OverlayRenderType.values()).toList();
    }

    public static String getFriendlyName(RenderType layer)
    {
        String base = layer.toString();
        String[] results1;

        if (base.contains(":"))
        {
            String[] results2;

            results1 = base.split(":", 2);

            if (results1[0].contains("["))
            {
                results2 = results1[0].split("\\[");

                return layer.format() + "/" + results2[1];
            }

            return layer.format() + "/" + results1[0];
        }

        return layer.format() + "/" + base;
    }
}

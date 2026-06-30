package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import fi.dy.masa.malilib.render.MaLiLibPipelines;

public record ChunkRenderLayers()
{
    public static final List<ChunkSectionLayer> BLOCK_RENDER_LAYERS = getBlockRenderLayers();
    public static final List<OverlayRenderType> TYPES = getTypes();
    public static final HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> PIPELINE_MAP = getBlockRenderPipelineMap();

    private static List<ChunkSectionLayer> getBlockRenderLayers()
    {
        List<ChunkSectionLayer> list = new ArrayList<>();

        // I know that there is the BlockRenderLayer.values(), but this is to customize this.
        list.add(ChunkSectionLayer.SOLID);
        list.add(ChunkSectionLayer.CUTOUT);
        list.add(ChunkSectionLayer.TRANSLUCENT);
        list.add(ChunkSectionLayer.TRIPWIRE);

        return list;
    }

    private static HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> getBlockRenderPipelineMap()
    {
        HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> map = new HashMap<>();

        map.put(ChunkSectionLayer.SOLID,         Pair.of(MaLiLibPipelines.LEGACY_SOLID_TERRAIN_MASA,       MaLiLibPipelines.LEGACY_SOLID_TERRAIN_MASA_OFFSET));
        map.put(ChunkSectionLayer.CUTOUT,        Pair.of(MaLiLibPipelines.LEGACY_CUTOUT_TERRAIN_MASA,      MaLiLibPipelines.LEGACY_CUTOUT_TERRAIN_MASA_OFFSET));
        map.put(ChunkSectionLayer.TRANSLUCENT,   Pair.of(MaLiLibPipelines.LEGACY_TRANSLUCENT_MASA,         MaLiLibPipelines.LEGACY_TRANSLUCENT_MASA_OFFSET));
        map.put(ChunkSectionLayer.TRIPWIRE,      Pair.of(MaLiLibPipelines.LEGACY_TRIPWIRE_TERRAIN_MASA,    MaLiLibPipelines.LEGACY_TRIPWIRE_TERRAIN_MASA_OFFSET));

        return map;
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

                return layer.mode().name() + "/" + results2[1];
            }

            return layer.mode().name() + "/" + results1[0];
        }

        return layer.mode().name() + "/" + base;
    }
}

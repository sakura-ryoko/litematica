package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.render.RenderLayer;

public record ChunkRenderLayers()
{
    public static final List<RenderLayer> LAYERS = getLayers();
    public static final List<OverlayRenderType> TYPES = getTypes();

    private static List<RenderLayer> getLayers()
    {
        List<RenderLayer> list = new ArrayList<>(RenderLayer.getBlockLayers());

        // Water Rendering
        list.add(RenderLayer.getWaterMask());

        return list;
    }

    private static List<OverlayRenderType> getTypes()
    {
        // In case we need to add additional Types in the future
        return Arrays.stream(OverlayRenderType.values()).toList();
    }

    public static String getFriendlyName(RenderLayer layer)
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

                return layer.getDrawMode().name() + "/" + results2[1];
            }

            return layer.getDrawMode().name() + "/" + results1[0];
        }

        return layer.getDrawMode().name() + "/" + base;
    }
}

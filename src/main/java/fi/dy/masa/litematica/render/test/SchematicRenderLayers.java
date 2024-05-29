package fi.dy.masa.litematica.render.test;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.model.ModelLoader;

public class SchematicRenderLayers
{
    public static final List<RenderLayer> LAYERS = new ArrayList<>();

    static
    {
        LAYERS.add(RenderLayer.getSolid());
        LAYERS.add(RenderLayer.getCutout());
        LAYERS.add(RenderLayer.getCutoutMipped());
        LAYERS.add(RenderLayer.getTranslucent());
        LAYERS.add(TexturedRenderLayers.getShieldPatterns());
        LAYERS.add(TexturedRenderLayers.getBeds());
        LAYERS.add(TexturedRenderLayers.getShulkerBoxes());
        LAYERS.add(TexturedRenderLayers.getSign());
        LAYERS.add(TexturedRenderLayers.getHangingSign());
        LAYERS.add(TexturedRenderLayers.getChest());
        LAYERS.add(RenderLayer.getArmorEntityGlint());
        LAYERS.add(RenderLayer.getGlint());
        LAYERS.add(RenderLayer.getGlintTranslucent());
        LAYERS.add(RenderLayer.getEntityGlint());
        LAYERS.add(RenderLayer.getDirectEntityGlint());
        LAYERS.add(RenderLayer.getWaterMask());
        LAYERS.addAll(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS);
    }
}

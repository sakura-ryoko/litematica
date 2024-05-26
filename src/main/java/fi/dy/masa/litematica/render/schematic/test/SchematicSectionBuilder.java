package fi.dy.masa.litematica.render.schematic.test;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value = EnvType.CLIENT)
public class SchematicSectionBuilder
{
    SchematicBlockRenderManager blockRenderManager;
    SchematicEntityRenderDispatch entityRenderDispatch;

    public SchematicSectionBuilder(SchematicBlockRenderManager blocksManager, SchematicEntityRenderDispatch entityRenderer)
    {
        this.blockRenderManager = blocksManager;
        this.entityRenderDispatch = entityRenderer;
    }
}

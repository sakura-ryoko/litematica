package fi.dy.masa.litematica.mixin.hud;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud
{
    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void litematica_addDebugLines(CallbackInfoReturnable<List<String>> cir)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            List<String> list = cir.getReturnValue();
            Pair<String, String> pair = EntityUtils.getEntityDebug();
            String pre = GuiBase.TXT_GOLD;
            String rst = GuiBase.TXT_RST;

            WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add(String.format("%s[Litematica]%s %s",
                                   pre, rst, renderer.getDebugInfoRenders()));

            String str = String.format("E: %d TE: %d C: %d, CT: %d, CV: %d",
                                       world.getRegularEntityCount(),
//                                       world.getEntityDebug(),
                                       world.getChunkProvider().getTileEntityCount(),
                                       world.getChunkProvider().getLoadedChunkCount(),
                                       DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
                                       DataManager.getSchematicPlacementManager().getLastVisibleChunksCount()
            );

            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));

            if (!pair.getLeft().isEmpty())
            {
                list.add(String.format("%s[%s]%s %s", pre, pair.getLeft(), rst, pair.getRight()));
            }
        }
    }
}

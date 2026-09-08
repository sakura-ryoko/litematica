package fi.dy.masa.litematica.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GameRenderer.class, priority = 600)
public interface IMixinGameRenderer
{
    @Accessor("fogRenderer")
    FogRenderer litematica_getFogRenderer();

    @Accessor("renderBlockOutline")
    boolean litematica_isBlockOutlineEnabled();
}

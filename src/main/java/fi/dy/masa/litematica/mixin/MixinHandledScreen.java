package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    private MixinHandledScreen(Text title)
    {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void litematica_renderSlotHighlightsPre(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory((HandledScreen<?>) (Object) this, this.client);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void litematica_renderSlotHighlightsPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory((HandledScreen<?>) (Object) this, this.client);
    }
}

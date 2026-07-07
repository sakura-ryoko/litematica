package fi.dy.masa.litematica.mixin.screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListItemCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    @Unique
    private boolean litematica_containerScanned = false;

    private MixinHandledScreen(Text title)
    {
        super(title);
    }

    @Inject(method = "renderMain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void litematica_renderSlotHighlightsPre(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;

        // Scan container items for material list cache (only once per screen open)
        if (!this.litematica_containerScanned)
        {
            MaterialListItemCache.getInstance().scanContainer(screen.getScreenHandler().slots);
            this.litematica_containerScanned = true;
        }

        MaterialListHudRenderer.renderLookedAtBlockInInventory(drawContext, screen, this.client);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void litematica_renderSlotHighlightsPost(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory(drawContext, (HandledScreen<?>) (Object) this, this.client);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void litematica_onContainerClose(CallbackInfo ci)
    {
        // Reset the scanned flag when container closes
        this.litematica_containerScanned = false;
    }
}

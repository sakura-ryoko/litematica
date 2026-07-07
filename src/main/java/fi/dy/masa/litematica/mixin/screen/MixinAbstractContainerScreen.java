package fi.dy.masa.litematica.mixin.screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListItemCache;
import fi.dy.masa.malilib.render.GuiContext;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen
{
    @Unique
    private boolean litematica_containerScanned = false;

    private MixinAbstractContainerScreen(Component title)
    {
        super(title);
    }

    @Inject(method = "extractContents",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getHoveredSlot(DD)Lnet/minecraft/world/inventory/Slot;",
                     shift = At.Shift.AFTER)
    )
    private void litematica_renderSlotHighlightsPre(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci)
    {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        if (Configs.Generic.MATERIAL_LIST_CONTAINER_SCAN.getBooleanValue())
        {
            // Scan container items for material list cache (only once per screen open)
            if (!this.litematica_containerScanned)
            {
                MaterialListItemCache.getInstance().scanContainer(screen.getMenu().slots);
                this.litematica_containerScanned = true;
            }
        }

        MaterialListHudRenderer.renderLookedAtBlockInInventory(GuiContext.fromGuiGraphics(graphics), screen, this.minecraft);
    }

//    @Inject(method = "extractRenderState", at = @At("TAIL"))
//    private void litematica_renderSlotHighlightsPost(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci)
//    {
//        MaterialListHudRenderer.renderLookedAtBlockInInventory(GuiContext.fromGuiGraphics(graphics), (AbstractContainerScreen<?>) (Object) this, this.minecraft);
//    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void litematica_onContainerClose(CallbackInfo ci)
    {
        // Reset the scanned flag when container closes
        this.litematica_containerScanned = false;
    }
}

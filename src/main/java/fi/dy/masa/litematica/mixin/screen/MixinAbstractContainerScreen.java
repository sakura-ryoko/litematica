package fi.dy.masa.litematica.mixin.screen;

//@Mixin(AbstractContainerScreen.class)
@Deprecated(forRemoval = true)
public abstract class MixinAbstractContainerScreen // extends Screen
{
//    @Unique
//    private boolean litematica_containerScanned = false;
//
//    private MixinAbstractContainerScreen(Component title)
//    {
//        super(title);
//    }
//
//    @Inject(method = "renderContents",
//            at = @At(value = "INVOKE",
//                     target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
//    private void litematica_renderSlotHighlightsPre(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
//    {
//        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
//
//        if (Configs.Generic.MATERIAL_LIST_CONTAINER_SCAN.getBooleanValue())
//        {
//            // Scan container items for material list cache (only once per screen open)
//            if (!this.litematica_containerScanned)
//            {
//                MaterialListItemCache.getInstance().scanContainer(screen.getMenu().slots);
//                this.litematica_containerScanned = true;
//            }
//        }
//
//        MaterialListHudRenderer.renderLookedAtBlockInInventory(GuiContext.fromGuiGraphics(drawContext), screen, this.minecraft);
//    }
//
////    @Inject(method = "render", at = @At("TAIL"))
////    private void litematica_renderSlotHighlightsPost(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
////    {
////        MaterialListHudRenderer.renderLookedAtBlockInInventory(GuiContext.fromGuiGraphics(drawContext), (AbstractContainerScreen<?>) (Object) this, this.minecraft);
////    }
//
//    @Inject(method = "onClose", at = @At("HEAD"))
//    private void litematica_onContainerClose(CallbackInfo ci)
//    {
//        // Reset the scanned flag when container closes
//        this.litematica_containerScanned = false;
//    }
}

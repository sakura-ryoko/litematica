package fi.dy.masa.litematica.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Post Re-Write code
 */
@Mixin(value = MinecraftClient.class)
public abstract class MixinMinecraftClient_easyPlace
{
    /*
    @Inject(method = "handleInputEvents",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"))
    private void onUseKeyPre(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            EasyPlaceUtils.setIsFirstClick();
        }
    }

    @Inject(method = "doItemUse", at = @At("TAIL"))
    private void onUseKeyPost(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            if (EasyPlaceUtils.shouldDoEasyPlaceActions())
            {
                EasyPlaceUtils.onRightClickTail();
            }
        }
    }
     */
}

package fi.dy.masa.litematica.mixin.post_rewrite;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.post_rewrite.EasyPlaceUtils;

@Mixin(value = MinecraftClient.class)
public abstract class MixinMinecraftClient_easyPlace
{
    /*
    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"))
    private void onUseKeyPre(CallbackInfo ci)
    {
        if (Configs.Test.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() == false)
        {
            EasyPlaceUtils.setIsFirstClick();
        }
    }

    @Inject(method = "doItemUse", at = @At("TAIL"))
    private void onUseKeyPost(CallbackInfo ci)
    {
        if (Configs.Test.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() == false)
        {
            if (EasyPlaceUtils.shouldDoEasyPlaceActions())
            {
                EasyPlaceUtils.onRightClickTail();
            }
        }
    }
     */
}

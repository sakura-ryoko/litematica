package fi.dy.masa.litematica.mixin.easyplace;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import fi.dy.masa.litematica.util.WorldUtils;

/**
 * Post Re-Write code
 */
@Mixin(value = Minecraft.class)
public abstract class MixinMinecraftClient_easyPlace
{
    @Inject(method = "startUseItem()V", at = @At(value = "INVOKE",
                                                 target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 0), cancellable = true)
    private void litematica_handlePlacementRestriction(CallbackInfo ci)
    {
        if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            if (Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue()
                && EasyPlaceUtils.handlePlacementRestriction())
            {
                ci.cancel();
            }
            else if (WorldUtils.handlePlacementRestriction((Minecraft)(Object) this))
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;startUseItem()V"))
    private void onUseKeyPre(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            EasyPlaceUtils.setIsFirstClick();
        }
    }

    @Inject(method = "startUseItem", at = @At("TAIL"))
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
}

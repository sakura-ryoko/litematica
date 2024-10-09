package fi.dy.masa.litematica.mixin.post_rewrite;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.post_rewrite.EasyPlaceUtils;

@Mixin(value = ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager
{
    /*
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        if (Configs.Test.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() == false)
        {
            // Prevent recursion, since the Easy Place mode can call this code again
            if (EasyPlaceUtils.isHandling() == false)
            {
                if (EasyPlaceUtils.shouldDoEasyPlaceActions())
                {
                    if (EasyPlaceUtils.handleEasyPlaceWithMessage())
                    {
                        cir.setReturnValue(ActionResult.FAIL);
                    }
                }
                else
                {
                    if (Configs.Test.PLACEMENT_RESTRICTION.getBooleanValue() &&
                        Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue() == false)
                    {
                        if (EasyPlaceUtils.handlePlacementRestriction())
                        {
                            cir.setReturnValue(ActionResult.FAIL);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "interactBlockInternal",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;",
            shift = At.Shift.BEFORE))
    private void onInteractBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        if (Configs.Test.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() == false)
        {
            // Prevent recursion, since the Easy Place mode can call this code again
            if (EasyPlaceUtils.isHandling() == false)
            {
                if (EasyPlaceUtils.shouldDoEasyPlaceActions() &&
                        EasyPlaceUtils.handleEasyPlaceWithMessage())
                {
                    cir.setReturnValue(ActionResult.FAIL);
                }
            }
        }
    }
    */
}

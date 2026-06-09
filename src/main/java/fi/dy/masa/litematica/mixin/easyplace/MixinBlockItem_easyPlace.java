package fi.dy.masa.litematica.mixin.easyplace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.util.PlacementHandler.UseContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = BlockItem.class, priority = 980)
public abstract class MixinBlockItem_easyPlace extends Item
{
    private MixinBlockItem_easyPlace(Item.Properties builder)
    {
        super(builder);
    }

    @Shadow protected abstract BlockState getPlacementState(BlockPlaceContext context);
    @Shadow protected abstract boolean canPlace(BlockPlaceContext context, BlockState stateForPlacement);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void litematica_modifyPlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_SP_HANDLING.getBooleanValue())
        {
            BlockState stateOrig = this.getBlock().getStateForPlacement(context);

            if (stateOrig != null)
            {
				if (!Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue() || this.canPlace(context, stateOrig))
				{
					UseContext ctx = UseContext.from(context, context.getHand());
					cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, ctx));
				}
            }
        }
    }
}

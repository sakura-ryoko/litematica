package fi.dy.masa.litematica.mixin.block;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.config.Configs;

@Mixin({ RailBlock.class, DetectorRailBlock.class, PoweredRailBlock.class})
public abstract class MixinRailBlocks extends BaseRailBlock
{
    protected MixinRailBlocks(boolean disableCorners, BlockBehaviour.Properties builder)
    {
        super(disableCorners, builder);
    }

    @Inject(method = "rotate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Rotation;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void litematica_fixRailRotation(BlockState state, Rotation rotation, CallbackInfoReturnable<BlockState> cir)
    {
        if (Configs.Generic.FIX_RAIL_ROTATION.getBooleanValue() && rotation == Rotation.CLOCKWISE_180)
        {
            RailShape shape = null;

            if (((Object) this) instanceof RailBlock)
            {
                shape = state.getValue(RailBlock.SHAPE);
            }
            else if (((Object) this) instanceof DetectorRailBlock)
            {
                shape = state.getValue(DetectorRailBlock.SHAPE);
            }
            else if (((Object) this) instanceof PoweredRailBlock)
            {
                shape = state.getValue(PoweredRailBlock.SHAPE);
            }

            // Fix the incomplete switch statement causing the ccw_90 rotation being used instead
            // for the 180-degree rotation of the straight rails.
            if (shape == RailShape.EAST_WEST || shape == RailShape.NORTH_SOUTH)
            {
                cir.setReturnValue(state);
                cir.cancel();
            }
        }
    }
}

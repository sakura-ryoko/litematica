package fi.dy.masa.litematica.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk
{
    @WrapOperation(method = "setBlockState",
                   slice = @Slice(from = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;")),
                   at = @At(value = "INVOKE",
						 target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
						 ordinal = 0))
    private boolean litematica_redirectIsRemote(Level instance, Operation<Boolean> original)
    {
        return WorldUtils.shouldPreventBlockUpdates(instance) || original.call(instance);
    }
}

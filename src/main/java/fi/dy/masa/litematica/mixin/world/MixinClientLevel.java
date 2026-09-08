package fi.dy.masa.litematica.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level
{
    private MixinClientLevel(WritableLevelData properties,
                             ResourceKey<Level> registryRef,
                             RegistryAccess manager,
                             Holder<DimensionType> dimension,
                             boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates)
    {
        super(properties, registryRef, manager, dimension, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void litematica_onHandleBlockUpdate(BlockPos pos, BlockState blockState, int updateFlag, CallbackInfo ci)
    {
        SchematicVerifier.markVerifierBlockChanges(pos);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}

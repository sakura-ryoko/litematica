package fi.dy.masa.litematica.mixin.model;

import java.util.Map;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.schematic.FallbackBlocks;

@Mixin(BlockStateDefinitions.class)
public class MixinBlockStateDefinitions
{
	@Mutable @Final @Shadow private static Map<Identifier, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void litematica$fillFallbackBlocks(CallbackInfo ci)
	{
		FallbackBlocks.register();
		ImmutableMap.Builder<Identifier, StateDefinition<Block, BlockState>> builder = new ImmutableMap.Builder<>();

		builder.putAll(STATIC_DEFINITIONS);

		for (Identifier id : FallbackBlocks.ID_TO_STATE_MANAGER.keySet())
		{
			builder.put(id, FallbackBlocks.ID_TO_STATE_MANAGER.get(id));
		}

		STATIC_DEFINITIONS = builder.build();
	}
}

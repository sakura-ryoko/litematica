package fi.dy.masa.litematica.mixin.block;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStatesLoader;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.render.schematic.blocks.FallbackBlocks;

@Mixin(BlockStatesLoader.class)
public class MixinBlockStatesLoader
{
	@Mutable
	@Final @Shadow private static Map<Identifier, StateManager<Block, BlockState>> STATIC_DEFINITIONS;

	@Inject(method = "getIdToStatesConverter", at = @At("HEAD"))
	private static void litematica$fillFallbackBlocks(CallbackInfoReturnable<Function<Identifier, StateManager<Block, BlockState>>> cir)
	{
		FallbackBlocks.register();
		ImmutableMap.Builder<Identifier, StateManager<Block, BlockState>> builder = new ImmutableMap.Builder<>();

		builder.putAll(STATIC_DEFINITIONS);

		for (Identifier id : FallbackBlocks.ID_TO_STATE_MANAGER.keySet())
		{
			if (!STATIC_DEFINITIONS.containsKey(id))
			{
				builder.put(id, FallbackBlocks.ID_TO_STATE_MANAGER.get(id));
			}
		}

		STATIC_DEFINITIONS = builder.build();
	}
}

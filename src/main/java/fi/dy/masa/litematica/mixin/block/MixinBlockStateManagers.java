package fi.dy.masa.litematica.mixin.block;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateManagers;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.schematic.blocks.FallbackBlocks;

@Mixin(BlockStateManagers.class)
public class MixinBlockStateManagers
{
	@Shadow private static Map<Identifier, StateManager<Block, BlockState>> STATIC_MANAGERS;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void litematica$fillFallbackBlocks(CallbackInfo ci)
	{
		FallbackBlocks.register();
		ImmutableMap.Builder<Identifier, StateManager<Block, BlockState>> builder = new ImmutableMap.Builder<>();

		builder.putAll(STATIC_MANAGERS);

		for (Identifier id : FallbackBlocks.ID_TO_STATE_MANAGER.keySet())
		{
			builder.put(id, FallbackBlocks.ID_TO_STATE_MANAGER.get(id));
		}

		STATIC_MANAGERS = builder.build();
	}
}

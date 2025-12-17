package fi.dy.masa.litematica.interfaces;

import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;

public interface ISchematicPickBlockEventListener
{
	Supplier<String> getName();

	void onSchematicPickBlockCancelled(Supplier<String> cancelledBy);

	SchematicPickBlockEventResult onSchematicPickBlockStart(boolean closest);

	SchematicPickBlockEventResult onSchematicPickBlockPreGather(World schematicWorld, BlockPos pos, BlockState expectedState);

	SchematicPickBlockEventResult onSchematicPickBlockPrePick(World schematicWorld, BlockPos pos, BlockState expectedState, ItemStack stack);

	void onSchematicPickBlockSuccess();
}

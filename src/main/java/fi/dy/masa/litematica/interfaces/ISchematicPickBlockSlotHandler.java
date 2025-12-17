package fi.dy.masa.litematica.interfaces;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;

public interface ISchematicPickBlockSlotHandler
{
	Supplier<String> getName();

	SchematicPickBlockEventResult executePickBlock(World schematicWorld, BlockPos pos, ItemStack stack);
}

package fi.dy.masa.litematica.interfaces;

import net.minecraft.item.ItemStack;

import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;

public interface ISchematicPickBlockEventManager
{
	void registerSchematicPickBlockEventListener(ISchematicPickBlockEventListener listener);

	SchematicPickBlockEventResult invokeRedirectPickBlockStack(ISchematicPickBlockEventListener listener, ItemStack newStack);

	SchematicPickBlockEventResult invokeRedirectPickBlockSlotHandler(ISchematicPickBlockEventListener listener, ISchematicPickBlockSlotHandler slotHandler);
}

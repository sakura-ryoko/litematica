package fi.dy.masa.litematica.interfaces;

import java.util.function.Supplier;

import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;
import net.minecraft.world.item.ItemStack;

public interface ISchematicPickBlockEventManager
{
	void registerSchematicPickBlockEventListener(ISchematicPickBlockEventListener listener);

	SchematicPickBlockEventResult invokeRedirectPickBlockStack(ISchematicPickBlockEventListener listener, ItemStack newStack);

	SchematicPickBlockEventResult invokeRedirectPickBlockSlotHandler(ISchematicPickBlockEventListener listener, ISchematicPickBlockSlotHandler slotHandler);

	boolean hasPickStack();

	boolean hasSlotHandler();

	ItemStack getPickStack();

	boolean isProcessingCancelled();

	void resetCancelled();

	Supplier<String> getProcessingCancelledBy();
}

package fi.dy.masa.litematica.schematic.pickblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.interfaces.ISchematicPickBlockEventListener;
import fi.dy.masa.litematica.interfaces.ISchematicPickBlockEventManager;
import fi.dy.masa.litematica.interfaces.ISchematicPickBlockSlotHandler;

/**
 * This adds event callbacks that are Cancel-able, and it can easily Override the picked stack, and how to pick it.
 * This is another Event Handler designed to make it so that Third-Party mods, such as 'ItemSwapper'
 * can handle pulling items out of shulkers, etc.
 */
public class SchematicPickBlockEventHandler implements ISchematicPickBlockEventManager
{
	private static final SchematicPickBlockEventHandler INSTANCE = new SchematicPickBlockEventHandler();
	private final List<ISchematicPickBlockEventListener> handlers = new ArrayList<>();
	public static SchematicPickBlockEventHandler getInstance() { return INSTANCE; }
	private ItemStack pickStack;
	private ISchematicPickBlockSlotHandler slotHandler;
	private boolean processingCancelled;
	private Supplier<String> processingCanceledBy;

	private SchematicPickBlockEventHandler()
	{
		this.pickStack = ItemStack.EMPTY;
		this.slotHandler = null;
		this.processingCancelled = false;
		this.processingCanceledBy = () -> "not_cancelled";
	}

	@Override
	public void registerSchematicPickBlockEventListener(@Nonnull ISchematicPickBlockEventListener listener)
	{
		if (!this.handlers.contains(listener))
		{
			this.handlers.add(listener);
		}
	}

	@Override
	public SchematicPickBlockEventResult invokeRedirectPickBlockStack(ISchematicPickBlockEventListener listener, ItemStack newStack)
	{
		if (!this.pickStack.isEmpty())
		{
			return SchematicPickBlockEventResult.ERROR;
		}
		else
		{
			this.pickStack = newStack.copy();
			return SchematicPickBlockEventResult.SUCCESS;
		}
	}

	@Override
	public SchematicPickBlockEventResult invokeRedirectPickBlockSlotHandler(ISchematicPickBlockEventListener listener, ISchematicPickBlockSlotHandler slotHandler)
	{
		if (this.slotHandler != null)
		{
			return SchematicPickBlockEventResult.ERROR;
		}
		else
		{
			this.slotHandler = slotHandler;
			return SchematicPickBlockEventResult.SUCCESS;
		}
	}

	public boolean hasPickStack()
	{
		return !this.pickStack.isEmpty();
	}

	public boolean hasSlotHandler()
	{
		return this.slotHandler != null;
	}

	public boolean isProcessingCancelled()
	{
		return this.processingCancelled;
	}

	public ItemStack getPickStack()
	{
		return this.pickStack;
	}

	public Supplier<String> getProcessingCanceledBy()
	{
		return this.processingCanceledBy;
	}

	@ApiStatus.Internal
	public boolean executePickBlockHandler(World world, BlockPos pos, ItemStack stack)
	{
		if (this.slotHandler != null)
		{
			SchematicPickBlockEventResult result = this.slotHandler.executePickBlock(world, pos, stack);

			if (result == SchematicPickBlockEventResult.CANCEL)
			{
				Litematica.LOGGER.warn("SchematicPickBlockEventHandler: Processing cancelled by: {} during 'executePickBlockHandler'", this.slotHandler.getName().get());
				this.processingCancelled = true;
				this.processingCanceledBy = this.slotHandler.getName();
				return true;
			}
			else if (result == SchematicPickBlockEventResult.ERROR)
			{
				Litematica.LOGGER.error("SchematicPickBlockEventHandler: Error processing 'executePickBlockHandler' from: {}", this.slotHandler.getName().get());
			}
		}

		return false;
	}

	@ApiStatus.Internal
	public boolean onSchematicPickBlockStart(boolean closest)
	{
		for (ISchematicPickBlockEventListener handler : this.handlers)
		{
			// This will possibly never be true here, but you never know.
			if (this.isProcessingCancelled())
			{
				handler.onSchematicPickBlockCancelled(this.getProcessingCanceledBy());
			}
			else
			{
				SchematicPickBlockEventResult result = handler.onSchematicPickBlockStart(closest);

				if (result == SchematicPickBlockEventResult.CANCEL)
				{
					Litematica.LOGGER.warn("SchematicPickBlockEventHandler: Processing cancelled by: {} during 'onSchematicPickBlockStart'", handler.getName().get());
					this.processingCancelled = true;
					this.processingCanceledBy = handler.getName();
				}
				else if (result == SchematicPickBlockEventResult.ERROR)
				{
					Litematica.LOGGER.error("SchematicPickBlockEventHandler: Error processing 'onSchematicPickBlockStart' from: {}", handler.getName().get());
				}
			}
		}

		return this.isProcessingCancelled();
	}

	@ApiStatus.Internal
	public boolean onSchematicPickBlockPreGather(World schematicWorld, BlockPos pos, BlockState expectedState)
	{
		for (ISchematicPickBlockEventListener handler : this.handlers)
		{
			if (this.isProcessingCancelled())
			{
				handler.onSchematicPickBlockCancelled(this.getProcessingCanceledBy());
			}
			else
			{
				SchematicPickBlockEventResult result = handler.onSchematicPickBlockPreGather(schematicWorld, pos, expectedState);

				if (result == SchematicPickBlockEventResult.CANCEL)
				{
					Litematica.LOGGER.warn("SchematicPickBlockEventHandler: Processing cancelled by: {} during 'onSchematicPickBlockPreGather'", handler.getName().get());
					this.processingCancelled = true;
					this.processingCanceledBy = handler.getName();
				}
				else if (result == SchematicPickBlockEventResult.ERROR)
				{
					Litematica.LOGGER.error("SchematicPickBlockEventHandler: Error processing 'onSchematicPickBlockPreGather' from: {}", handler.getName().get());
				}
			}
		}

		return this.isProcessingCancelled();
	}

	@ApiStatus.Internal
	public boolean onSchematicPickBlockPrePick(World schematicWorld, BlockPos pos, BlockState expectedState, ItemStack stack)
	{
		for (ISchematicPickBlockEventListener handler : this.handlers)
		{
			if (this.isProcessingCancelled())
			{
				handler.onSchematicPickBlockCancelled(this.getProcessingCanceledBy());
			}
			else
			{
				SchematicPickBlockEventResult result = handler.onSchematicPickBlockPrePick(schematicWorld, pos, expectedState, stack);

				if (result == SchematicPickBlockEventResult.CANCEL)
				{
					Litematica.LOGGER.warn("SchematicPickBlockEventHandler: Processing cancelled by: {} during 'onSchematicPickBlockPrePick'", handler.getName().get());
					this.processingCancelled = true;
					this.processingCanceledBy = handler.getName();
				}
				else if (result == SchematicPickBlockEventResult.ERROR)
				{
					Litematica.LOGGER.error("SchematicPickBlockEventHandler: Error processing 'onSchematicPickBlockPrePick' from: {}", handler.getName().get());
				}
			}
		}

		return this.isProcessingCancelled();
	}

	@ApiStatus.Internal
	public void onSchematicPickBlockSuccess()
	{
		for (ISchematicPickBlockEventListener handler : this.handlers)
		{
			// This will possibly never be true here, but you never know.
			if (this.isProcessingCancelled())
			{
				handler.onSchematicPickBlockCancelled(this.getProcessingCanceledBy());
			}
			else
			{
				handler.onSchematicPickBlockSuccess();
			}
		}
	}
}

package fi.dy.masa.litematica.schematic.placement.thread;

import java.util.concurrent.atomic.AtomicBoolean;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.litematica.Litematica;

public class SchematicPlacementDaemonExecutor implements IThreadDaemonExecutor<SchematicPlacementManagerTask>
{
	private final AtomicBoolean running = new AtomicBoolean(true);

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	public void start()
	{
		this.running.set(true);
	}

	@Override
	public void stop()
	{
		this.running.set(false);
	}

	@Override
	public void run()
	{
		while (this.isRunning())
		{
			try
			{
				SchematicPlacementManagerTask task = SchematicPlacementDaemonHandler.INSTANCE.getNextTask();

				if (task != null)
				{
					this.processTask(task);
				}
			}
			catch (Exception err)
			{
				Litematica.LOGGER.error("SchematicPlacementTaskExecutor: Exception: {}", err.getLocalizedMessage());
				this.stop();
				return;
			}
		}
	}

	@Override
	public void processTask(SchematicPlacementManagerTask task)
	{
		task.run();
	}
}

package fi.dy.masa.litematica.schematic.placement;

import java.util.concurrent.atomic.AtomicBoolean;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.litematica.Litematica;

public class PlacementManagerDaemonExecutor implements IThreadDaemonExecutor<PlacementManagerTask>
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
			PlacementManagerTask task = PlacementManagerDaemonHandler.INSTANCE.getNextTask();

			if (task != null)
			{
				try
				{
					this.processTask(task);
				}
				catch (Exception err)
				{
					Litematica.LOGGER.error("PlacementManagerDaemonExecutor: Exception: {}", err.getLocalizedMessage());

					// Reschedule it
//					PlacementManagerDaemonHandler.INSTANCE.addTask(task);
				}
			}
		}
	}

	@Override
	public void processTask(PlacementManagerTask task)
	{
		task.run();
	}
}

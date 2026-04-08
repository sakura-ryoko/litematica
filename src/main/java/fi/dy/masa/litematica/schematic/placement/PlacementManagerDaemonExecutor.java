package fi.dy.masa.litematica.schematic.placement;

import java.util.concurrent.atomic.AtomicBoolean;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;

public class PlacementManagerDaemonExecutor implements IThreadDaemonExecutor<PlacementManagerTask>
{
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final AtomicBoolean paused = new AtomicBoolean(false);
	private final long sleepTime;
	private final float sleepDelay;
	private final long maxTicks;
	private long lastTaskTime;
	private long ticks;

	public PlacementManagerDaemonExecutor()
	{
		this(1200000L);  // 20 min
	}

	public PlacementManagerDaemonExecutor(long sleepTime)
	{
		this.sleepTime = MathUtils.clamp(sleepTime, 60000L, Long.MAX_VALUE); // 1 min
		this.sleepDelay = 0.75F;        // <1-second sleep delay (Must not be < 1/2 the tick rate)
		this.maxTicks = 128L;           // Cap how many ticks per an interrupt cycle without tasks to do
		this.ticks = 0L;
	}

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	public boolean isPaused()
	{
		return this.paused.get();
	}

	@Override
	public void start()
	{
		if (!this.isRunning())
		{
			Litematica.debugLogError("Executor: Starting");
			if (this.isPaused())
			{
				this.paused.set(false);
			}

			this.running.set(true);
		}

		this.run();
	}

	@Override
	public void interrupt(InterruptedException interrupt)
	{
		Litematica.debugLogError("Executor: Interrupt Signal: {}",
		                        interrupt.getLocalizedMessage() != null
		                        ? interrupt.getLocalizedMessage()  // This is null sometimes?
		                        : "received interrupt signal");
		if (this.isPaused() || !this.isRunning())
		{
			this.resume();
		}
	}

	@Override
	public void pause()
	{
		Litematica.debugLogError("Executor: Pausing");
		this.paused.set(true);
	}

	@Override
	public void resume()
	{
		if (this.isPaused())
		{
			Litematica.debugLogError("Executor: Resuming");
			this.paused.set(false);
		}

		this.start();
	}

	@Override
	public void stop()
	{
		Litematica.debugLogError("Executor: Stopping");
		if (!this.isPaused())
		{
			this.paused.set(true);
		}
		if (this.isRunning())
		{
			this.running.set(false);
		}
	}

	@Override
	public long sleepTime()
	{
		return this.sleepTime;
	}

	@Override
	public String getName()
	{
		return PlacementManagerDaemonHandler.INSTANCE.getName();
	}

	@Override
	public boolean hasTasks()
	{
		return PlacementManagerDaemonHandler.INSTANCE.hasTasks();
	}

	@Override
	public void run()
	{
		if (!this.isCorrectThread()) { return; }
		this.lastTaskTime = System.currentTimeMillis();
		this.ticks = 0L;
		Litematica.debugLogError("Executor: Running: [{}/{}]", this.isRunning(), this.isPaused());

		while (this.isRunning())
		{
			if (this.isPaused() && this.hasTasks())
			{
				this.resume();
			}
			else if (!this.isPaused() && this.loopSafe())
			{
				this.paused.set(true);
				this.sleep();
				return;
			}
		}
	}

	@Override
	public boolean loopSafe()
	{
		this.ticks++;

		try
		{
			PlacementManagerTask task = this.takeNextTask();

			if (task != null)
			{
				this.processTask(task);
				this.lastTaskTime = System.currentTimeMillis();
				return false;
			}
		}
		catch (InterruptedException e)
		{
			this.interrupt(e);
		}
		catch (Exception err)
		{
			Litematica.debugLogError("PlacementManagerDaemonExecutor#loopSafe: Exception: {}", err.getLocalizedMessage());
		}

		return this.shouldPause();
	}

	@Override
	public boolean shouldPause()
	{
		if (this.hasTasks()) { return false; }
		if (this.ticks > this.maxTicks) { return true; }
		return (System.currentTimeMillis() - this.lastTaskTime) > (this.sleepDelay * 1000L);
	}

	private PlacementManagerTask takeNextTask() throws InterruptedException
	{
		return PlacementManagerDaemonHandler.INSTANCE.getNextTask();
	}

	@Override
	public void processTask(PlacementManagerTask task) throws InterruptedException
	{
		task.run();
	}
}

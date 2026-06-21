package fi.dy.masa.litematica.schematic.placement;

import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.thread.ThreadExecutorPair;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class PlacementManagerDaemonHandler implements IThreadDaemonHandler<PlacementManagerTask>
{
	public static final PlacementManagerDaemonHandler INSTANCE = new PlacementManagerDaemonHandler();
	public static final int MIN_PLATFORM_THREADS = 2;                       // The hard min limit of usable Threads
	public static final int MAX_PLATFORM_THREADS = calculateMaxThreads();   // The hard max limit of usable Threads
	private static final float TASK_INTERVAL = 1.50F;                       // The amount of time in between task check updates
	private static final int MAX_DEFERRED_CAP = 850;                        // The approx amount of tasks that can be queued before they are deferred for each task interval
	private boolean useVirtual = false;
	private final String namePrefix = Reference.MOD_NAME+" Placement Manager";
	private int threadCount;
	private final ConcurrentHashMap<String, ThreadExecutorPair<PlacementManagerTask>> threadMap;
//	private final LinkedBlockingQueue<PlacementManagerTask> queueUnload = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> queueRebuild = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> queueOther = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> deferredQueue = new LinkedBlockingQueue<>();
	private final ReentrantLock lock = new ReentrantLock();
	private long lastTick;
	private boolean processing = false;
	private boolean forceStop = false;

	private static int calculateMaxThreads()
	{
		final int count = (Runtime.getRuntime().availableProcessors() / 4);
		final int result = MathUtils.max(count, MIN_PLATFORM_THREADS);
		Litematica.LOGGER.info("Placement Manager calculated thread limit: [{}]", String.format("%02d/%02d", MIN_PLATFORM_THREADS, result));
		return result;
	}

	private int calculateDefaultSafeThreadCount()
	{
		final int result = this.getThreadCountSafe();
		this.useVirtual = result < 1;
		return MathUtils.clamp(result, MIN_PLATFORM_THREADS, MAX_PLATFORM_THREADS);
	}

	private PlacementManagerDaemonHandler()
	{
		this.threadCount = MathUtils.max(MAX_PLATFORM_THREADS, MIN_PLATFORM_THREADS);
		this.threadMap = new ConcurrentHashMap<>(this.threadCount, 0.9f, 1);
//		this.buildThreadMap();      // Build the map later (After world join)
		this.lastTick = System.currentTimeMillis();
	}

	private synchronized void buildThreadMap()
	{
		// Only build when empty
		if (this.threadMap.isEmpty())
		{
			if (this.forceStop) { return; }
			this.lock.lock();

			try
			{
				final int count = this.getClampedThreadCount(this.threadCount);

				for (int i = 0; i < count; i++)
				{
					final String name = count > 1 ? this.namePrefix + " " + (i + 1) : this.namePrefix;
					this.threadMap.put(name, this.threadFactory(name, this.useVirtual, new PlacementManagerDaemonExecutor()));
				}
			}
			finally
			{
				this.lock.unlock();
			}
		}
	}

	@Override
	public String getName()
	{
		return this.namePrefix;
	}

	private int getDeferredCap()
	{
//		return MathUtils.clamp(this.getProfile().deferredCap(), 64, MAX_DEFERRED_CAP);
		return MAX_DEFERRED_CAP;
	}

	private int getClampedThreadCount(final int count)
	{
		return MathUtils.clamp(count, MIN_PLATFORM_THREADS, MAX_PLATFORM_THREADS);
	}

	private int getConfiguredThreadCount()
	{
		int count = Configs.Generic.PLACEMENT_MANAGER_THREAD_COUNT.getIntegerValue();

		if (count < MIN_PLATFORM_THREADS)
		{
			count = MathUtils.max(this.calculateDefaultSafeThreadCount(), MIN_PLATFORM_THREADS);
		}

		return this.getClampedThreadCount(count);
	}

	public void resetThreadCount(ConfigInteger config, boolean noBuild)
	{
		int count = this.getConfiguredThreadCount();
		final int lastCount = this.getClampedThreadCount(config.getLastIntegerValue());

		if (count != lastCount || this.threadCount != count)
		{
			this.stop();
			this.lock.lock();

			try
			{
				if (this.useVirtual || count < MIN_PLATFORM_THREADS)
				{
					count = MIN_PLATFORM_THREADS;
				}

//				Litematica.LOGGER.error("CPU Count: {} / Safe Count: {}", Runtime.getRuntime().availableProcessors(), this.calculateDefaultSafeThreadCount());
				Litematica.LOGGER.info("Resetting Placement Manager Thread count from config change [{} -> {}]", lastCount, count);

				synchronized (this.threadMap)
				{
					this.threadMap.clear();
				}
			}
			finally
			{
				this.threadCount = this.getClampedThreadCount(count);
				this.lock.unlock();
				this.gc();
			}

			if (!noBuild)
			{
				this.buildThreadMap();
				this.start();
			}
		}
	}

	public void checkThreadCount(boolean noBuild)
	{
//		Litematica.LOGGER.error("checkThreadCount(): count: {} // SAFE: {} // configured: {}", this.threadCount, this.calculateDefaultSafeThreadCount(), this.getConfiguredThreadCount());
		if (this.threadCount != this.getConfiguredThreadCount())
		{
			this.resetThreadCount(Configs.Generic.PLACEMENT_MANAGER_THREAD_COUNT, noBuild);
		}

		if (this.threadMap.isEmpty() && !noBuild)
		{
			this.gc();
			this.buildThreadMap();
		}
	}

	@Override
	public void start()
	{
		if (this.forceStop) { return; }
		// , this.getProfile().getDisplayName()
		Litematica.LOGGER.info("Starting [{}] Placement Manager Daemon threads", this.threadMap.size());
		Set<String> keys = this.threadMap.keySet();

		for (String key : keys)
		{
			ThreadExecutorPair<PlacementManagerTask> pair = this.threadMap.get(key);

			try
			{
				this.safeStart(pair);
			}
			catch (ConcurrentModificationException cme)
			{
				// Busy
			}
			catch (IllegalStateException is)
			{
				this.lock.lock();

				try
				{
					// Terminated
					pair = this.threadFactory(key, this.useVirtual, new PlacementManagerDaemonExecutor());
					pair.thread().start();

					synchronized (this.threadMap)
					{
						this.threadMap.replace(key, pair);
					}
				}
				finally
				{
					this.lock.unlock();
				}
			}
			catch (RuntimeException re)
			{
				// Already Running
			}
			catch (Exception ignored) {}
		}
	}

	@Override
	public void stop()
	{
		Litematica.LOGGER.info("Stopping [{}] Placement Manager Daemon threads", this.threadMap.size());
		Set<String> keys = this.threadMap.keySet();

		for (String key : keys)
		{
			ThreadExecutorPair<PlacementManagerTask> pair = this.threadMap.get(key);

			try
			{
				this.safeStop(pair);
			}
			catch (ConcurrentModificationException cme)
			{
				// Busy
				Litematica.LOGGER.warn("Thread [{}] is currently busy, and shouldn't be stopped", key);
			}
			catch (IllegalStateException is)
			{
				// Terminated already
			}
			catch (IllegalThreadStateException is)
			{
				// Never started
			}
			catch (Exception ignored) {}
		}
	}

	@Override
	public void reset()
	{
		this.clearAllTasks();
	}

	@Override
	public synchronized void addTask(PlacementManagerTask newTask)
	{
		if (this.checkIfTasksAreFull())
		{
			this.deferredQueue.offer(newTask);
			return;
		}

		final boolean empty = this.getTaskCount() == 0;

		switch (newTask)
		{
//			case PlacementManagerTaskUnload tU -> this.queueUnload.offer(newTask);
			case PlacementManagerTaskRebuild tL -> this.queueRebuild.offer(newTask);
			default -> this.queueOther.offer(newTask);
		}

		if (empty)
		{
			if (Reference.DEBUG_MODE)
			{
				Litematica.LOGGER.error("addTask: [EMPTY] Waking up threads...");
			}

			this.ensureThreadsAreAlive();
		}

		this.processing = true;
	}

	@Override
	public synchronized PlacementManagerTask getNextTask()
	{
//		if (!this.queueUnload.isEmpty())
//		{
//			return this.queueUnload.poll();
//		}

		if (!this.queueRebuild.isEmpty())
		{
			return this.queueRebuild.poll();
		}

		if (!this.queueOther.isEmpty())
		{
			return this.queueOther.poll();
		}

		return null;
	}

	protected synchronized int getTaskCount()
	{
		//  + this.queueUnload.size()
		return this.queueRebuild.size() + this.queueOther.size() + this.deferredQueue.size();
	}

	// Get any non-deferred tasks to be considered "active"
	protected synchronized boolean hasActiveTasks()
	{
		// !this.queueUnload.isEmpty() ||
		return !this.queueRebuild.isEmpty() || !this.queueOther.isEmpty();
	}

	@Override
	public synchronized boolean hasTasks()
	{
		// !this.queueUnload.isEmpty() ||
		return !this.queueRebuild.isEmpty() || !this.queueOther.isEmpty() || !this.deferredQueue.isEmpty();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(TASK_INTERVAL * 1000L);
	}

	private boolean checkIfTasksAreFull()
	{
		final int threadCount = this.threadMap.size();
		// this.queueUnload.size() +
		final int total = this.queueRebuild.size() + this.queueOther.size();
		final int calc = MathUtils.clamp((threadCount / 2), 1, threadCount) * this.getDeferredCap();
		return total >= calc && total > 0;
	}

	protected boolean allDone()
	{
		// this.queueUnload.isEmpty() &&
		if (this.queueRebuild.isEmpty() &&
			this.queueOther.isEmpty())
		{
			if (!this.deferredQueue.isEmpty())
			{
				this.fillDeferredTasks();
				return false;
			}

			return true;
		}

		return false;
	}

	private void fillDeferredTasks()
	{
		int cap = this.getDeferredCap();
		int total = 0;
		PlacementManagerTask task;

		while (total < cap && (task = this.deferredQueue.poll()) != null)
		{
			this.addTask(task);
			total++;
		}
	}

	@Override
	public void onClientTick(Minecraft mc)
	{
		if (this.forceStop) { return; }
		long now = System.currentTimeMillis();
		if (this.lastTick > now) this.lastTick = now;

		// Scheduled maintenance tasks
		if ((now - this.lastTick) > this.getTaskInterval())
		{
			if (mc.level != null)
			{
				if (this.processing && this.allDone())
				{
					Litematica.debugLog("PlacementManagerDaemonHandler:  All tasks complete");
//					DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
					LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();
					this.processing = false;
				}

				// Scheduled updates if we have tasks
				this.ensureThreadsAreAlive();
			}

			this.lastTick = now;
		}
	}

	private void ensureThreadsAreAlive()
	{
		if (this.forceStop) { return; }
		final int count = this.getTaskCount();

		if (count > 0)
		{
			this.checkThreadCount(false);
			Litematica.debugLog("PlacementManagerDaemonHandler: {} tasks detected --> checking Thread states", count);
			Set<String> keySet = this.threadMap.keySet();

			for (String key : keySet)
			{
				ThreadExecutorPair<PlacementManagerTask> pair = this.threadMap.get(key);

				try
				{
					this.safeStart(pair);
				}
				catch (IllegalStateException is)
				{
					this.lock.lock();

					try
					{
						// Terminated (Replace)
						pair = this.threadFactory(key, this.useVirtual, new PlacementManagerDaemonExecutor());
						pair.thread().start();

						synchronized (this.threadMap)
						{
							this.threadMap.replace(key, pair);
						}
					}
					finally
					{
						this.lock.unlock();
					}
				}
				catch (RuntimeException ignored) {}
			}
		}
	}

//	protected void removeUnloadTasksFor(int x, int z)
//	{
//		this.queueUnload.removeIf(task -> task.cx() == x && task.cz() == z);
//	}

	protected void removeRebuildTasksFor(int x, int z)
	{
		this.queueRebuild.removeIf(task -> task.cx() == x && task.cz() == z);
	}

	protected void removeOtherTasksFor(int x, int z)
	{
		this.queueOther.removeIf(task -> task.cx() == x && task.cz() == z);
	}

	protected void removeDeferredTasksFor(int x, int z)
	{
		this.deferredQueue.removeIf(task -> task.cx() == x && task.cz() == z);
	}

	public boolean hasAnyRebuildTasksFor(ChunkPos pos)
	{
		return this.hasAnyRebuildTasksFor(pos.x, pos.z);
	}

//	public synchronized boolean hasAnyUnloadTasksFor(int cx, int cz)
//	{
//		return this.queueUnload.stream().anyMatch(task -> (task.cx() == cx && task.cz() == cz));
//	}

	public synchronized boolean hasAnyRebuildTasksFor(int cx, int cz)
	{
		return this.queueRebuild.stream().anyMatch(task -> (task.cx() == cx && task.cz() == cz));
	}

	public synchronized boolean hasAnyOtherTasksFor(int cx, int cz)
	{
		return this.queueOther.stream().anyMatch(task -> (task.cx() == cx && task.cz() == cz));
	}

	public synchronized boolean hasAnyDeferredTasksFor(int cx, int cz)
	{
		return this.deferredQueue.stream().anyMatch(task -> (task.cx() == cx && task.cz() == cz));
	}

	public boolean hasAnyTasks()
	{
		// this.hasAnyUnloadTasks() ||
		return  this.hasAnyRebuildTasks() ||
				this.hasAnyOtherTasks()  || this.hasAnyDeferredTasks();
	}

//	public boolean hasAnyUnloadTasks()
//	{
//		return !this.queueUnload.isEmpty();
//	}

	public boolean hasAnyRebuildTasks()
	{
		return !this.queueRebuild.isEmpty();
	}

	public boolean hasAnyOtherTasks()
	{
		return !this.queueOther.isEmpty();
	}

	public boolean hasAnyDeferredTasks()
	{
		return !this.deferredQueue.isEmpty();
	}

	public boolean hasAnyTasksFor(int cx, int cz)
	{
		// this.hasAnyUnloadTasksFor(cx, cz) ||
		return
				this.hasAnyRebuildTasksFor(cx, cz) ||
				this.hasAnyOtherTasksFor(cx, cz) ||
				this.hasAnyDeferredTasksFor(cx, cz);
	}

	protected void removeAllTasksFor(int cx, int cz)
	{
		this.removeOtherTasksFor(cx, cz);
		this.removeRebuildTasksFor(cx, cz);
//		this.removeUnloadTasksFor(cx, cz);
		this.removeDeferredTasksFor(cx, cz);
	}

//	protected void removeAllUnloadTasks()
//	{
//		synchronized (this.queueUnload)
//		{
//			this.queueUnload.clear();
//		}
//	}

	protected void removeAllRebuildTasks()
	{
		synchronized (this.queueRebuild)
		{
			this.queueRebuild.clear();
		}
	}

	protected void removeAllOtherTasks()
	{
		synchronized (this.queueOther)
		{
			this.queueOther.clear();
		}
	}

	protected void removeAllDeferredTasks()
	{
		synchronized (this.deferredQueue)
		{
			this.deferredQueue.clear();
		}
	}

	public String getDebugString()
	{
		// UL: %02d
		return String.format("T: %02d RB: %03d O: %02d D: %02d",
		                     this.threadMap.size(),
		                     this.queueRebuild.size(),
//		                     this.queueUnload.size(),
		                     this.queueOther.size(),
		                     this.deferredQueue.size()
		);
	}

	public void clearAllTasks()
	{
//		this.removeAllUnloadTasks();
		this.removeAllRebuildTasks();
		this.removeAllOtherTasks();
		this.removeAllDeferredTasks();
		this.processing = false;
	}

	@Override
	public void resetForceStop()
	{
		this.forceStop = false;
	}

	@Override
	public boolean isForceStop()
	{
		return this.forceStop;
	}

	@Override
	public void endAll()
	{
		this.forceStop = true;
		this.reset();
		this.stop();
		this.lock.lock();

		try
		{
			// Give the threads 50 ms to clean up.
			Thread.sleep(50L);

			synchronized (this.threadMap)
			{
				this.threadMap.clear();
			}
		}
		catch (InterruptedException e)
		{
			// NO-OP
		}
		finally
		{
			this.lock.unlock();
			this.gc();
		}
	}

	@Override
	public void close() throws Exception
	{
		this.endAll();
	}
}

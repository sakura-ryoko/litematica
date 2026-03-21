package fi.dy.masa.litematica.schematic.placement;

import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class PlacementManagerDaemonHandler implements IThreadDaemonHandler<PlacementManagerTask>
{
	public static final PlacementManagerDaemonHandler INSTANCE = new PlacementManagerDaemonHandler();
	private static final int MAX_PLATFORM_THREADS = 2;
	private boolean useVirtual = false;
	private final String namePrefix = Reference.MOD_NAME+" Placement Manager";
	private static final float TASK_INTERVAL = 1.50F;
	private final int threadCount = this.calculateMaxThreads();
	private final ConcurrentHashMap<String, Thread> threadMap = this.builder();
	private final LinkedBlockingQueue<PlacementManagerTask> queueUnload = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> queueRebuild = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> queueOther = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<PlacementManagerTask> deferredQueue = new LinkedBlockingQueue<>();
	private long lastTick;
	private boolean processing = false;

	private int calculateMaxThreads()
	{
		final int result = this.getThreadCountSafe();
		if (result < 1) { this.useVirtual = true; }

		return MathUtils.clamp(result, 1, MAX_PLATFORM_THREADS);
	}

	private ConcurrentHashMap<String, Thread> builder()
	{
		ConcurrentHashMap<String, Thread> threads = new ConcurrentHashMap<>(this.threadCount, 0.9f, 1);

		for (int i = 0; i < this.threadCount; i++)
		{
			final String name = this.threadCount > 1 ? this.namePrefix+" "+ (i+1) : this.namePrefix;
			threads.put(name, this.threadFactory(name, this.useVirtual, new PlacementManagerDaemonExecutor()));
		}

		return threads;
	}

	private PlacementManagerDaemonHandler()
	{
		this.lastTick = System.currentTimeMillis();
	}

	@Override
	public String getName()
	{
		return this.namePrefix;
	}

	@Override
	public void start()
	{
		Litematica.LOGGER.info("Starting [{}] Placement Manager Daemon threads", this.threadMap.size());
		Set<String> keys = this.threadMap.keySet();

		for (String key : keys)
		{
			try
			{
				this.safeStart(this.threadMap.get(key));
			}
			catch (ConcurrentModificationException cme)
			{
				// Busy
			}
			catch (IllegalStateException is)
			{
				// Terminated
				Thread entry = this.threadFactory(key, this.useVirtual, new PlacementManagerDaemonExecutor());
				entry.start();

				synchronized (this.threadMap)
				{
					this.threadMap.replace(key, entry);
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
			try
			{
				this.safeStop(this.threadMap.get(key));
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

		final boolean wasEmpty = !this.hasTasks();

		switch (newTask)
		{
			case PlacementManagerTaskUnload tU -> this.queueUnload.offer(newTask);
			case PlacementManagerTaskRebuild tL -> this.queueRebuild.offer(newTask);
			default -> this.queueOther.offer(newTask);
		}

		if (wasEmpty)
		{
			this.ensureThreadsAreAlive();
		}

		this.processing = true;
	}

	@Override
	public synchronized PlacementManagerTask getNextTask()
	{
		if (!this.queueUnload.isEmpty())
		{
			return this.queueUnload.poll();
		}

		if (!this.queueRebuild.isEmpty())
		{
			return this.queueRebuild.poll();
		}

		if (!this.queueOther.isEmpty())
		{
			return this.queueOther.poll();
		}

//		if (!this.deferredQueue.isEmpty())
//		{
//			this.fillDeferredTasks();
//		}

		return null;
	}

	protected int getTaskCount()
	{
		return this.queueRebuild.size() + this.queueUnload.size() + this.queueOther.size() + this.deferredQueue.size();
	}

	@Override
	public boolean hasTasks()
	{
		return !this.queueUnload.isEmpty() || !this.queueRebuild.isEmpty() || !this.queueOther.isEmpty() || !this.deferredQueue.isEmpty();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(TASK_INTERVAL * 1000L);
	}

	private synchronized boolean checkIfTasksAreFull()
	{
		final int threadCount = this.threadMap.size();
		final int total = this.queueUnload.size() + this.queueRebuild.size() + this.queueOther.size();
		final int calc = MathUtils.clamp((threadCount / 2), 1, threadCount) * 650;
		return total >= calc && total > 0;
	}

	protected boolean allDone()
	{
		if (this.queueUnload.isEmpty() &&
			this.queueRebuild.isEmpty() &&
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

	private synchronized void fillDeferredTasks()
	{
		CopyOnWriteArrayList<PlacementManagerTask> tasks = new CopyOnWriteArrayList<>(this.deferredQueue);

		synchronized (this.deferredQueue)
		{
			this.deferredQueue.clear();
		}

		tasks.forEach(this::addTask);
		tasks.clear();
	}

	@Override
	public void onClientTick(Minecraft mc)
	{
		long now = System.currentTimeMillis();
		if (this.lastTick > now) this.lastTick = now;

		// Scheduled maintenance tasks
		if ((now - this.lastTick) > this.getTaskInterval())
		{
			if (mc.level != null)
			{
				if (this.processing && this.allDone())
				{
//					Litematica.LOGGER.warn("PlacementManagerDaemonHandler:  All tasks complete");
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
		if (this.hasTasks())
		{
			Set<String> keySet = this.threadMap.keySet();

			for (String key : keySet)
			{
				try
				{
					this.safeStart(this.threadMap.get(key));
				}
				catch (IllegalStateException is)
				{
					// Terminated (Replace)
					Thread entry = this.threadFactory(key, this.useVirtual, new PlacementManagerDaemonExecutor());
					entry.start();

					synchronized (this.threadMap)
					{
						this.threadMap.replace(key, entry);
					}
				}
				catch (RuntimeException ignored) {}
			}
		}
	}

//	@Override
//	public void safeStart(Thread t) throws RuntimeException
//	{
//		if (t == null) { throw new RuntimeException(); }
//		Litematica.debugLogError("PlacementManagerDaemonHandler#safeStart: '{}' [State: {}]", t.getName(), t.getState().name());
//
//		switch (t.getState())
//		{
//			case NEW -> t.start();
//			case TIMED_WAITING, WAITING -> t.interrupt();
//			case RUNNABLE -> throw new RuntimeException();
//			case BLOCKED -> throw new ConcurrentModificationException();
//			case TERMINATED -> throw new IllegalStateException();
//		}
//	}

	protected void removeUnloadTasksFor(int x, int z)
	{
		synchronized (this.queueUnload)
		{
			Queue<PlacementManagerTask> newQueue = new ConcurrentLinkedQueue<>(this.queueUnload);

			this.queueUnload.clear();
			this.queueUnload.addAll(newQueue.stream()
			                                .filter(task -> !(task.cx() == x && task.cz() == z))
			                                .toList());
		}
	}

	protected void removeRebuildTasksFor(int x, int z)
	{
		synchronized (this.queueUnload)
		{
			Queue<PlacementManagerTask> newQueue = new ConcurrentLinkedQueue<>(this.queueRebuild);

			this.queueRebuild.clear();
			this.queueRebuild.addAll(newQueue.stream()
			                                 .filter(task -> !(task.cx() == x && task.cz() == z))
			                                 .toList());
		}
	}

	protected void removeOtherTasksFor(int x, int z)
	{
		synchronized (this.queueOther)
		{
			Queue<PlacementManagerTask> newQueue = new ConcurrentLinkedQueue<>(this.queueOther);

			this.queueOther.clear();
			this.queueOther.addAll(newQueue.stream()
			                               .filter(task -> !(task.cx() == x && task.cz() == z))
			                               .toList());
		}
	}

	protected void removeDeferredTasksFor(int x, int z)
	{
		synchronized (this.deferredQueue)
		{
			Queue<PlacementManagerTask> newQueue = new ConcurrentLinkedQueue<>(this.deferredQueue);

			this.deferredQueue.clear();
			this.deferredQueue.addAll(newQueue.stream()
			                                  .filter(task -> !(task.cx() == x && task.cz() == z))
			                                  .toList());
		}
	}

	public boolean hasAnyRebuildTasksFor(ChunkPos pos)
	{
		return this.hasAnyRebuildTasksFor(pos.x(), pos.z());
	}

	public synchronized boolean hasAnyUnloadTasksFor(int cx, int cz)
	{
		return !this.queueUnload.stream().filter(task -> (task.cx() == cx && task.cz() == cz)).toList().isEmpty();
	}

	public synchronized boolean hasAnyRebuildTasksFor(int cx, int cz)
	{
		return !this.queueRebuild.stream().filter(task -> (task.cx() == cx && task.cz() == cz)).toList().isEmpty();
	}

	public synchronized boolean hasAnyOtherTasksFor(int cx, int cz)
	{
		return !this.queueOther.stream().filter(task -> (task.cx() == cx && task.cz() == cz)).toList().isEmpty();
	}

	public synchronized boolean hasAnyDeferredTasksFor(int cx, int cz)
	{
		return !this.deferredQueue.stream().filter(task -> (task.cx() == cx && task.cz() == cz)).toList().isEmpty();
	}

	public boolean hasAnyTasks()
	{
		return  this.hasAnyUnloadTasks() || this.hasAnyRebuildTasks() ||
				this.hasAnyOtherTasks()  || this.hasAnyDeferredTasks();
	}

	public boolean hasAnyUnloadTasks()
	{
		return !this.queueUnload.isEmpty();
	}

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
		return  this.hasAnyUnloadTasksFor(cx, cz) ||
				this.hasAnyRebuildTasksFor(cx, cz) ||
				this.hasAnyOtherTasksFor(cx, cz) ||
				this.hasAnyDeferredTasksFor(cx, cz);
	}

	protected void removeAllTasksFor(int cx, int cz)
	{
		this.removeOtherTasksFor(cx, cz);
		this.removeRebuildTasksFor(cx, cz);
		this.removeUnloadTasksFor(cx, cz);
		this.removeDeferredTasksFor(cx, cz);
	}

	protected void removeAllUnloadTasks()
	{
		synchronized (this.queueUnload)
		{
			this.queueUnload.clear();
		}
	}

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
		return String.format("T: %02d RB: %03d UL: %02d O: %02d D: %02d",
		                     this.threadMap.size(),
		                     this.queueRebuild.size(),
		                     this.queueUnload.size(),
		                     this.queueOther.size(),
		                     this.deferredQueue.size()
		);
	}

	public void clearAllTasks()
	{
		this.removeAllUnloadTasks();
		this.removeAllRebuildTasks();
		this.removeAllOtherTasks();
		this.removeAllDeferredTasks();
		this.processing = false;
	}

	@Override
	public void close() throws Exception
	{
		this.endAll();
	}
}

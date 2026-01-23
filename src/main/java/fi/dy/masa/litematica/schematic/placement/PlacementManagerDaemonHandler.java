package fi.dy.masa.litematica.schematic.placement;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class PlacementManagerDaemonHandler implements IThreadDaemonHandler<PlacementManagerTask>
{
	private final int threadCount = this.calculateMaxThreads();
	private final ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threadMap = this.builder();
	public static final PlacementManagerDaemonHandler INSTANCE = new PlacementManagerDaemonHandler();

	private final ConcurrentLinkedQueue<PlacementManagerTask> queueUnload = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PlacementManagerTask> queueRebuild = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PlacementManagerTask> queueOther = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PlacementManagerTask> deferredQueue = new ConcurrentLinkedQueue<>();

	private static final float taskInterval = 0.75f;
	private long lastTick;
	private boolean processing = false;

	private int calculateMaxThreads()
	{
		// Don't use more than 1 / 4 of possible Platform threads for this; or MAX_PLATFORM_THREADS.
		return Math.clamp((Runtime.getRuntime().availableProcessors() / 4), 1, Reference.MAX_PLATFORM_THREADS);
	}

	private ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> builder()
	{
		ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threads = new ConcurrentHashMap<>();
		String prefix = Reference.MOD_NAME+" Placement Manager ";

		for (int i = 0; i < this.threadCount; i++)
		{
			String name = prefix + (i+1);
			ThreadFactory FACTORY = Thread.ofPlatform().name(name).daemon(true).factory();
			PlacementManagerDaemonExecutor executor = new PlacementManagerDaemonExecutor();

			threads.put(name, Pair.of(FACTORY.newThread(executor), executor));
		}

		return threads;
	}

	private PlacementManagerDaemonHandler()
	{
		this.lastTick = System.currentTimeMillis();
		this.start();
	}

	@Override
	public void start()
	{
		Litematica.LOGGER.info("Starting [{}] Placement Manager Daemon threads", this.threadMap.size());

		synchronized (this.threadMap)
		{
			this.threadMap.forEach(
					(name, pair) ->
					{
						pair.getLeft().start();
						pair.getRight().start();
					}
			);
		}
	}

	@Override
	public void stop()
	{
		Litematica.debugLog("Stopping [{}] Placement Manager Daemon threads", this.threadMap.size());

		synchronized (this.threadMap)
		{
			this.threadMap.forEach(
					(name, pair) ->
					{
						pair.getRight().stop();
						pair.getLeft().interrupt();
					}
			);
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
			this.deferredQueue.add(newTask);
			return;
		}

		switch (newTask)
		{
			case PlacementManagerTaskUnload tU -> this.queueUnload.offer(newTask);
			case PlacementManagerTaskRebuild tL -> this.queueRebuild.offer(newTask);
			default -> this.queueOther.offer(newTask);
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

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(taskInterval * 1000L);
	}

	private synchronized boolean checkIfTasksAreFull()
	{
		return (this.queueUnload.size() + this.queueRebuild.size() + this.queueOther.size())
				>= ((this.threadMap.size() / 3) * 1000);
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
			this.ensureThreadSafety();

			if (this.processing && this.allDone())
			{
//				Litematica.LOGGER.warn("PlacementManagerDaemonHandler:  All tasks complete");
//				DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
				LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();
				this.processing = false;
			}

			// Scheduled updates
			this.lastTick = now;
		}
	}

	// TODO -- is this even necessary?
	private void ensureThreadSafety()
			throws RuntimeException
	{
		this.threadMap.forEach(
				(name, pair) ->
				{
					if (!pair.getLeft().isAlive() || pair.getLeft().isInterrupted())
					{
						String err = String.format("'%s' was killed [%s]", name, this.getThreadStatus(pair.getLeft()));
						this.clearAllTasks();
						this.stop();

						TemporaryWorldManager.INSTANCE.reset();
						EntitiesDataStorage.getInstance().reset(true);
						Configs.saveToFile();
						DataManager.save(true);
						DataManager.getInstance().reset(true);
						DataManager.clear();
						Litematica.LOGGER.fatal(err);

						throw new RuntimeException(err);
					}
				}
		);
	}

	private String getThreadStatus(Thread thread)
	{
		if (thread == null)
		{
			return "<>";
		}

		return "(" + thread.threadId() + ')'
				+ "/"
				+ thread.getState().name();
	}

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
		return this.hasAnyRebuildTasksFor(pos.x, pos.z);
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

	public void endAll()
	{
		this.clearAllTasks();
		this.stop();
	}

	@Override
	public void close() throws Exception
	{
		this.endAll();
	}
}

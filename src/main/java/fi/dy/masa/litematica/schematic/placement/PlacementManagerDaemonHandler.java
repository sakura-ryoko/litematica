package fi.dy.masa.litematica.schematic.placement;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class PlacementManagerDaemonHandler implements IThreadDaemonHandler<PlacementManagerTask>
{
	private final ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threadMap = this.builder();
	public static final PlacementManagerDaemonHandler INSTANCE = new PlacementManagerDaemonHandler();

	private final ConcurrentLinkedQueue<PlacementManagerTask> queueUnload = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PlacementManagerTask> queueRebuild = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PlacementManagerTask> queueOther = new ConcurrentLinkedQueue<>();

	private static final int MAX_THREADS = 1;      // Please do not increase this value beyond 2 - 4
	private static final float taskInterval = 0.75f;
	private long lastTick;
	private boolean processing = false;

	private ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> builder()
	{
		ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threads = new ConcurrentHashMap<>();
		String prefix = Reference.MOD_NAME+" Placement Manager ";

		for (int i = 0; i < MAX_THREADS; i++)
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
		this.updateAll();
		this.stop();
		this.start();
	}

//	protected void scheduleFullRebuild(Supplier<WorldSchematic> supplier, int cx, int cz)
//	{
//		this.addTask(new PlacementManagerTaskUnload(supplier, cx, cz));
//		this.addTask(new PlacementManagerTaskLoad(supplier, cx, cz));
//		this.addTask(new PlacementManagerTaskFillChunk(supplier, cx, cz));
//		this.addTask(new PlacementManagerTaskNeedsUpdate(supplier, cx, cz));
//	}

	@Override
	public synchronized void addTask(PlacementManagerTask newTask)
	{
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

		return this.queueOther.poll();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(taskInterval * 1000L);
	}

	protected boolean allDone()
	{
		if (this.queueUnload.isEmpty() &&
			this.queueRebuild.isEmpty())
		{
			return this.queueOther.isEmpty();
		}

		return false;
	}

	@Override
	public void onClientTick(Minecraft mc)
	{
		long now = System.currentTimeMillis();
		if (this.lastTick > now) this.lastTick = now;

		// Scheduled maintenance tasks
		if ((now - this.lastTick) > this.getTaskInterval())
		{
			this.threadMap.forEach(
					(name, pair) ->
					{
						if (!pair.getLeft().isAlive())
						{
							pair.getLeft().interrupt();
						}
					}
			);

			if (this.processing && this.allDone())
			{
				Litematica.LOGGER.warn("PlacementManagerDaemonHandler:  All tasks complete");
//				DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
				LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();
				this.processing = false;
			}

			// Scheduled updates
			this.lastTick = now;
		}
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

	public boolean hasAnyTasksFor(int cx, int cz)
	{
		return  this.hasAnyUnloadTasksFor(cx, cz) ||
				this.hasAnyRebuildTasksFor(cx, cz) ||
				this.hasAnyOtherTasksFor(cx, cz);
	}

	protected void removeAllTasksFor(int cx, int cz)
	{
		this.removeOtherTasksFor(cx, cz);
		this.removeRebuildTasksFor(cx, cz);
		this.removeUnloadTasksFor(cx, cz);
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

	public String getDebugString()
	{
		return String.format("T: %02d RB: %04d UL: %02d O: %02d",
		                     this.threadMap.size(),
		                     this.queueRebuild.size(),
		                     this.queueUnload.size(),
		                     this.queueOther.size()
		);
	}

	public void updateAll()
	{
		this.removeAllUnloadTasks();
		this.removeAllRebuildTasks();
		this.removeAllOtherTasks();
		this.processing = false;
	}

	@Override
	public void close() throws Exception
	{
		this.updateAll();
		this.stop();
	}
}

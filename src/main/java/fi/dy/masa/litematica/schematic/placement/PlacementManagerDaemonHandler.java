package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class PlacementManagerDaemonHandler implements IThreadDaemonHandler<PlacementManagerTask>
{
//	private static final ThreadFactory THREAD_FACTORY_1 = Thread.ofPlatform().name(Reference.MOD_NAME+" Placement Manager 1").daemon(true).factory();
	private final ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threadMap = this.builder();
	public static final PlacementManagerDaemonHandler INSTANCE = new PlacementManagerDaemonHandler();

	private final ConcurrentLinkedQueue<PlacementManagerTask> queue = new ConcurrentLinkedQueue<>();
	private final int MAX_THREADS = 2;
	private final float taskInterval = 0.50f;
	private long lastTick;
	private boolean processing = false;

	private ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> builder()
	{
		ConcurrentHashMap<String, Pair<Thread, PlacementManagerDaemonExecutor>> threads = new ConcurrentHashMap<>();
		String prefix = Reference.MOD_NAME+" Placement Manager ";

		for (int i = 0; i < MAX_THREADS; i++)
		{
			String name = prefix + i;
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
		synchronized (this.queue)
		{
			this.queue.clear();
		}

		this.stop();
		this.start();
	}

	@Override
	public synchronized void addTask(PlacementManagerTask newTask)
	{
		this.queue.offer(newTask);
		this.processing = true;
	}

	@Override
	public synchronized PlacementManagerTask getNextTask()
	{
		return this.queue.poll();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(this.taskInterval * 1000L);
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

			if (this.processing && this.queue.isEmpty())
			{
				Litematica.LOGGER.error("PlacementManagerDaemonHandler:  All tasks complete");
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

		StringBuilder sb = new StringBuilder();

		sb.append('(').append(thread.threadId()).append(')');

		sb.append("/");
		sb.append(thread.getState().name());

		return sb.toString();
	}

	public String getDebugString()
	{
		return String.format("Q: %04d T: %02d", this.queue.size(), this.threadMap.size());
	}

	protected synchronized void removeTasksMatching(int cx, int cz)
	{
		List<PlacementManagerTask> toRemove = new ArrayList<>();

		try
		{
			this.queue.forEach(t ->
		                   {
							   if (t.cx() == cx && t.cz() == cz)
							   {
								   toRemove.add(t);
							   }
		                   });

			synchronized (this.queue)
			{
				toRemove.forEach(this.queue::remove);
			}
		}
		catch(Exception ignored) {}
	}

	public void updateAll()
	{
		synchronized (this.queue)
		{
			this.queue.clear();
		}
		this.processing = false;
	}

	@Override
	public void close() throws Exception
	{
		synchronized (this.queue)
		{
			this.queue.clear();
		}
		this.stop();
	}
}

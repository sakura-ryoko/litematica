package fi.dy.masa.litematica.schematic.placement.thread;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

public class SchematicPlacementDaemonHandler implements IThreadDaemonHandler<SchematicPlacementManagerTask>
{
//	 		Thread.ofPlatform().name(Reference.MOD_NAME+" Placement Manager").daemon(true).factory();
//			Thread.ofVirtual() .name(Reference.MOD_NAME+" Placement Manager").factory();
	private static final ThreadFactory THREAD_FACTORY = Thread.ofPlatform().name(Reference.MOD_NAME+" Placement Manager").daemon(true).factory();
	public static final SchematicPlacementDaemonHandler INSTANCE = new SchematicPlacementDaemonHandler();
	private final Thread thread;
	private final SchematicPlacementDaemonExecutor threadExecutor;

	private final Queue<SchematicPlacementManagerTask> queue = new LinkedBlockingQueue<>();
	private final float taskInterval = 50.0f;
	private long lastTick;

	private SchematicPlacementDaemonHandler()
	{
		this.lastTick = System.currentTimeMillis();
		this.threadExecutor = new SchematicPlacementDaemonExecutor();
		this.thread = THREAD_FACTORY.newThread(this.threadExecutor);
		this.start();
	}

	@Override
	public void start()
	{
		this.thread.start();
	}

	@Override
	public void stop()
	{
		this.threadExecutor.stop();
		this.thread.interrupt();
	}

	@Override
	public void reset()
	{
		this.queue.clear();
		this.stop();
		this.start();

	}

	@Override
	public void addTask(SchematicPlacementManagerTask newTask)
	{
		this.queue.offer(newTask);
	}

	@Override
	public SchematicPlacementManagerTask getNextTask()
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
			if (!this.thread.isAlive())
			{
				this.thread.interrupt();
			}

//			ChunkPos pos = new ChunkPos(0, 0);
//			this.addTask(new SchematicPlacementManagerTask(pos, pos.toLong(),
//			                                               () -> Litematica.LOGGER.warn("Ticking Schematic Placement Manager as a Task")
//			));

			// Scheduled updates
			this.lastTick = now;
		}
	}

	@Override
	public void close() throws Exception
	{
		this.queue.clear();
		this.stop();
	}
}

package fi.dy.masa.litematica.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.RenderPhase;

public class InfoHudSync implements IInfoHudRenderer
{
	private final List<AbstractInfoHudSync<?>> list;
	@Nullable private final ICompletionListener completionListener;
	private boolean completed;

	public InfoHudSync(@Nullable ICompletionListener completionListener)
	{
		this.list = new ArrayList<>();
		this.completionListener = completionListener;
		InfoHud.getInstance().addInfoHudRenderer(this, true);
	}

	public void addInfo(AbstractInfoHudSync<?> info)
	{
		this.list.add(info);
	}

	public boolean isEmpty()
	{
		return this.list.isEmpty();
	}

	public int size()
	{
		return this.list.size();
	}

	public void clearInfo()
	{
		this.list.forEach(AbstractInfoHudSync::clearInfo);
		this.list.clear();
	}

	private void notifyListener()
	{
		if (this.completionListener != null)
		{
			Minecraft.getInstance().execute(() ->
			                                {
				                                if (this.completed)
				                                {
					                                this.completionListener.onTaskCompleted();
				                                }
				                                else
				                                {
					                                this.completionListener.onTaskAborted();
				                                }
			                                });
		}
	}

	@Override
	public boolean getShouldRenderText(RenderPhase phase)
	{
		return phase == RenderPhase.POST;
	}

	@Override
	public List<String> getText(RenderPhase phase)
	{
		List<String> list = new ArrayList<>();

		this.list.forEach(info -> list.addAll(info.getInfo()));

		return list;
	}

	public void onReceiveInfoSync(CompoundData data)
	{
		final boolean complete = data.getBooleanOrDefault("InfoHudComplete", false);
		this.completed = complete;

		if (complete)
		{
			this.clearInfo();
			this.onStop();
		}

		this.clearInfo();
		ListData list = data.getList("InfoHudSync");

		if (!list.isEmpty())
		{
			for (int i = 0; i < list.size(); i++)
			{
				CompoundData entry = list.getCompoundAt(i);

				if (!entry.isEmpty())
				{
					InfoHudSyncType type = InfoHudSyncType.valueOf(entry.getStringOrDefault("Type", ""));

					if (type != null)
					{
						AbstractInfoHudSync<?> info = type.newInstance();

						if (info != null)
						{
							info.receiveInfo(entry);
							this.addInfo(info);
						}
					}
				}
			}
		}
	}

	public void onStop()
	{
		this.notifyListener();
		InfoHud.getInstance().removeInfoHudRenderer(this, false);
		this.completed = true;
	}

	public boolean isComplete()
	{
		return this.completed;
	}
}

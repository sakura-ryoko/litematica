package fi.dy.masa.litematica.scheduler.info_hud;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.util.data.tag.CompoundData;

public abstract class AbstractInfoHudSync<T>
{
	private final InfoHudSyncType type;

	public AbstractInfoHudSync(InfoHudSyncType type)
	{
		this.type = type;
	}

	public InfoHudSyncType type()
	{
		return this.type;
	}

	public abstract void addInfo(T info);

	public abstract boolean hasInfo();

	public abstract void clearInfo();

	public abstract void receiveInfo(CompoundData data);

	public abstract ImmutableList<String> getInfo();
}

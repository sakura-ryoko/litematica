package fi.dy.masa.litematica.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.StringData;

/**
 * @deprecated Probably was not the best way to do this
 */
@Deprecated
public class InfoHudSyncString extends AbstractInfoHudSync<String>
{
	private final List<String> infoHudFeedback;

	public InfoHudSyncString()
	{
		super(InfoHudSyncType.STRING);
		this.infoHudFeedback = new ArrayList<>();
	}

	@Override
	public void addInfo(String info)
	{
		this.infoHudFeedback.add(info);
	}

	@Override
	public boolean hasInfo()
	{
		return !this.infoHudFeedback.isEmpty();
	}

	@Override
	public void clearInfo()
	{
		this.infoHudFeedback.clear();
	}

	@Override
	public void receiveInfo(CompoundData data)
	{
		final InfoHudSyncType type = InfoHudSyncType.fromString(data.getStringOrDefault("Type", ""));

		if (type != null && type == InfoHudSyncType.STRING)
		{
			ListData list = data.getList("Data");

			if (list != null && !list.isEmpty())
			{
				if (this.hasInfo())
				{
					this.clearInfo();
				}

				for (int i = 0; i < list.size(); i++)
				{
					BaseData entry = list.get(i);

					if (entry.getType() == Constants.NBT.TAG_STRING && entry instanceof StringData sd)
					{
						this.addInfo(sd.getString());
					}
				}
			}
		}
	}

	@Override
	public ImmutableList<String> getInfo()
	{
		return ImmutableList.copyOf(this.infoHudFeedback);
	}
}

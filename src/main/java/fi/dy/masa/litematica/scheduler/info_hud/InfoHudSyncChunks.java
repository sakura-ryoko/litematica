package fi.dy.masa.litematica.scheduler.info_hud;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.litematica.config.Configs;

public class InfoHudSyncChunks extends AbstractInfoHudSync<InfoHudSyncChunks.Entry>
{
	private final List<Entry> infoHudFeedback;

	public InfoHudSyncChunks()
	{
		super(InfoHudSyncType.REMAINING_CHUNKS);
		this.infoHudFeedback = new ArrayList<>();
	}

	@Override
	public void addInfo(Entry info)
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

		if (type != null && type == InfoHudSyncType.REMAINING_CHUNKS)
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
					CompoundData entry = list.getCompoundAt(i);

					if (entry != null && !entry.isEmpty())
					{
						this.addInfo(Entry.fromData(entry));
					}
				}
			}
		}
	}

	@Override
	public ImmutableList<String> getInfo()
	{
		Entry info = this.infoHudFeedback.getFirst();
		ImmutableList.Builder<String> builder = ImmutableList.builder();

		if (info != null)
		{
			String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
			String title = StringUtils.translate("litematica.gui.label.task.title.remaining_chunks", "Servux: '§d"+info.n()+"§f'", info.rc());
			final int maxLines =  Math.min(Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue(), this.infoHudFeedback.size());
			builder.add(String.format("%s%s%s", pre, title, GuiBase.TXT_RST));

			for (int i = 0; i < maxLines; i++)
			{
				info = this.infoHudFeedback.get(i);

				if (info != null && info.rc() > 0 && info.cx() != -1 && info.cz() != -1)
				{
					builder.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", info.cx(), info.cz(), info.cx() << 4, info.cz() << 4));
				}
			}

			return builder.build();
		}

		return ImmutableList.of();
	}

	public record Entry(String n, int rc, int cx, int cz)
	{
		public static Entry fromData(CompoundData data)
		{
			final String n = data.getStringOrDefault("n", "");
			final int rc = data.getIntOrDefault("rc", -1);
			final int cx = data.getIntOrDefault("cx", -1);
			final int cz = data.getIntOrDefault("cz", -1);

			return new Entry(n, rc, cx, cz);
		}
	}
}

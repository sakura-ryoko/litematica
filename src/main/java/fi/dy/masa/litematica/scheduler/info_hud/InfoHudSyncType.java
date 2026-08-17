package fi.dy.masa.litematica.scheduler.info_hud;

import javax.annotation.Nullable;

import fi.dy.masa.litematica.Litematica;

public enum InfoHudSyncType
{
	REMAINING_CHUNKS    ("remaining_chunks",    InfoHudSyncChunks.class),
	STRING              ("string",              InfoHudSyncString.class),
	;

	private final String name;
	private final Class<? extends AbstractInfoHudSync<?>> info;

	InfoHudSyncType(final String name, final Class<? extends AbstractInfoHudSync<?>> info)
	{
		this.name = name;
		this.info = info;
	}

	public String getName()
	{
		return this.name;
	}

	@Nullable
	public AbstractInfoHudSync<?> newInstance()
	{
		try
		{
			return this.info.getDeclaredConstructor().newInstance();
		}
		catch (Exception e)
		{
			Litematica.LOGGER.error("InfoHudSyncType#getInfoHudSync(): Exception initializing type; {}", e.getLocalizedMessage());
		}

		return null;
	}

	@Nullable
	public static InfoHudSyncType fromString(String s)
	{
		for (InfoHudSyncType i : InfoHudSyncType.values())
		{
			if (i.toString().equalsIgnoreCase(s))
			{
				return i;
			}
		}

		return null;
	}
}

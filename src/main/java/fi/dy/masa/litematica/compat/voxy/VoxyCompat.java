package fi.dy.masa.litematica.compat.voxy;

import fi.dy.masa.malilib.MaLiLibFabricData;
import fi.dy.masa.malilib.compat.ModIds;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;

public class VoxyCompat
{
	private static boolean isVoxyLoaded = false;
	private static String voxyVersion = "";

	public static void register()
	{
		if (MaLiLibFabricData.ALL_MOD_VERSIONS.containsKey(ModIds.voxy))
		{
			voxyVersion = MaLiLibFabricData.ALL_MOD_VERSIONS.get(ModIds.voxy);
			isVoxyLoaded = true;
		}

		Litematica.LOGGER.info("Voxy: [{}]", isVoxyLoaded ? voxyVersion : "N/F");
	}

	public static boolean hasVoxy()
	{
		return isVoxyLoaded;
	}

	public static void checkConfig()
	{
		Configs.Generic.LOAD_ENTIRE_SCHEMATICS.setBooleanValue(hasVoxy());
	}
}

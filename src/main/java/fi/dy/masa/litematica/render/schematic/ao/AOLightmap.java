package fi.dy.masa.litematica.render.schematic.ao;

import fi.dy.masa.litematica.config.Configs;

public class AOLightmap
{
	protected static final ThreadLocal<AOBrightness> BRIGHTNESS_CACHE = ThreadLocal.withInitial(AOBrightness::new);
	public final AOBrightness brightnessCache = BRIGHTNESS_CACHE.get();

	public void enableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().enable();
		}
	}

	public void disableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().disable();
		}
	}
}

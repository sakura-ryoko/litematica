package fi.dy.masa.litematica.compat.iris;

import fi.dy.masa.malilib.MaLiLibFabricData;
import fi.dy.masa.malilib.compat.ModIds;
import fi.dy.masa.litematica.Litematica;

public class IrisCompat
{
	private static boolean isSodiumLoaded = false;
	private static boolean isIrisLoaded = false;
	private static String sodiumVersion = "";
	private static String irisVersion = "";

	static
	{
		if (MaLiLibFabricData.ALL_MOD_VERSIONS.containsKey(ModIds.sodium))
		{
			sodiumVersion = MaLiLibFabricData.ALL_MOD_VERSIONS.get(ModIds.sodium);
			isSodiumLoaded = true;
		}
		if (MaLiLibFabricData.ALL_MOD_VERSIONS.containsKey(ModIds.iris))
		{
			irisVersion = MaLiLibFabricData.ALL_MOD_VERSIONS.get(ModIds.iris);
			isIrisLoaded = true;
		}

		Litematica.LOGGER.info("Sodium: [{}], Iris: [{}]", isSodiumLoaded ? sodiumVersion : "N/F", isIrisLoaded ? irisVersion : "N/F");
	}

	public static boolean hasSodium()
	{
		return isSodiumLoaded;
	}

	public static boolean hasIris()
	{
		return isSodiumLoaded && isIrisLoaded;
	}

	public static boolean isShaderActive()
	{
//		if (hasIris())
//		{
//			return IrisApi.getInstance().isShaderPackInUse();
//		}

		return false;
	}

	public static boolean isShadowPassActive()
	{
//		if (hasIris())
//		{
//			return IrisApi.getInstance().isRenderingShadowPass();
//		}

		return false;
	}

	public static void registerPipelines()
	{
//		if (hasIris())
//		{
//			Litematica.LOGGER.info("Assigning Litematica Pipelines to Iris Programs:");
//
//			// LEGACY_TERRAIN
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_SOLID_TERRAIN, IrisProgram.TERRAIN_SOLID);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_WIREFRAME, IrisProgram.LINES);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_CUTOUT_TERRAIN, IrisProgram.TERRAIN_CUTOUT);
//
//			// LEGACY_TERRAIN_OFFSET
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_SOLID_TERRAIN_OFFSET, IrisProgram.TERRAIN_SOLID);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_WIREFRAME_OFFSET, IrisProgram.LINES);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET, IrisProgram.TERRAIN_CUTOUT);
//
//			// LEGACY_TERRAIN_TRANSLUCENT
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_TRANSLUCENT, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.LEGACY_TRANSLUCENT_OFFSET, IrisProgram.TRANSLUCENT);
//		}
	}
}
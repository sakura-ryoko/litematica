package fi.dy.masa.litematica;

import net.minecraft.SharedConstants;

import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.util.StringUtils;

public class Reference
{
    public static final String MOD_ID = "litematica";
    public static final String MOD_NAME = "Litematica";
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);
    public static final String MC_VERSION = SharedConstants.getCurrentVersion().id();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID+"-"+MOD_TYPE+"-"+MC_VERSION+"-"+MOD_VERSION;
    private static final boolean LOCAL_DEBUG = false;
    public static final boolean DEBUG_MODE = isDebug();

    private static boolean isDebug()
    {
        return LOCAL_DEBUG || MaLiLibReference.DEBUG_MODE || MaLiLibReference.RUNNING_IN_IDE;
    }
}

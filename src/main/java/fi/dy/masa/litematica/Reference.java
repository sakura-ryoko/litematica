package fi.dy.masa.litematica;

import net.minecraft.SharedConstants;
import fi.dy.masa.malilib.util.StringUtils;

public class Reference
{
    public static final String MOD_ID = "litematica";
    public static final String MOD_NAME = "Litematica";
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);
    public static final String MC_VERSION = SharedConstants.getCurrentVersion().id();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID+"-"+MOD_TYPE+"-"+MC_VERSION+"-"+MOD_VERSION;
    /** Hard limit on how many threads that {@link fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler} is allowed to use **/
    public static final int MAX_PLATFORM_THREADS = 4;
    public static final boolean DEBUG_MODE = false;
}

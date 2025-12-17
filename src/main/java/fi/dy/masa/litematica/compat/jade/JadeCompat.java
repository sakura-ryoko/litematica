package fi.dy.masa.litematica.compat.jade;

import java.util.Objects;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.Litematica;

/**
 * This is just a compat class to "move" the Info Overlay lower so the window doesn't clash as bad.
 */
public class JadeCompat
{
    private static final String JADE_ID = "jade";
    private static final int jadeShift = 35;
    private static boolean hasJade;

    public static void checkForJade()
    {
        String jadeVer = StringUtils.getModVersionString(JADE_ID);

        if (!Objects.equals(jadeVer, "?"))
        {
            Litematica.debugLog("Detected Jade version {}.", jadeVer);
            hasJade = true;
        }
        else
        {
            hasJade = false;
        }
    }

    public static boolean hasJade()
    {
        return hasJade;
    }

    public static int getJadeShift()
    {
        if (hasJade())
        {
            return jadeShift;
        }

        return 0;
    }

    // todo:  Disable snowdee.jade.addon.universal.ItemStorageProvider via reflection while Info Overlay is open?
}

package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

@Deprecated(forRemoval = true)
public enum PlacementManagerThreadProfile implements IConfigOptionListEntry
{
    MAX("max", "litematica.label.pm_thread_profile.max", 32, 20L, 2048),
    DEFAULT("default", "litematica.label.pm_thread_profile.default", 16, 50L, 1024),
    MINIMAL("min", "litematica.label.pm_thread_profile.min", 8, 100L, 512),
    POTATO("potato", "litematica.label.pm_thread_profile.potato", 4, 150L, 256),
    ;

    private static final ImmutableList<PlacementManagerThreadProfile> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;
    private final int maxTicks;
    private final long yieldTime;
    private final int deferredCap;

    PlacementManagerThreadProfile(String configString, String translationKey, int maxTicks, long yieldTime, int deferredCap)
    {
        this.configString = configString;
        this.translationKey = translationKey;
        this.maxTicks = maxTicks;
        this.yieldTime = yieldTime;
        this.deferredCap = deferredCap;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public PlacementManagerThreadProfile cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public PlacementManagerThreadProfile fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static PlacementManagerThreadProfile fromStringStatic(String name)
    {
        for (PlacementManagerThreadProfile val : PlacementManagerThreadProfile.VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PlacementManagerThreadProfile.DEFAULT;
    }

    public int maxTicks()
    {
        return this.maxTicks;
    }

    public long yieldTime()
    {
        return this.yieldTime;
    }

    public int deferredCap()
    {
        return this.deferredCap;
    }
}

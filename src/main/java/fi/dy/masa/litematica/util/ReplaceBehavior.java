package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ReplaceBehavior implements IConfigOptionListEntry, StringIdentifiable
{
    NONE            ("none",            "litematica.gui.label.replace_behavior.none"),
    ALL             ("all",             "litematica.gui.label.replace_behavior.all"),
    WITH_NON_AIR    ("with_non_air",    "litematica.gui.label.replace_behavior.with_non_air");

    public static final StringIdentifiable.EnumCodec<ReplaceBehavior> CODEC = StringIdentifiable.createCodec(ReplaceBehavior::values);
    public static final ImmutableList<ReplaceBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    ReplaceBehavior(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String asString()
    {
        return this.configString;
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
    public IConfigOptionListEntry cycle(boolean forward)
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
    public ReplaceBehavior fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static ReplaceBehavior fromStringStatic(String name)
    {
        for (ReplaceBehavior val : ReplaceBehavior.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return ReplaceBehavior.NONE;
    }
}

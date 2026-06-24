package fi.dy.masa.litematica.util;

import javax.annotation.Nonnull;
import net.minecraft.util.StringRepresentable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum InclusionType implements IConfigOptionListEntry, StringRepresentable
{
    NONE             ("all",             "litematica.gui.label.inclusion_type.none"),
    INCLUDE          ("include",         "litematica.gui.label.inclusion_type.include"),
    ONLY             ("only",            "litematica.gui.label.inclusion_type.only");

    public static final StringRepresentable.EnumCodec<InclusionType> CODEC = StringRepresentable.fromEnum(InclusionType::values);
    public static final ImmutableList<InclusionType> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    InclusionType(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public @Nonnull String getSerializedName()
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
    public InclusionType fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static InclusionType fromStringStatic(String name)
    {
        for (InclusionType mode : InclusionType.values())
        {
            if (mode.configString.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return InclusionType.NONE;
    }
}

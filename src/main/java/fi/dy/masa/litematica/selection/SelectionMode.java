package fi.dy.masa.litematica.selection;

import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum SelectionMode implements IConfigOptionListEntry, StringIdentifiable
{
    NORMAL  ("normal", "litematica.gui.label.area_selection.mode.normal"),
    SIMPLE  ("simple", "litematica.gui.label.area_selection.mode.simple");

    public static final EnumCodec<SelectionMode> CODEC = StringIdentifiable.createCodec(SelectionMode::values);
    public static final ImmutableList<SelectionMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    SelectionMode(String configName, String translationKey)
    {
        this.configString = configName;
        this.translationKey = translationKey;
    }

    public Codec<SelectionMode> codec()
    {
        return CODEC;
    }

    public String getTranslationKey()
    {
        return this.translationKey;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public SelectionMode cycle(boolean forward)
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
    public SelectionMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static SelectionMode fromStringStatic(String name)
    {
        for (SelectionMode mode : SelectionMode.values())
        {
            if (mode.name().equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return SelectionMode.NORMAL;
    }

    @Override
    public @Nonnull String asString()
    {
        return this.configString;
    }
}

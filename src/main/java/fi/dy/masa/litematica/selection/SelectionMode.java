package fi.dy.masa.litematica.selection;

import com.google.common.collect.ImmutableList;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.util.StringUtils;

public enum SelectionMode implements StringIdentifiable
{
    NORMAL  ("normal", "litematica.gui.label.area_selection.mode.normal"),
    SIMPLE  ("simple", "litematica.gui.label.area_selection.mode.simple");

    public static final StringIdentifiable.EnumCodec<SelectionMode> CODEC = StringIdentifiable.createCodec(SelectionMode::values);
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

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

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

    public static SelectionMode fromString(String name)
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
    public String asString()
    {
        return this.configString;
    }
}

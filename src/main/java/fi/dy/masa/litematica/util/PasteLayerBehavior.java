package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum PasteLayerBehavior implements IConfigOptionListEntry, StringIdentifiable
{
    ALL             ("all",             "litematica.gui.label.paste_layer_behavior.all"),
    RENDERED_ONLY   ("rendered_only",   "litematica.gui.label.paste_layer_behavior.rendered_only");

    public static final EnumCodec<PasteLayerBehavior> CODEC = StringIdentifiable.createCodec(PasteLayerBehavior::values);
    public static final ImmutableList<PasteLayerBehavior> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    PasteLayerBehavior(String configString, String translationKey)
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
    public PasteLayerBehavior fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static PasteLayerBehavior fromStringStatic(String name)
    {
        for (PasteLayerBehavior val : PasteLayerBehavior.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PasteLayerBehavior.ALL;
    }
}

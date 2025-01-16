package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.Schema;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.config.Configs;

public enum DataFixerMode implements IConfigOptionListEntry
{
    ALWAYS                  ("always", "litematica.gui.label.data_fixer_mode.always"),
    //BELOW_1215              ("below_1215", "litematica.gui.label.data_fixer_mode.below_1215"),
    BELOW_1205              ("below_1205", "litematica.gui.label.data_fixer_mode.below_1205"),
    BELOW_120X              ("below_120X", "litematica.gui.label.data_fixer_mode.below_120X"),
    BELOW_119X              ("below_119X", "litematica.gui.label.data_fixer_mode.below_119X"),
    BELOW_117X              ("below_117X", "litematica.gui.label.data_fixer_mode.below_117X"),
    BELOW_116X              ("below_116X", "litematica.gui.label.data_fixer_mode.below_116X"),
    BELOW_113X              ("below_113X", "litematica.gui.label.data_fixer_mode.below_113X"),
    BELOW_112X              ("below_112X", "litematica.gui.label.data_fixer_mode.below_112X"),
    NEVER                   ("never", "litematica.gui.label.data_fixer_mode.never");

    public static final ImmutableList<DataFixerMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    DataFixerMode(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
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
    public DataFixerMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static DataFixerMode fromStringStatic(String name)
    {
        for (DataFixerMode val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return DataFixerMode.ALWAYS;
    }

    @Nullable
    public static Schema getEffectiveSchema(int dataVersion)
    {
        DataFixerMode config = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Schema schema = Schema.getSchemaByDataVersion(dataVersion);

        switch (config)
        {
            case ALWAYS -> { return schema; }
            // FIXME 1.21.5+
            /*
            case BELOW_1215 ->
            {
                if (dataVersion < Schema.SCHEMA_1_21_05.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
             */
            case BELOW_1205 ->
            {
                if (dataVersion < Schema.SCHEMA_1_20_05.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_120X ->
            {
                if (dataVersion < Schema.SCHEMA_1_20_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_119X ->
            {
                if (dataVersion < Schema.SCHEMA_1_19_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_117X ->
            {
                if (dataVersion < Schema.SCHEMA_1_17_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_116X ->
            {
                if (dataVersion < Schema.SCHEMA_1_16_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_113X ->
            {
                if (dataVersion < Schema.SCHEMA_1_13_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case BELOW_112X ->
            {
                if (dataVersion < Schema.SCHEMA_1_12_00.getDataVersion())
                {
                    return schema;
                }

                return null;
            }
            case NEVER -> { return null; }
            default -> { return Schema.getSchemaByDataVersion(Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getDefaultIntegerValue()); }
        }
    }
}

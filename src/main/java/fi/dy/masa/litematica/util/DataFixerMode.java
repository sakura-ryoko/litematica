package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.config.Configs;

public enum DataFixerMode implements IConfigOptionListEntry
{
    ALWAYS                  ("always", "litematica.gui.label.data_fixer_mode.always"),
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
        Schema schema = getSchemaByVersion(dataVersion);

        switch (config)
        {
            case ALWAYS -> { return schema; }
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
            default -> { return getSchemaByVersion(Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getDefaultIntegerValue()); }
        }
    }

    public static Schema getSchemaByVersion(int dataVersion)
    {
        for (Schema schema : Schema.values())
        {
            if (schema.getDataVersion() < dataVersion)
            {
                return schema;
            }
        }

        return getSchemaByVersion(Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getDefaultIntegerValue());
    }

    // TODO --> Add Schema Versions to this as versions get released
    public enum Schema
    {
        // Minecraft Data Versions
        SCHEMA_1_21_00 (3953),
        SCHEMA_1_20_05 (3837),
        SCHEMA_1_20_04 (3700),
        SCHEMA_1_20_02 (3578),
        SCHEMA_1_20_01 (3465),
        SCHEMA_1_20_00 (3463),
        SCHEMA_1_19_04 (3337),
        SCHEMA_1_19_00 (3105),
        SCHEMA_1_18_02 (2975),
        SCHEMA_1_18_00 (2860),
        SCHEMA_1_17_01 (2730),
        SCHEMA_1_17_00 (2724),
        SCHEMA_1_16_05 (2586),
        SCHEMA_1_16_00 (2566),
        SCHEMA_1_15_02 (2230),
        SCHEMA_1_15_00 (2225),
        SCHEMA_1_14_04 (1976),
        SCHEMA_1_14_00 (1952),
        SCHEMA_1_13_02 (1631),
        SCHEMA_1_13_00 (1519),
        SCHEMA_1_12_02 (1343),
        SCHEMA_1_12_00 (1139),
        SCHEMA_1_11_02 (922),
        SCHEMA_1_11_00 (819),
        SCHEMA_1_10_02 (512),
        SCHEMA_1_10_00 (510),
        SCHEMA_1_09_04 (184),
        SCHEMA_1_09_00 (169),
        SCHEMA_15W32A  (100);

        private final int schemaId;

        Schema(int id)
        {
            this.schemaId = id;
        }

        public int getDataVersion()
        {
            return this.schemaId;
        }
    }
}

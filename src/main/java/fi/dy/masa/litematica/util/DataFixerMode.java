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
            if (schema.getDataVersion() <= dataVersion)
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
        SCHEMA_1_21_04 (4188, "1.21.4"), // 1.21.4-rc.3
        SCHEMA_24W46A  (4178, "24w46a"),
        SCHEMA_24W44A  (4174, "24w44a"),
        SCHEMA_1_21_03 (4082, "1.21.3"),
        SCHEMA_1_21_02 (4080, "1.21.2"),
        SCHEMA_24W40A  (4072, "24w40a"),
        SCHEMA_24W37A  (4065, "24w37a"),
        SCHEMA_24W35A  (4062, "24w35a"),
        SCHEMA_24W33A  (4058, "24w33a"),
        SCHEMA_1_21_01 (3955, "1.21.1"),
        SCHEMA_1_21_00 (3953, "1.21"),
        SCHEMA_24W21A  (3946, "24w21a"),
        SCHEMA_24W18A  (3940, "24w18a"),
        SCHEMA_1_20_05 (3837, "1.20.5"),
        SCHEMA_24W14A  (3827, "24w14a"),
        SCHEMA_24W13A  (3826, "24w13a"),
        SCHEMA_24W12A  (3824, "24w12a"),
        SCHEMA_24W10A  (3821, "24w10a"),
        SCHEMA_24W09A  (3819, "24w09a"), // Data Components ( https://minecraft.wiki/w/Data_component_format )
        SCHEMA_24W07A  (3817, "24w07a"),
        SCHEMA_24W03A  (3804, "24w03a"),
        SCHEMA_23W51A  (3801, "23w51a"),
        SCHEMA_1_20_04 (3700, "1.20.4"),
        SCHEMA_23W46A  (3691, "23w46a"),
        SCHEMA_23W43B  (3687, "23w43b"),
        SCHEMA_23W40A  (3679, "23w40a"),
        SCHEMA_1_20_02 (3578, "1.20.2"),
        SCHEMA_23W35A  (3571, "23w35a"),
        SCHEMA_23W31A  (3567, "23w31a"),
        SCHEMA_1_20_01 (3465, "1.20.1"),
        SCHEMA_1_20_00 (3463, "1.20"),
        SCHEMA_23W18A  (3453, "23w18a"),
        SCHEMA_23W16A  (3449, "23w16a"),
        SCHEMA_23W12A  (3442, "23w12a"),
        SCHEMA_1_19_04 (3337, "1.19.4"),
        SCHEMA_1_19_03 (3218, "1.19.3"),
        SCHEMA_1_19_02 (3120, "1.19.2"),
        SCHEMA_1_19_01 (3117, "1.19.1"),
        SCHEMA_1_19_00 (3105, "1.19"),
        SCHEMA_22W19A  (3096, "22w19a"),
        SCHEMA_22W16A  (3091, "22w16a"),
        SCHEMA_22W11A  (3080, "22w11a"),
        SCHEMA_1_18_02 (2975, "1.18.2"),
        SCHEMA_1_18_01 (2865, "1.18.1"),
        SCHEMA_1_18_00 (2860, "1.18"),
        SCHEMA_21W44A  (2845, "21w44a"),
        SCHEMA_21W41A  (2839, "21w41a"),
        SCHEMA_21W37A  (2834, "21w37a"),
        SCHEMA_1_17_01 (2730, "1.17.1"),
        SCHEMA_1_17_00 (2724, "1.17"),
        SCHEMA_21W20A  (2715, "21w20a"),
        SCHEMA_21W15A  (2709, "21w15a"),
        SCHEMA_21W10A  (2699, "21w10a"),
        SCHEMA_21W05A  (2690, "21w05a"),
        SCHEMA_20W49A  (2685, "20w49a"),
        SCHEMA_20W45A  (2681, "20w45a"),
        SCHEMA_1_16_05 (2586, "1.16.5"),
        SCHEMA_1_16_04 (2584, "1.16.4"),
        SCHEMA_1_16_03 (2580, "1.16.3"),
        SCHEMA_1_16_02 (2578, "1.16.2"),
        SCHEMA_1_16_01 (2567, "1.16.1"),
        SCHEMA_1_16_00 (2566, "1.16"),
        SCHEMA_20W22A  (2555, "20w22a"),
        SCHEMA_20W15A  (2525, "20w15a"),
        SCHEMA_20W06A  (2504, "20w06a"),
        SCHEMA_1_15_02 (2230, "1.15.2"),
        SCHEMA_1_15_01 (2227, "1.15.1"),
        SCHEMA_1_15_00 (2225, "1.15"),
        SCHEMA_19W46B  (2217, "19w46b"),
        SCHEMA_19W40A  (2208, "19w40a"),
        SCHEMA_19W34A  (2200, "19w34a"),
        SCHEMA_1_14_04 (1976, "1.14.4"),
        SCHEMA_1_14_03 (1968, "1.14.3"),
        SCHEMA_1_14_02 (1963, "1.14.2"),
        SCHEMA_1_14_01 (1957, "1.14.1"),
        SCHEMA_1_14_00 (1952, "1.14"),
        SCHEMA_19W14B  (1945, "19w14b"),
        SCHEMA_19W08B  (1934, "19w08b"),
        SCHEMA_18W50A  (1919, "18w50a"),
        SCHEMA_18W43A  (1901, "18w43a"),
        SCHEMA_1_13_02 (1631, "1.13.2"),
        SCHEMA_1_13_01 (1628, "1.13.1"),
        SCHEMA_1_13_00 (1519, "1.13"),
        SCHEMA_18W22C  (1499, "18w22c"),
        SCHEMA_18W14B  (1481, "18w14b"),
        SCHEMA_18W07C  (1469, "18w07c"),
        SCHEMA_17W50A  (1457, "17w50a"),
        SCHEMA_17W47A  (1451, "17w47a"), // The Flattening ( https://minecraft.wiki/w/Java_Edition_1.13/Flattening )
        SCHEMA_17W46A  (1449, "17w46a"),
        SCHEMA_17W43A  (1444, "17w43a"),
        SCHEMA_1_12_02 (1343, "1.12.2"),
        SCHEMA_1_12_01 (1241, "1.12.1"),
        SCHEMA_1_12_00 (1139, "1.12"),
        SCHEMA_1_11_02 (922,  "1.11.2"),
        SCHEMA_1_11_00 (819,  "1.11"),
        SCHEMA_1_10_02 (512,  "1.10.2"),
        SCHEMA_1_10_00 (510,  "1.10"),
        SCHEMA_1_09_04 (184,  "1.9.4"),
        SCHEMA_1_09_00 (169,  "1.9"),
        SCHEMA_15W32A  (100,  "15w32a");

        private final int schemaId;
        private final String str;

        Schema(int id, String ver)
        {
            this.schemaId = id;
            this.str = ver;
        }

        public int getDataVersion()
        {
            return this.schemaId;
        }

        public String getString()
        {
            return this.str;
        }

        @Override
        public String toString()
        {
            return "MC: "+this.getString()+" [Schema: "+this.getDataVersion()+"]";
        }
    }
}

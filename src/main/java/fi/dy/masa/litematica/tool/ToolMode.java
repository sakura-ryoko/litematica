package fi.dy.masa.litematica.tool;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.EntityUtils;

public enum ToolMode implements StringIdentifiable
{
    AREA_SELECTION      ("area_selection",      "litematica.tool_mode.name.area_selection",        false, false),
    SCHEMATIC_PLACEMENT ("schematic_placement", "litematica.tool_mode.name.schematic_placement",   false, true),
    FILL                ("fill",                "litematica.tool_mode.name.fill",                  true, false, true, false),
    REPLACE_BLOCK       ("replace_block",       "litematica.tool_mode.name.replace_block",         true, false, true, true),
    PASTE_SCHEMATIC     ("paste_schematic",     "litematica.tool_mode.name.paste_schematic",       true, true),
    GRID_PASTE          ("grid_paste",          "litematica.tool_mode.name.grid_paste",            true, true),
    MOVE                ("move",                "litematica.tool_mode.name.move",                  true, false),
    DELETE              ("delete",              "litematica.tool_mode.name.delete",                true, false),
    REBUILD             ("rebuild",             "litematica.tool_mode.name.rebuild",               false, true, true, false);

    public static final StringIdentifiable.EnumCodec<ToolMode> CODEC = StringIdentifiable.createCodec(ToolMode::values);
    public static final ImmutableList<ToolMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String unlocName;
    private final boolean creativeOnly;
    private final boolean usesSchematic;
    private final boolean usesBlockPrimary;
    private final boolean usesBlockSecondary;

    @Nullable private BlockState blockPrimary;
    @Nullable private BlockState blockSecondary;

    ToolMode(String configName, String unlocName, boolean creativeOnly, boolean usesSchematic)
    {
        this(configName, unlocName, creativeOnly, usesSchematic, false, false);
    }

    ToolMode(String configName, String unlocName, boolean creativeOnly, boolean usesSchematic, boolean usesBlockPrimary, boolean usesBlockSecondary)
    {
        this.configString = configName;
        this.unlocName = unlocName;
        this.creativeOnly = creativeOnly;
        this.usesSchematic = usesSchematic;
        this.usesBlockPrimary = usesBlockPrimary;
        this.usesBlockSecondary = usesBlockSecondary;
    }

    public Codec<ToolMode> codec()
    {
        return CODEC;
    }

    @Override
    public String asString()
    {
        return this.configString;
    }

    public boolean getUsesSchematic()
    {
        if (this == ToolMode.DELETE && ToolModeData.DELETE.getUsePlacement())
        {
            return true;
        }

        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection()
    {
        return this.getUsesSchematic() == false || DataManager.getSchematicProjectsManager().hasProjectOpen();
    }

    public boolean getUsesBlockPrimary()
    {
        return this.usesBlockPrimary;
    }

    public boolean getUsesBlockSecondary()
    {
        return this.usesBlockSecondary;
    }

    @Nullable
    public BlockState getPrimaryBlock()
    {
        return this.blockPrimary;
    }

    @Nullable
    public BlockState getSecondaryBlock()
    {
        return this.blockSecondary;
    }

    public void setPrimaryBlock(@Nullable BlockState state)
    {
        this.blockPrimary = state;
    }

    public void setSecondaryBlock(@Nullable BlockState state)
    {
        this.blockSecondary = state;
    }

    public String getName()
    {
        return StringUtils.translate(this.unlocName);
    }

    public ToolMode cycle(PlayerEntity player, boolean forward)
    {
        ToolMode[] values = ToolMode.values();
        final boolean isCreative = EntityUtils.isCreativeMode(player);
        final int numModes = values.length;
        final int inc = forward ? 1 : -1;
        int nextId = this.ordinal() + inc;

        for (int i = 0; i < numModes; ++i)
        {
            if (nextId < 0)
            {
                nextId = numModes - 1;
            }
            else if (nextId >= numModes)
            {
                nextId = 0;
            }

            ToolMode mode = values[nextId];

            if (isCreative || mode.creativeOnly == false)
            {
                return mode;
            }

            nextId += inc;
        }

        return this;
    }
}

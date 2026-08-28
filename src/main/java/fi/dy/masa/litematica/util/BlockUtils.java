package fi.dy.masa.litematica.util;

import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import com.google.common.base.Splitter;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;

public class BlockUtils
{
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    public static BlockState fixMirrorDoubleChest(BlockState state, Mirror mirror, ChestType type)
    {
        Direction facing = state.getValue(ChestBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        if (mirror == Mirror.FRONT_BACK) // x
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.X)
            {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        }
        else if (mirror == Mirror.LEFT_RIGHT) // z
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.Z)
            {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        }

        return state;
    }

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str, int minecraftDataVersion)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
			// Run Data Fixer
	        blockName = SchematicConversionMaps.updateBlockName(blockName, minecraftDataVersion);
            Identifier id = Identifier.tryParse(blockName);

            if (id != null && BuiltInRegistries.BLOCK.containsKey(id))
            {
                Optional<Holder.Reference<Block>> opt = BuiltInRegistries.BLOCK.get(id);
                Block block;

                if (opt.isPresent())
                {
                    block = opt.get().value();
                    BlockState state = block.defaultBlockState();

                    if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']')
                    {
                        StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();
                        String propStr = str.substring(index + 1, str.length() - 1);

                        for (String propAndVal : COMMA_SPLITTER.split(propStr))
                        {
                            Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                            if (valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Property<?> prop = stateManager.getProperty(valIter.next());

                            if (prop == null || valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                            if (val != null)
                            {
                                state = getBlockStateWithProperty(state, prop, val);
                            }
                        }
                    }

                    return Optional.of(state);
                }
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop, Comparable<?> value)
    {
        return state.setValue(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr)
    {
        return prop.getValue(valStr).orElse(null);
    }

    public static boolean blocksHaveSameProperties(BlockState state1, BlockState state2)
    {
        StateDefinition<Block, BlockState> stateManager1 = state1.getBlock().getStateDefinition();
        StateDefinition<Block, BlockState> stateManager2 = state2.getBlock().getStateDefinition();
        return stateManager1.getProperties().equals(stateManager2.getProperties());
    }

    public static Optional<Block> getBlockFromString(String str)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
            Identifier id = Identifier.tryParse(blockName);

            if (id != null && BuiltInRegistries.BLOCK.containsKey(id))
            {
                Block block = BuiltInRegistries.BLOCK.getValue(id);

                return Optional.of(block);
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static Optional<TagKey<Block>> getBlockTagFromString(String str)
    {
        if (str.startsWith("#"))
		{
            try
            {
                String tagName = str.substring(1);
                Identifier id = Identifier.tryParse(tagName);

				if (id != null)
				{
					TagKey<Block> blockTag = TagKey.create(Registries.BLOCK, id);
					return Optional.of(blockTag);
				}
            }
			catch (Exception e)
            {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public static boolean areStatesEqualIgnoringAge(BlockState state1, BlockState state2)
    {
        if (state1 == state2)
        {
            return true;
        }

        if (state1.getBlock() != state2.getBlock())
        {
            return false;
        }

        for (Property<?> prop : state1.getProperties())
        {
            if ("age".equalsIgnoreCase(prop.getName()))
            {
                continue;
            }

            if (!state1.getValue(prop).equals(state2.getValue(prop)))
            {
                return false;
            }
        }

        return true;
    }
}

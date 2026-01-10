package fi.dy.masa.litematica.materials;

import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import fi.dy.masa.litematica.mixin.block.IMixinAbstractBlock;

public class MaterialCache
{
    private static final MaterialCache INSTANCE = new MaterialCache();

    protected final IdentityHashMap<BlockState, ItemStack> buildItemsForStates = new IdentityHashMap<>();
    protected final IdentityHashMap<BlockState, ItemStack> displayItemsForStates = new IdentityHashMap<>();

    private MaterialCache() { }

    public static MaterialCache getInstance()
    {
        return INSTANCE;
    }

    public void clearCache()
    {
        this.buildItemsForStates.clear();
        this.displayItemsForStates.clear();
    }

//    public ItemStack getRequiredBuildItemForState(BlockState state)
//    {
//        return this.getRequiredBuildItemForState(state);
//    }

    public ItemStack getRequiredBuildItemForState(BlockState state)
    {
        ItemStack stack = this.buildItemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, true);
        }

        return stack;
    }

    public ItemStack getRequiredBuildItemForState(BlockState state, @Nullable Level world, BlockPos pos)
    {
        ItemStack stack = this.buildItemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, world, pos, true);
        }

        return stack;
    }

    public ItemStack getItemForDisplayNameForState(BlockState state)
    {
        ItemStack stack = this.displayItemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, false);
        }

        return stack;
    }

    protected ItemStack getItemForStateFromWorld(BlockState state, boolean isBuildItem)
    {
        return getItemForStateFromWorld(state, null, BlockPos.ZERO, isBuildItem);
    }

    protected ItemStack getItemForStateFromWorld(BlockState state, @Nullable Level world, BlockPos pos, boolean isBuildItem)
    {
        ItemStack stack = isBuildItem ? this.getStateToItemOverride(state) : null;

        if (stack == null)
        {
            if (world == null)
            {
                stack = state.getBlock().asItem().getDefaultInstance();
            }
            else
            {
//            world.setBlock(pos, state, 0x14);
                stack = ((IMixinAbstractBlock) state.getBlock()).litematica_getPickStack(world, pos, state, false);
            }
        }

        if (stack == null || stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            this.overrideStackSize(state, stack);
        }

        if (isBuildItem)
        {
            this.buildItemsForStates.put(state, stack);
        }
        else
        {
            this.displayItemsForStates.put(state, stack);
        }

        return stack;
    }

    public boolean requiresMultipleItems(BlockState state)
    {
        Block block = state.getBlock();
        if (block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT)
        {
            return true;
        }
        // Block Entity Stuff
//        else if (block instanceof LecternBlock && state.get(LecternBlock.HAS_BOOK))
//        {
//            return true;
//        }
//        else if (block instanceof ChiseledBookshelfBlock)
//        {
//            for (BooleanProperty prop : ChiseledBookshelfBlock.SLOT_OCCUPIED_PROPERTIES)
//            {
//                if (state.get(prop))
//                {
//                    return true;
//                }
//            }
//
//            return false;
//        }
        else return block instanceof AbstractCauldronBlock && block != Blocks.CAULDRON;
    }

//    public ImmutableList<ItemStack> getItems(BlockState state)
//    {
//        return this.getItems(state, this.checkPos);
//    }

    public ImmutableList<ItemStack> getItems(BlockState state)
    {
        Block block = state.getBlock();

        if (block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT)
        {
            Block pottedBlock = ((FlowerPotBlock) block).getPotted();

            return ImmutableList.of(new ItemStack(Blocks.FLOWER_POT),
                                    pottedBlock.equals(Blocks.AIR)
                                    ? ItemStack.EMPTY
                                    : pottedBlock.asItem().getDefaultInstance()
            );

//            return ImmutableList.of(new ItemStack(Blocks.FLOWER_POT), ((IMixinAbstractBlock) block).litematica_getPickStack(world, pos, state, false));
        }
        else if (block instanceof AbstractCauldronBlock && block != Blocks.CAULDRON)
        {
            if (block instanceof LavaCauldronBlock)
            {
                return ImmutableList.of(new ItemStack(Blocks.CAULDRON), new ItemStack(Items.LAVA_BUCKET));
            }
            else if (block == Blocks.POWDER_SNOW_CAULDRON)
            {
                return ImmutableList.of(new ItemStack(Blocks.CAULDRON), new ItemStack(Items.POWDER_SNOW_BUCKET));
            }
            else if (block == Blocks.WATER_CAULDRON)
            {
                final int level = state.getValue(LayeredCauldronBlock.LEVEL);

                return switch (level)
                {
                    case 1 -> ImmutableList.of(new ItemStack(Blocks.CAULDRON), PotionContents.createItemStack(Items.POTION, Potions.WATER));
                    case 2 -> ImmutableList.of(new ItemStack(Blocks.CAULDRON), PotionContents.createItemStack(Items.POTION, Potions.WATER), PotionContents.createItemStack(Items.POTION, Potions.WATER));
                    case 3 -> ImmutableList.of(new ItemStack(Blocks.CAULDRON), new ItemStack(Items.WATER_BUCKET));
                    default -> ImmutableList.of(new ItemStack(Blocks.CAULDRON));
                };
            }
        }

        return ImmutableList.of(this.getRequiredBuildItemForState(state));
    }

    @Nullable
    protected ItemStack getStateToItemOverride(BlockState state)
    {
        Block block = state.getBlock();

        if (block == Blocks.PISTON_HEAD ||
            block == Blocks.MOVING_PISTON ||
            block == Blocks.NETHER_PORTAL ||
            block == Blocks.END_PORTAL ||
            block == Blocks.END_GATEWAY)
        {
            return ItemStack.EMPTY;
        }
        else if (block == Blocks.FARMLAND)
        {
            return new ItemStack(Blocks.DIRT);
        }
        else if (block == Blocks.BROWN_MUSHROOM_BLOCK)
        {
            return new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK);
        }
        else if (block == Blocks.RED_MUSHROOM_BLOCK)
        {
            return new ItemStack(Blocks.RED_MUSHROOM_BLOCK);
        }
        else if (block == Blocks.LAVA)
        {
            if (state.getValue(LiquidBlock.LEVEL) == 0)
            {
                return new ItemStack(Items.LAVA_BUCKET);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        else if (block == Blocks.WATER)
        {
            if (state.getValue(LiquidBlock.LEVEL) == 0)
            {
                return new ItemStack(Items.WATER_BUCKET);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        else if (block instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof DoublePlantBlock && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }

        return null;
    }

    protected void overrideStackSize(BlockState state, ItemStack stack)
    {
        Block block = state.getBlock();

        if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
        else if (block == Blocks.SNOW)
        {
            stack.setCount(state.getValue(SnowLayerBlock.LAYERS));
        }
        else if (block instanceof TurtleEggBlock)
        {
            stack.setCount(state.getValue(TurtleEggBlock.EGGS));
        }
        else if (block instanceof SeaPickleBlock)
        {
            stack.setCount(state.getValue(SeaPickleBlock.PICKLES));
        }
        else if (block instanceof CandleBlock)
        {
            stack.setCount(state.getValue(CandleBlock.CANDLES));
        }
        else if (block instanceof MultifaceSpreadeableBlock)
        {
            stack.setCount(MultifaceSpreadeableBlock.availableFaces(state).size());
        }
    }
}

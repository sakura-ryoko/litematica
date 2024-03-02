package fi.dy.masa.litematica.util;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemUtils
{
    private static final IdentityHashMap<BlockState, ItemStack> ITEMS_FOR_STATES = new IdentityHashMap<>();

    /*

    *** Irrelevant to use any further; ComponentMap's are set, but are EMPTY so why bother checking them this way?

    public static boolean areTagsEqualIgnoreDamage(ItemStack stackReference, ItemStack stackToCheck)
    {
        ComponentMap tagReference = stackReference.getComponents();
        ComponentMap tagToCheck = stackToCheck.getComponents();

        if (tagReference != null && tagToCheck != null)
        {
            if (tagReference.contains(DataComponentTypes.DAMAGE) || tagToCheck.contains(DataComponentTypes.DAMAGE))
            {
                return false;
            }
                Set<String> keysReference = new HashSet<>(tagReference.getKeys());

                for (String key : keysReference) {
                    if (key.equals("Damage")) {
                        continue;
                    }

                    if (!Objects.equals(tagReference.get(key), tagToCheck.get(key))) {
                        return false;
                    }
                }
                return Objects.equals(stackReference.get(DataComponentTypes.DAMAGE), stackToCheck.get(DataComponentTypes.DAMAGE));
        }

        return (tagReference == null) && (tagToCheck == null);
    }
*/

    public static ItemStack getItemForState(BlockState state)
    {
        ItemStack stack = ITEMS_FOR_STATES.get(state);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void setItemForBlock(World world, BlockPos pos, BlockState state)
    {
        if (!ITEMS_FOR_STATES.containsKey(state))
        {
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state, false));
        }
    }

    public static ItemStack getItemForBlock(World world, BlockPos pos, BlockState state, boolean checkCache)
    {
        if (checkCache)
        {
            ItemStack stack = ITEMS_FOR_STATES.get(state);

            if (stack != null)
            {
                return stack;
            }
        }

        if (state.isAir())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getStateToItemOverride(state);

        if (stack.isEmpty())
        {
            stack = state.getBlock().getPickStack(world, pos, state);
        }

        if (stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            overrideStackSize(state, stack);
        }

        ITEMS_FOR_STATES.put(state, stack);

        return stack;
    }

    public static ItemStack getStateToItemOverride(BlockState state)
    {
        if (state.getBlock() == Blocks.LAVA)
        {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        else if (state.getBlock() == Blocks.WATER)
        {
            return new ItemStack(Items.WATER_BUCKET);
        }

        return ItemStack.EMPTY;
    }

    private static void overrideStackSize(BlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
    }

    public static void storeTEInStack(ItemStack stack, BlockEntity te)
    {
        NbtCompound tag = te.createNbtWithId(DataManager.getInstance().getWorldRegistryManager());
        ComponentMap data = stack.getComponents();

        Litematica.debugLog("storeTEInStack(): te tag: {}", tag.toString());

        if ((stack.getItem() instanceof BlockItem &&
            ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSkullBlock)
                || (stack.getItem() instanceof PlayerHeadItem))
        {
            if (tag.contains("SkullOwner", 10))
            {
                NbtCompound tagOwner = tag.getCompound("SkullOwner");

                Litematica.debugLog("storeTEInStack(): SkullOwner: {}", tagOwner.toString());

                //NbtCompound tagSkull = new NbtCompound();
                //tagSkull.put("SkullOwner", tagOwner);
                //stack.setNbt(tagSkull);

                if (data != null && data.contains(DataComponentTypes.PROFILE))
                {
                    ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
                    if (profile != null)
                    {
                        // Compare the UUID
                        if (!tagOwner.getUuid("Id").equals(profile.gameProfile().getId()))
                        {
                            GameProfile newGameProfile = NbtHelper.toGameProfile(tagOwner);
                            if (newGameProfile != null)
                            {
                                ProfileComponent newProfile = new ProfileComponent(newGameProfile);
                                stack.set(DataComponentTypes.PROFILE, newProfile);

                                Litematica.debugLog("storeTEInStack(): newProfile set 1 {}", newProfile.toString());
                            }
                        }
                    }
                }
                else
                {
                    // DataCompoent doesn't exist, add it.
                    GameProfile newGameProfile = NbtHelper.toGameProfile(tagOwner);
                    if (newGameProfile != null)
                    {
                        ProfileComponent newProfile = new ProfileComponent(newGameProfile);
                        stack.set(DataComponentTypes.PROFILE, newProfile);

                        Litematica.debugLog("storeTEInStack(): newProfile set 2 {}", newProfile.toString());
                    }
                }
            }
            else if (tag.contains("ExtraType", 8))
            {
                String extraUUID = tag.getString("ExtraType");

                Litematica.debugLog("storeTEInStack(): extraUUID {}", extraUUID);

                if (!extraUUID.isEmpty())
                {
                    UUID uuid = UUID.fromString(extraUUID);

                    if (data != null && data.contains(DataComponentTypes.PROFILE))
                    {
                        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
                        if (profile != null)
                        {
                            // Compare the UUID
                            if (!uuid.equals(profile.gameProfile().getId()))
                            {
                                GameProfile extraGameProfile = new GameProfile(Util.NIL_UUID, extraUUID);
                                ProfileComponent newProfile = new ProfileComponent(extraGameProfile);
                                stack.set(DataComponentTypes.PROFILE, newProfile);

                                Litematica.debugLog("storeTEInStack(): newProfile set 3 {}", newProfile.toString());
                            }
                        }
                    }
                    else
                    {
                        // DataComponent doesn't exist, add it.
                        GameProfile extraGameProfile = new GameProfile(Util.NIL_UUID, extraUUID);
                        ProfileComponent newProfile = new ProfileComponent(extraGameProfile);
                        stack.set(DataComponentTypes.PROFILE, newProfile);

                        Litematica.debugLog("storeTEInStack(): newProfile set 4 {}", newProfile.toString());
                    }
                }
            }
        }
        else
        {
            // So this is where the now "infamous" Purple (+NBT) lore comes from on my Shulker boxes?
            /*
            NbtCompound tagLore = new NbtCompound();
            NbtList tagList = new NbtList();

            tagList.add(NbtString.of("(+NBT)"));
            tagLore.put("Lore", tagList);
            stack.setSubNbt("display", tagLore);
            stack.setSubNbt("BlockEntityTag", tag);
             */

            if (data != null && data.contains(DataComponentTypes.BLOCK_ENTITY_DATA))
            {
                NbtComponent nbt = NbtComponent.of(tag);

                // Overwrite existing data
                stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, nbt);

                Litematica.debugLog("storeTEInStack(): set block entity data: {}", nbt.toString());
            }
        }
    }

    public static String getStackString(ItemStack stack)
    {
        if (!stack.isEmpty())
        {
            Identifier rl = Registries.ITEM.getId(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                                 rl != null ? rl.toString() : "null", stack.getName().getString(),
                                 stack.getComponents() != null ? stack.getComponents().toString() : "<no NBT>", stack);
        }

        return "<empty>";
    }
}

package fi.dy.masa.litematica.util;

import java.util.*;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.litematica.Litematica;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    public static void storeTEInStack(ItemStack stack, BlockEntity te, DynamicRegistryManager registryManager)
    {
        NbtCompound tag = te.createNbtWithId(registryManager);
        ComponentMap data = stack.getComponents();

        Litematica.logger.info("storeTEInStack(): TE tag: {} -> item: {}", tag.toString(), stack.getItem().toString());

        if ((stack.getItem() instanceof BlockItem &&
            ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSkullBlock)
                || (stack.getItem() instanceof PlayerHeadItem))
        {
            if (tag.contains("SkullOwner", 10))
            {
                NbtCompound tagOwner = tag.getCompound("SkullOwner");

                Litematica.debugLog("storeTEInStack(): SkullOwner: {}", tagOwner.toString());

                if (data != null && data.contains(DataComponentTypes.PROFILE))
                {
                    ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
                    if (profile != null)
                    {
                        // Compare the UUID
                        if (!tagOwner.getUuid("Id").equals(profile.gameProfile().getId()))
                        {
                            GameProfile newGameProfile;
                            if (tagOwner.contains("Name"))
                            {
                                newGameProfile = new GameProfile(tagOwner.getUuid("Id"), tagOwner.getString("Name"));
                            }
                            // Sometimes it's stored in lower case
                            else if (tagOwner.contains("name"))
                            {
                                newGameProfile = new GameProfile(tagOwner.getUuid("id"), tagOwner.getString("name"));
                            }
                            else if (tagOwner.contains("id"))
                            {
                                newGameProfile = new GameProfile(Util.NIL_UUID, tagOwner.getUuid("id").toString());
                            }
                            else
                            {
                                newGameProfile = new GameProfile(Util.NIL_UUID, tagOwner.getUuid("Id").toString());
                            }
                            ProfileComponent newProfile = new ProfileComponent(newGameProfile);
                            stack.set(DataComponentTypes.PROFILE, newProfile);

                            Litematica.debugLog("storeTEInStack(): newProfile set 1 {}", newProfile.toString());
                        }
                    }
                }
                else
                {
                    // DataComponent doesn't exist, add it.
                    GameProfile newGameProfile;
                    if (tagOwner.contains("Name"))
                    {
                        newGameProfile = new GameProfile(tagOwner.getUuid("Id"), tagOwner.getString("Name"));
                    }
                    // Sometimes it's stored in lower case
                    else if (tagOwner.contains("name"))
                    {
                        newGameProfile = new GameProfile(tagOwner.getUuid("id"), tagOwner.getString("name"));
                    }
                    else if (tagOwner.contains("id"))
                    {
                        newGameProfile = new GameProfile(Util.NIL_UUID, tagOwner.getUuid("id").toString());
                    }
                    else
                    {
                        newGameProfile = new GameProfile(Util.NIL_UUID, tagOwner.getUuid("Id").toString());
                    }
                    ProfileComponent newProfile = new ProfileComponent(newGameProfile);
                    stack.set(DataComponentTypes.PROFILE, newProfile);

                    Litematica.debugLog("storeTEInStack(): newProfile set 1 {}", newProfile.toString());
                }
                // Remove
                tag.remove("SkullOwner");
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
                // Remove
                tag.remove("ExtraType");
            }
        }
            // So this is where the now "infamous" Purple (+NBT) lore comes from on my Shulker boxes?
            /*
            NbtCompound tagLore = new NbtCompound();
            NbtList tagList = new NbtList();

            tagList.add(NbtString.of("(+NBT)"));
            tagLore.put("Lore", tagList);
            stack.setSubNbt("display", tagLore);
            stack.setSubNbt("BlockEntityTag", tag);
             */

        if (data != null) {
            if (data.getTypes().contains(DataComponentTypes.CUSTOM_NAME))
            {
                if (tag.contains("display"))
                {
                    NbtCompound nbt = tag.getCompound("display");
                    if (tag.contains("Name"))
                    {
                        MutableText dispName = Text.empty().append(Text.Serialization.fromJson(nbt.getString("Name"), registryManager));
                        if (nbt.contains("color", 99))
                        {
                            dispName.append(Text.translatable("item.color", String.format(Locale.ROOT, "#%06X", nbt.getInt("color"))).formatted(Formatting.GRAY));
                        }

                        // Overwrite existing data
                        stack.set(DataComponentTypes.CUSTOM_NAME, dispName);

                        Litematica.debugLog("storeTEInStack(): set custom display name: {}", dispName);
                    }
                    else
                    {
                        Litematica.debugLog("storeTEInStack(): ignoring empty Custom Display Name data {}", nbt.toString());
                    }
                }

                // Remove
                tag.remove("display");
            }
            if (data.getTypes().contains(DataComponentTypes.LORE))
            {
                if (tag.getType("Lore") == 9)
                {
                    NbtList loreNbt = tag.getList("Lore", 8);
                    List<Text> loreList = new ArrayList<>();

                    for (int i = 0; i < loreNbt.size(); ++i)
                    {
                        String ele = loreNbt.getString(i);

                        try
                        {
                            MutableText eleMutable = Text.Serialization.fromJson(ele, registryManager);
                            if (eleMutable != null)
                            {
                                // I am going to assume that the new Serialization sets the Text Style
                                loreList.add(eleMutable);
                            }
                        }
                        catch (Exception e)
                        {
                            Litematica.debugLog("storeTEInStack(): ignoring invalid Lore data {}", loreNbt.toString());
                            tag.remove("Lore");
                        }
                    }
                    LoreComponent loreComp = new LoreComponent(loreList);
                    if (!Objects.equals(data.get(DataComponentTypes.LORE), loreComp))
                    {
                        stack.set(DataComponentTypes.LORE, loreComp);

                        Litematica.debugLog("storeTEInStack(): set Lore data {}", loreComp.toString());
                    }
                }
                // Remove
                tag.remove("Lore");
            }
            // TODO Add more Data types here as needed
            if (data.getTypes().contains(DataComponentTypes.BLOCK_ENTITY_DATA))
            {
                NbtComponent entityData = NbtComponent.of(tag);

                // Overwrite existing data
                stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, entityData);

                Litematica.debugLog("storeTEInStack(): set block entity data: {}", entityData.toString());
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

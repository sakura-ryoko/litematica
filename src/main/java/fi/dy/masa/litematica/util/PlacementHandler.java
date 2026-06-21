package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntityDataManager;

public class PlacementHandler
{
    public static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
            // BooleanProperty:
            // INVERTED - DaylightDetector
            // OPEN - Barrel, Door, FenceGate, Trapdoor
            // PERSISTENT - Leaves (Disabled)
            BlockStateProperties.INVERTED,
            BlockStateProperties.OPEN,
            //Properties.PERSISTENT,

            // EnumProperty:
            // ATTACHMENT - Bells
            // AXIS - Pillar
            // BLOCK_HALF - Stairs, Trapdoor
            // BLOCK_FACE - Button, Grindstone, Lever
            // CHEST_TYPE - Chest
            // COMPARATOR_MODE - Comparator
            // DOOR_HINGE - Door
            // ORIENTATION - Crafter
            // RAIL_SHAPE / STRAIGHT_RAIL_SHAPE - Rails
            // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
            // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
            // BLOCK_FACE - Button, Grindstone, Lever
            BlockStateProperties.BELL_ATTACHMENT,
            BlockStateProperties.AXIS,
            BlockStateProperties.HALF,
            BlockStateProperties.ATTACH_FACE,
            BlockStateProperties.CHEST_TYPE,
            BlockStateProperties.MODE_COMPARATOR,
            BlockStateProperties.DOOR_HINGE,
            BlockStateProperties.FACING,
            BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.ORIENTATION,
            BlockStateProperties.RAIL_SHAPE,
            BlockStateProperties.RAIL_SHAPE_STRAIGHT,
            BlockStateProperties.SLAB_TYPE,
            BlockStateProperties.STAIRS_SHAPE,
			BlockStateProperties.COPPER_GOLEM_POSE,

            // IntProperty:
            // BITES - Cake
            // DELAY - Repeater
            // NOTE - NoteBlock
            // ROTATION - Banner, Sign, Skull
            BlockStateProperties.BITES,
            BlockStateProperties.DELAY,
            BlockStateProperties.NOTE,
            BlockStateProperties.ROTATION_16
    );

    /**
     * BlackList for Block States.  Entries here will be reset to their default value.
     */
    public static final ImmutableMap<Property<?>, ? extends Comparable<?>> BLACKLISTED_PROPERTIES = ImmutableMap.of(
            BlockStateProperties.WATERLOGGED,       Boolean.FALSE,
            BlockStateProperties.POWERED,           Boolean.FALSE
    );

    public static EasyPlaceProtocol getEffectiveProtocolVersion()
    {
        EasyPlaceProtocol protocol = (EasyPlaceProtocol) Configs.Generic.EASY_PLACE_PROTOCOL.getOptionListValue();

        if (protocol == EasyPlaceProtocol.AUTO)
        {
            if (Minecraft.getInstance().isLocalServer() ||
                EntityDataManager.getInstance().hasServuxServer() ||
                DataManager.hasServuxServer())
            {
                return EasyPlaceProtocol.V3;
            }

            if (DataManager.isCarpetServer())
            {
                return EasyPlaceProtocol.V2;
            }

            return EasyPlaceProtocol.SLAB_ONLY;
        }

        return protocol;
    }

    @Nullable
    public static BlockState applyPlacementProtocolToPlacementState(BlockState state, UseContext context)
    {
        EasyPlaceProtocol protocol = getEffectiveProtocolVersion();

        if (protocol == EasyPlaceProtocol.V3)
        {
            return applyPlacementProtocolV3(state, context);
        }
        else if (protocol == EasyPlaceProtocol.V2)
        {
            return applyPlacementProtocolV2(state, context);
        }
        else
        {
            return state;
        }
    }

    public static BlockState applyPlacementProtocolV2(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.hitVec().x - (double) context.pos().getX()) - 2;

        if (protocolValue < 0)
        {
            return state;
        }

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        if (property.isPresent())
        {
            //System.out.printf("[PHv2] applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, property.get().getName());
            state = applyDirectionProperty(state, context, property.get(), protocolValue);

            if (state == null)
            {
                return null;
            }
        }
        else if (state.hasProperty(BlockStateProperties.AXIS))
        {
            Direction.Axis axis = Direction.Axis.VALUES[((protocolValue >> 1) & 0x3) % 3];
            //System.out.printf("[PHv2] applying: 0x%08X (Axis -> %s)\n", protocolValue, axis.getName());

            if (BlockStateProperties.AXIS.getPossibleValues().contains(axis))
            {
                state = state.setValue(BlockStateProperties.AXIS, axis);
                //System.out.printf("[PHv2] axis stateOut: %s\n", state.toString());
            }
        }

        // Divide by two, and then remove the 4 bits used for the facing
        protocolValue >>>= 5;

        if (protocolValue > 0)
        {
            Block block = state.getBlock();

            if (block instanceof RepeaterBlock)
            {
                Integer delay = protocolValue;

                if (RepeaterBlock.DELAY.getPossibleValues().contains(delay))
                {
                    state = state.setValue(RepeaterBlock.DELAY, delay);
                }
            }
            else if (block instanceof ComparatorBlock)
            {
                state = state.setValue(ComparatorBlock.MODE, ComparatorMode.SUBTRACT);
            }
        }

        if (state.hasProperty(BlockStateProperties.HALF))
        {
            state = state.setValue(BlockStateProperties.HALF, protocolValue > 0 ? Half.TOP : Half.BOTTOM);
        }
        
        //System.out.printf("[PHv2] stateOut: %s\n", state.toString());

        return state;
    }

    public static <T extends Comparable<T>> BlockState applyPlacementProtocolV3(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.hitVec().x - (double) context.pos().getX()) - 2;
        BlockState oldState = state;
        //System.out.printf("[PHv3] hit vec.x %s, pos.x: %s\n", context.hitVec().x(), context.pos().getX());
        //System.out.printf("[PHv3] raw protocol value in: 0x%08X\n", protocolValue);

        if (protocolValue < 0)
        {
            return oldState;
        }

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property.isPresent() && property.get() != BlockStateProperties.VERTICAL_DIRECTION)
        {
            //System.out.printf("[PHv3] applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, property.get().getName());
            state = applyDirectionProperty(state, context, property.get(), protocolValue);

            if (state == null)
            {
                return null;
            }

            if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
            {
                if (state.canSurvive(context.world(), context.pos()))
                {
                    //System.out.printf("[PHv3] validator passed for \"%s\"\n", property.get().getName());
                    oldState = state;
                }
                else
                {
                    //System.out.printf("[PHv3] validator failed for \"%s\"\n", property.get().getName());
                    state = oldState;
                }
            }
            else
            {
                oldState = state;
            }
            
            // Consume the bits used for the facing
            protocolValue >>>= 3;
        }

        // Consume the lowest unused bit
        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateDefinition().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                //System.out.printf("[PHv3] check property [%s], whitelisted [%s], blacklisted [%s]\n", p.getName(), WHITELISTED_PROPERTIES.contains(p), BLACKLISTED_PROPERTIES.contains(p));

                if (property.isPresent() && property.get().equals(p))
                {
                    //System.out.printf("[PHv3] skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                    continue;
                }
                else if (WHITELISTED_PROPERTIES.contains(p) &&
                        !BLACKLISTED_PROPERTIES.containsKey(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getPossibleValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = Mth.log2(Mth.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;

                    //System.out.printf("[PHv3] trying to apply valInd: %d, bits: %d, prot val: 0x%08X [Property %s]\n", valueIndex, requiredBits, protocolValue, prop.getName());

                    if (valueIndex >= 0 && valueIndex < list.size())
                    {
                        T value = list.get(valueIndex);

                        if (state.getValue(prop).equals(value) == false &&
                            value != SlabType.DOUBLE) // don't allow duping slabs by forcing a double slab via the protocol
                        {
                            //System.out.printf("[PHv3] applying \"%s\": %s\n", prop.getName(), value);
                            state = state.setValue(prop, value);

                            if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
                            {
                                if (state.canSurvive(context.world(), context.pos()))
                                {
                                    //System.out.printf("[PHv3] validator passed for \"%s\"\n", prop.getName());
                                    oldState = state;
                                }
                                else
                                {
                                    //System.out.printf("[PHv3] validator failed for \"%s\"\n", prop.getName());
                                    state = oldState;
                                }
                            }
                            else
                            {
                                oldState = state;
                            }
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
                else
                {
                    //System.out.printf("[PHv3] skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                }
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        // Strip Blacklisted properties, and use the Block's default state.
        // This needs to be done after the initial loop, or it breaks compatibility
        for (Property<?> p : BLACKLISTED_PROPERTIES.keySet())
        {
            if (state.hasProperty(p))
            {
                @SuppressWarnings("unchecked")
                Property<T> prop = (Property<T>) p;
//                BlockState def = state.getBlock().defaultBlockState();
                state = state.setValue(prop, (T) BLACKLISTED_PROPERTIES.get(p));
                //System.out.printf("[PHv3] blacklisted state [%s] found, setting value\n", prop.getName());
            }
        }

        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && (
            oldState.hasProperty(BlockStateProperties.WATERLOGGED) && oldState.getValue(BlockStateProperties.WATERLOGGED) ||
            (oldState.getFluidState() != null && oldState.getFluidState().getType().isSame(Fluids.WATER))
        ))
        {
            // Revert only if original state was waterlogged / Still Water already
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }

        if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
        {
            if (state.canSurvive(context.world(), context.pos()))
            {
                //System.out.printf("[PHv3] validator passed for \"%s\"\n", state);
                return state;
            }
            else
            {
                //System.out.printf("[PHv3] validator failed for \"%s\"\n", state);
                return null;
            }
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, UseContext context,
                                                     EnumProperty<Direction> property, int protocolValue)
    {
        Direction facingOrig = state.getValue(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) // the opposite of the normal facing requested
        {
            facing = facing.getOpposite();
        }
        else if (decodedFacingIndex >= 0 && decodedFacingIndex <= 5)
        {
            facing = Direction.from3DDataValue(decodedFacingIndex);

            if (property.getPossibleValues().contains(facing) == false)
            {
                facing = context.entity().getDirection().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (raw: %d, dec: %d)\n", facingOrig, facing, protocolValue, decodedFacingIndex);

        if (facing != facingOrig && property.getPossibleValues().contains(facing))
        {
            if (state.getBlock() instanceof BedBlock)
            {
                BlockPos headPos = context.pos.relative(facing);
                BlockPlaceContext ctx = context.itemPlacementContext();

                if (context.world().getBlockState(headPos).canBeReplaced(ctx) == false)
                {
                    return null;
                }
            }

            state = state.setValue(property, facing);
        }

        return state;
    }

    public record UseContext(Level world, BlockPos pos, Direction side, Vec3 hitVec, LivingEntity entity,
                             InteractionHand hand, @Nullable BlockPlaceContext itemPlacementContext)
        {

            /*
            public static UseContext of(World world, BlockPos pos, Direction side, Vec3d hitVec, LivingEntity entity, Hand hand)
            {
                return new UseContext(world, pos, side, hitVec, entity, hand, null);
            }
            */

            public static UseContext from(BlockPlaceContext ctx, InteractionHand hand)
            {
                Vec3 pos = ctx.getClickLocation();
                return new UseContext(ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace(), new Vec3(pos.x, pos.y, pos.z),
                                      ctx.getPlayer(), hand, ctx);
            }
        }
}

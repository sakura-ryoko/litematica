package fi.dy.masa.litematica.util.post_rewrite;

import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.mixin.post_rewrite.IMixinKeyBinding;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.post_rewrite.malilib.BlockUtils2;
import fi.dy.masa.litematica.util.post_rewrite.malilib.PlacementUtils;
import fi.dy.masa.litematica.util.post_rewrite.malilib.Registry;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class EasyPlaceUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static final HashMap<Block, Boolean> HAS_USE_ACTION_CACHE = new HashMap<>();

    private static boolean isHandling;
    private static boolean isFirstClickEasyPlace;
    private static boolean isFirstClickPlacementRestriction;

    public static boolean isHandling()
    {
        return isHandling;
    }

    public static void setHandling(boolean handling)
    {
        isHandling = handling;
    }

    public static void setIsFirstClick()
    {
        if (shouldDoEasyPlaceActions())
        {
            isFirstClickEasyPlace = true;
        }

        /*
        if (Configs.Test.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            isFirstClickPlacementRestriction = true;
        }
         */
    }

    private static boolean hasUseAction(Block block)
    {
        Boolean val = HAS_USE_ACTION_CACHE.get(block);

        if (val == null)
        {
            try
            {
                // TODO FIXME cross-MC-version fragile
                String name = Block.class.getSimpleName().equals("Block") ? "onUse": "a";
                Method method = block.getClass().getMethod(name, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, BlockHitResult.class);
                Method baseMethod = Block.class.getMethod(name, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, BlockHitResult.class);
                val = method.equals(baseMethod) == false;
            }
            catch (Exception e)
            {
                Litematica.logger.warn("EasyPlaceUtils: Failed to reflect method Block::onUse", e);
                val = false;
            }

            HAS_USE_ACTION_CACHE.put(block, val);
        }

        return val.booleanValue();
    }

    public static boolean shouldDoEasyPlaceActions()
    {
        /*
        return Configs.Test.EASY_PLACE_MODE.getBooleanValue() &&
               DataManager.getToolMode() != ToolMode.REBUILD &&
               Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld();
         */

        return false;
    }

    public static void easyPlaceOnUseTick()
    {
        InputUtil.Key useKey = ((IMixinKeyBinding) MinecraftClient.getInstance().options.useKey).litematica_getBoundKey();

        if (isHandling == false &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            shouldDoEasyPlaceActions() &&
            //Keys.isKeyDown(GameWrap.getOptions().keyBindUseItem.getKeyCode()))
            CompatUtils.isKeyHeld(useKey))
        {
            GuiBase.isAltDown();
            isHandling = true;
            handleEasyPlace();
            isHandling = false;
        }
    }

    public static boolean handleEasyPlaceWithMessage()
    {
        if (isHandling)
        {
            return false;
        }

        isHandling = true;
        ActionResult result = handleEasyPlace();
        isHandling = false;

        // Only print the warning message once per right click
        if (isFirstClickEasyPlace && result == ActionResult.FAIL)
        {
            //MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
            //MessageDispatcher.warning(1500).type(output).translate("litematica.message.easy_place_fail");

            // Lame hack-around due to missing MessageOutput / Dispatcher
            InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
        }

        isFirstClickEasyPlace = false;

        return result != ActionResult.PASS;
    }

    public static void onRightClickTail()
    {
        // If the click wasn't handled yet, handle it now.
        // This is only called when right clicking on air with an empty hand,
        // as in that case neither the processRightClickBlock nor the processRightClick method get called.
        if (isFirstClickEasyPlace)
        {
            handleEasyPlaceWithMessage();
        }
    }

    @Nullable
    private static BlockHitResult getTargetPosition(@Nullable RayTraceUtils.RayTraceWrapper traceWrapper)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos overriddenPos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        if (overriddenPos != null)
        {
            if (mc.player == null)
            {
                return null;
            }
            double reach = mc.player.getBlockInteractionRange();
            Entity entity = mc.getCameraEntity();
            BlockHitResult trace = RayTraceUtils.traceToPositions(Collections.singletonList(overriddenPos), entity, reach);
            BlockPos pos = overriddenPos;
            Vec3d hitPos;
            Direction side;

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                hitPos = trace.getPos();
                side = trace.getSide();
            }
            else
            {
                hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                side = Direction.UP;
            }

            return new BlockHitResult(hitPos, side, pos, false);
        }
        else if (traceWrapper != null && traceWrapper.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            //HitResult trace = traceWrapper.getBlockHitResult();
            return traceWrapper.getBlockHitResult();
        }

        return null;
    }

    @Nullable
    private static BlockHitResult getAdjacentClickPosition(final BlockPos targetPos)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (mc.player == null)
        {
            return null;
        }
        double reach = mc.player.getBlockInteractionRange();
        Entity entity = mc.getCameraEntity();
        if (entity == null)
        {
            return null;
        }
        HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(world, entity,false, reach);

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) traceVanilla;
            BlockPos posVanilla = blockHitResult.getBlockPos();

            // If there is a block in the world right behind the targeted schematic block, then use
            // that block as the click position
            if (PlacementUtils.isReplaceable(world, posVanilla, false) == false &&
                targetPos.equals(posVanilla.offset(blockHitResult.getSide())))
            {
                return new BlockHitResult(blockHitResult.getPos(), ((BlockHitResult) traceVanilla).getSide(), posVanilla, false);
            }
        }

        for (Direction side : Direction.values())
        {
            BlockPos posSide = targetPos.offset(side);

            if (PlacementUtils.isReplaceable(world, posSide, false) == false)
            {
                Vec3d hitPos = getHitPositionForSidePosition(posSide, side);
                //return HitPosition.of(posSide, hitPos, side.getOpposite());
                return new BlockHitResult(hitPos, side.getOpposite(), posSide, false);
            }
        }

        return null;
    }

    private static Vec3d getHitPositionForSidePosition(BlockPos posSide, Direction sideFromTarget)
    {
        Direction.Axis axis = sideFromTarget.getAxis();
        double x = posSide.getX() + 0.5 - sideFromTarget.getOffsetX() * 0.5;
        double y = posSide.getY() + (axis == Direction.Axis.Y ? (sideFromTarget == Direction.DOWN ? 1.0 : 0.0) : 0.0);
        double z = posSide.getZ() + 0.5 - sideFromTarget.getOffsetZ() * 0.5;

        return new Vec3d(x, y, z);
    }

    @Nullable
    private static BlockHitResult getClickPosition(BlockHitResult targetPosition,
                                                   BlockState stateSchematic,
                                                   BlockState stateClient)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof SlabBlock;

        if (isSlab)
        {
            return getClickPositionForSlab(targetPosition, stateSchematic, stateClient);
        }

        BlockPos targetBlockPos = targetPosition.getBlockPos();
        //boolean requireAdjacent = Configs.Test.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();
        boolean requireAdjacent = true;

        return requireAdjacent ? getAdjacentClickPosition(targetBlockPos) : targetPosition;
    }

    @Nullable
    private static BlockHitResult getClickPositionForSlab(BlockHitResult targetPosition,
                                                          BlockState stateSchematic,
                                                          BlockState stateClient)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        SlabBlock slab = (SlabBlock) stateSchematic.getBlock();
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        World worldClient = mc.world;
        boolean isDouble = stateSchematic.get(SlabBlock.TYPE).equals(SlabType.DOUBLE);

        if (isDouble)
        {
            if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient))
            {
                boolean isTop = stateClient.get(SlabBlock.TYPE) == SlabType.TOP;
                Direction side = isTop ? Direction.DOWN : Direction.UP;
                Vec3d hitPos = targetPosition.getPos();
                //return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), side);
                return new BlockHitResult(new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), side, targetBlockPos, false);
            }
            else if (PlacementUtils.isReplaceable(worldClient, targetBlockPos, true))
            {
                BlockHitResult pos = getClickPositionForSlabHalf(targetPosition, stateSchematic, false, worldClient);
                return pos != null ? pos : getClickPositionForSlabHalf(targetPosition, stateSchematic, true, worldClient);
            }
        }
        // Single slab required, so the target position must be replaceable
        else if (isDouble == false && PlacementUtils.isReplaceable(worldClient, targetBlockPos, true))
        {
            boolean isTop = stateSchematic.get(SlabBlock.TYPE) == SlabType.TOP;
            return getClickPositionForSlabHalf(targetPosition, stateSchematic, isTop, worldClient);
        }

        return null;
    }

    @Nullable
    private static BlockHitResult getClickPositionForSlabHalf(BlockHitResult targetPosition, BlockState stateSchematic, boolean isTop, World worldClient)
    {
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        //boolean requireAdjacent = Configs.Test.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();
        boolean requireAdjacent = true;

        // Can click on air blocks, check if the slab can be placed by clicking on the target position itself,
        // or if it's a fluid block, then the block above or below, depending on the half
        if (requireAdjacent == false)
        {
            Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
            boolean isReplaceable = PlacementUtils.isReplaceable(worldClient, targetBlockPos, false);

            if (isReplaceable)
            {
                BlockPos posOffset = targetBlockPos.offset(clickSide);
                BlockState stateSide = worldClient.getBlockState(posOffset);

                // Clicking on the target position itself does not create a double slab above or below, so just click on the position itself
                if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateSide) == false)
                {
                    Vec3d hitPos = targetPosition.getPos();
                    //return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), clickSide);
                    return new BlockHitResult(new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), clickSide, targetBlockPos, false);
                }
            }
            else if (worldClient.getBlockState(targetBlockPos).isLiquid())
            {
                // Can click on the compensated position without creating a double slab there
                if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSide.getOpposite(), worldClient))
                {
                    BlockPos pos = targetBlockPos.offset(clickSide.getOpposite());
                    Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    return new BlockHitResult(hitPos, clickSide, pos, false);
                }
            }
        }

        // Required to be clicking on an existing adjacent block,
        // or couldn't click on the target position itself without creating an adjacent double slab
        return getAdjacentClickPositionForSlab(targetBlockPos, stateSchematic, isTop, worldClient);
    }

    @Nullable
    private static BlockHitResult getAdjacentClickPositionForSlab(BlockPos targetBlockPos, BlockState stateSchematic, boolean isTop, World worldClient)
    {
        Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
        Direction clickSideOpposite = clickSide.getOpposite();
        BlockPos posSide = targetBlockPos.offset(clickSideOpposite);

        // Can click on the existing block above or below
        if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSideOpposite, worldClient))
        {
            //return HitPosition.of(posSide, getHitPositionForSidePosition(posSide, clickSideOpposite), clickSide);
            return new BlockHitResult(getHitPositionForSidePosition(posSide, clickSideOpposite), clickSide, posSide, false);
        }
        // Try the sides
        else
        {
            for (Direction side : Direction.values())
            {
                if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, side, worldClient))
                {
                    posSide = targetBlockPos.offset(side);
                    Vec3d hitPos = getHitPositionForSidePosition(posSide, side);
                    double y = isTop ? 0.9 : 0.1;
                    //return HitPosition.of(posSide, new Vec3d(hitPos.x, posSide.getY() + y, hitPos.z), side.getOpposite());
                    return new BlockHitResult(new Vec3d(hitPos.x, posSide.getY() + y, hitPos.z), side.getOpposite(), posSide, false);
                }
            }
        }

        return null;
    }

    private static boolean canClickOnAdjacentBlockToPlaceSingleSlabAt(BlockPos targetBlockPos, BlockState targetState, Direction side, World worldClient)
    {
        BlockPos posSide = targetBlockPos.offset(side);
        BlockState stateSide = worldClient.getBlockState(posSide);

        return PlacementUtils.isReplaceable(worldClient, posSide, false) == false &&
               (side.getAxis() != Direction.Axis.Y ||
                clientBlockIsSameMaterialSingleSlab(targetState, stateSide) == false
                || stateSide.get(SlabBlock.TYPE) != targetState.get(SlabBlock.TYPE));
    }

    private static ActionResult handleEasyPlace()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity entity = mc.getCameraEntity();
        ClientWorld world = mc.world;
        double reach = mc.player.getBlockInteractionRange();
        RayTraceUtils.RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(world, entity, reach, true, true, true);
        BlockHitResult targetPosition = getTargetPosition(traceWrapper);

        // No position override, and didn't ray trace to a schematic block
        if (targetPosition == null)
        {
            if (traceWrapper != null && traceWrapper.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.VANILLA_BLOCK)
            {
                return placementRestrictionInEffect() ? ActionResult.FAIL : ActionResult.PASS;
            }

            return ActionResult.PASS;
        }

        final BlockPos targetBlockPos = targetPosition.getBlockPos();
        World schematicWorld = SchematicWorldHandler.getSchematicWorld();
        BlockState stateSchematic = schematicWorld.getBlockState(targetBlockPos);
        BlockState stateClient = world.getBlockState(targetBlockPos);
        ItemStack requiredStack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

        // The block is correct already, or it was recently placed, or some of the checks failed
        if (stateSchematic == stateClient || requiredStack.isEmpty() ||
            easyPlaceIsPositionCached(targetBlockPos) ||
            canPlaceBlock(targetBlockPos, world, stateSchematic, stateClient) == false)
        {
            return ActionResult.FAIL;
        }

        BlockHitResult clickPosition = getClickPosition(targetPosition, stateSchematic, stateClient);
        Hand hand = PickBlockUtils.doPickBlockForStack(requiredStack);

        // Didn't find a valid or safe click position, or was unable to pick block
        if (clickPosition == null || hand == null)
        {
            return ActionResult.FAIL;
        }

        boolean isSlab = stateSchematic.getBlock() instanceof SlabBlock;
        boolean usingAdjacentClickPosition = clickPosition.getBlockPos().equals(targetBlockPos) == false;
        BlockPos clickPos = clickPosition.getBlockPos();
        Vec3d hitPos = clickPosition.getPos();
        Direction side = clickPosition.getSide();

        if (usingAdjacentClickPosition == false && isSlab == false)
        {
            side = applyPlacementFacing(stateSchematic, side, stateClient);

            // Fluid _blocks_ are not replaceable... >_>
            if (stateClient.canPlaceAt(world, targetBlockPos) == false &&
                stateClient.isLiquid())
            {
                clickPos = clickPos.offset(side, -1);
            }
        }

        if (isSlab == false)
        {
            hitPos = applyCarpetProtocolHitVec(clickPos, stateSchematic, hitPos);
        }

        //System.out.printf("targetPos: %s, clickPos: %s side: %s, hit: %s\n", targetBlockPos, clickPos, side, hitPos);
        stateClient = world.getBlockState(clickPos);
        boolean needsSneak = hasUseAction(stateClient.getBlock());
        boolean didFakeSneak = needsSneak && EntityUtils2.setFakedSneakingState(true);
        PlayerEntity player = mc.player;

        BlockHitResult hitResult = new BlockHitResult(hitPos, side, clickPos, false);

        //if (GameWrap.getInteractionManager().processRightClickBlock(player, world, clickPos, side.getVanillaDirection(), hitPos.toVanilla(), hand) == EnumActionResult.SUCCESS)
        if (mc.interactionManager.interactBlock(mc.player, hand, hitResult) == ActionResult.PASS)
        {
            // Mark that this position has been handled (use the non-offset position that is checked above)
            cacheEasyPlacePosition(targetBlockPos);

            if (Configs.Generic.EASY_PLACE_SWING_HAND.getBooleanValue())
            {
                player.swingHand(hand);
            }
            //GameWrap.getClient().entityRenderer.itemRenderer.resetEquippedProgress(hand);
            mc.getEntityRenderDispatcher().getHeldItemRenderer().resetEquipProgress(hand);

            if (isSlab && stateSchematic.get(SlabBlock.TYPE).equals(SlabType.DOUBLE))
            {
                stateClient = world.getBlockState(targetBlockPos);

                if (stateClient.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false)
                {
                    side = stateClient.get(SlabBlock.TYPE) == SlabType.TOP ? Direction.DOWN : Direction.UP;
                    hitPos = new Vec3d(targetBlockPos.getX(), targetBlockPos.getY() + 0.5, targetBlockPos.getZ());
                    //System.out.printf("slab - pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                    hitResult = new BlockHitResult(hitPos, side, targetBlockPos, false);
                    //GameWrap.getInteractionManager().processRightClickBlock(player, world, targetBlockPos, side.getVanillaDirection(), hitPos.toVanilla(), hand);
                    mc.interactionManager.interactBlock(mc.player, hand, hitResult);
                }
            }
        }

        if (didFakeSneak)
        {
            EntityUtils2.setFakedSneakingState(false);
        }

        return ActionResult.SUCCESS;
    }

    private static boolean clientBlockIsSameMaterialSingleSlab(BlockState stateSchematic, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if ((blockSchematic instanceof SlabBlock) &&
            (blockClient instanceof SlabBlock) &&
            stateClient.get(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false)
        {
            SlabType propSchematic = stateSchematic.get(SlabBlock.TYPE);
            SlabType propClient = stateClient.get(SlabBlock.TYPE);

            return propSchematic == propClient && stateSchematic.get(SlabBlock.TYPE) == stateClient.get(SlabBlock.TYPE);
        }

        return false;
    }

    private static boolean canPlaceBlock(BlockPos targetPos, World worldClient, BlockState stateSchematic, BlockState stateClient)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof SlabBlock;

        if (isSlab)
        {
            if (PlacementUtils.isReplaceable(worldClient, targetPos, true) == false &&
                (stateSchematic.get(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false
                || clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient) == false))
            {
                return false;
            }

            return true;
        }

        return PlacementUtils.isReplaceable(worldClient, targetPos, true);
    }

    private static Vec3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Optional<Direction> facingOptional = BlockUtils2.getFirstPropertyFacingValue(state);

        if (facingOptional.isPresent())
        {
            x = facingOptional.get().ordinal() + 2 + pos.getX();
        }

        if (block instanceof RepeaterBlock)
        {
            x += ((state.get(RepeaterBlock.DELAY)) - 1) * 10;
        }
        else if (block instanceof TrapdoorBlock && state.get(TrapdoorBlock.HALF) == BlockHalf.TOP)
        {
            x += 10;
        }
        else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            x += 10;
        }
        else if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == BlockHalf.TOP)
        {
            x += 10;
        }

        return new Vec3d(x, y, z);
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Optional<EnumProperty<Direction>> propOptional = BlockUtils2.getFirstDirectionProperty(stateSchematic);

        if (propOptional.isPresent())
        {
            side = stateSchematic.get(propOptional.get()).getOpposite();
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     */
    public static boolean handlePlacementRestriction()
    {
        boolean cancel = placementRestrictionInEffect();

        if (cancel && isFirstClickPlacementRestriction)
        {
            //MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
            //MessageDispatcher.warning(1000).type(output).translate("litematica.message.placement_restriction_fail");

            InfoUtils.printActionbarMessage("litematica.message.placement_restriction_fail");
        }

        isFirstClickPlacementRestriction = false;

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity entity = mc.getCameraEntity();
        World world = mc.world;
        double reach = mc.player.getBlockInteractionRange();
        HitResult trace = RayTraceUtils.getRayTraceFromEntity(world, entity, false, reach);

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState stateClient = world.getBlockState(pos);

            if (stateClient.canPlaceAt(world, pos) == false)
            {
                pos = pos.offset(blockHitResult.getSide());
                stateClient = world.getBlockState(pos);
            }

            // The targeted position is far enough from any schematic sub-regions to not need handling
            if (isPositionWithinRangeOfSchematicRegions(pos, 2) == false)
            {
                return false;
            }

            // Placement position is already occupied
            if (stateClient.canPlaceAt(world, pos) == false &&
                stateClient.isLiquid() == false)
            {
                return true;
            }

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();

            // The targeted position should be air or it's outside the current render range
            if (worldSchematic.isAir(pos) || range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            return stack.isEmpty() || EntityUtils2.getUsedHandForItem(mc.player, stack, true) == null;
        }

        return false;
    }

    private static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();

        final int minCX = (pos.getX() - range) >> 4;
        final int minCY = (pos.getY() - range) >> 4;
        final int minCZ = (pos.getZ() - range) >> 4;
        final int maxCX = (pos.getX() + range) >> 4;
        final int maxCY = (pos.getY() + range) >> 4;
        final int maxCZ = (pos.getZ() + range) >> 4;
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int cy = minCY; cy <= maxCY; ++cy)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                for (int cx = minCX; cx <= maxCX; ++cx)
                {
                    List<SchematicPlacementManager.PlacementPart> parts = manager.getPlacementPartsInChunk(cx, cz);

                    for (SchematicPlacementManager.PlacementPart part : parts)
                    {
                        IntBoundingBox box = part.bb;

                        if (x >= box.minX - range && x <= box.maxX + range &&
                            y >= box.minY - range && y <= box.maxY + range &&
                            z >= box.minZ - range && z <= box.maxZ + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos)
    {
        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), 2000000000));
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }
}

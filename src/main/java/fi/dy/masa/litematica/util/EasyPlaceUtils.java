package fi.dy.masa.litematica.util;

import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.MessageOutputType;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.game.PlacementUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

/**
 * Post Re-Write Code
 */
public class EasyPlaceUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static final HashMap<Block, Boolean> HAS_USE_ACTION_CACHE = new HashMap<>();

    private static boolean isHandling;
    private static boolean isFirstClickEasyPlace;
    private static boolean isFirstClickPlacementRestriction;
	private static long easyPlaceLastPickBlockTime = System.nanoTime();

	public static boolean isHandling()
    {
        return isHandling;
    }

    public static void setHandling(boolean handling)
    {
        isHandling = handling;
    }

	public static double getValidBlockRange(Minecraft mc)
	{
		return Configs.Generic.EASY_PLACE_VANILLA_REACH.getBooleanValue() ? mc.player.blockInteractionRange() : mc.player.blockInteractionRange() + 1.0;
	}

	public static void setIsFirstClick()
    {
        if (shouldDoEasyPlaceActions())
        {
            isFirstClickEasyPlace = true;
        }

        if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            isFirstClickPlacementRestriction = true;
        }
    }

    private static boolean hasUseAction(Block block)
    {
        Boolean val = HAS_USE_ACTION_CACHE.get(block);

        if (val == null)
        {
            try
            {
                // TODO FIXME cross-MC-version fragile
                String name = Block.class.getSimpleName().equals("Block") ? "useWithoutItem": "a";
                Method method = block.getClass().getMethod(name, BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class);
                Method baseMethod = Block.class.getMethod(name, BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class);
                val = method.equals(baseMethod) == false;
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("EasyPlaceUtils: Failed to reflect method Block::useWithoutItem", e);
                val = false;
            }

            HAS_USE_ACTION_CACHE.put(block, val);
        }

        return val.booleanValue();
    }

    public static boolean shouldDoEasyPlaceActions()
    {
        return Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
				Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue() &&
				GameWrap.getClientPlayer() != null &&
				DataManager.getToolMode() != ToolMode.REBUILD &&
				Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld();
    }

    public static void easyPlaceOnUseTick()
    {
//        InputUtil.Key useKey = ((IMixinKeyBinding) MinecraftClient.getInstance().options.useKey).litematica_getBoundKey();

        if (isHandling == false &&
			Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            shouldDoEasyPlaceActions() &&
//            Keys.isKeyDown(GameWrap.getOptions().keyBindUseItem.getKeyCode()))
//            CompatUtils.isKeyHeld(useKey))
//			MinecraftClient.getInstance().options.useKey.isPressed()
			Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()
		)
        {
            isHandling = true;
            handleEasyPlace();
            isHandling = false;
        }
    }

    public static boolean handleEasyPlaceWithMessage()
    {
        if (isHandling())
        {
            return false;
        }

		isHandling = true;
		InteractionResult result = handleEasyPlace();
		isHandling = false;
//		System.out.printf("handleEasyPlaceWithMessage() --> %s (%s)\n", result != ActionResult.PASS, result.toString());

		// Only print the warning message once per right click
		if (isFirstClickEasyPlace && result == InteractionResult.FAIL)
		{
			MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();
			//MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
			//MessageDispatcher.warning(1500).type(output).translate("litematica.message.easy_place_fail");

			if (type == MessageOutputType.MESSAGE)
			{
				InfoUtils.showInGameMessage(Message.MessageType.WARNING, "litematica.message.easy_place_fail");
			}
			else if (type == MessageOutputType.ACTIONBAR)
			{
                InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
			}

			isFirstClickEasyPlace = false;

			return true;
		}

		isFirstClickEasyPlace = false;

		return result != InteractionResult.PASS;
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
        Minecraft mc = Minecraft.getInstance();
        BlockPos overriddenPos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        if (overriddenPos != null)
        {
            if (mc.player == null)
            {
                return null;
            }
            double reach = mc.player.blockInteractionRange();
            Entity entity = mc.getCameraEntity();
            BlockHitResult trace = RayTraceUtils.traceToPositions(Collections.singletonList(overriddenPos), entity, reach);
            BlockPos pos = overriddenPos;
            Vec3 hitPos;
            Direction side;

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                hitPos = trace.getLocation();
                side = trace.getDirection();
            }
            else
            {
                hitPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
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
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        if (mc.player == null)
        {
            return null;
        }
        double reach = mc.player.blockInteractionRange();
        Entity entity = mc.getCameraEntity();
        if (entity == null || world == null)
        {
            return null;
        }
        HitResult traceVanilla = fi.dy.masa.malilib.util.game.RayTraceUtils.getRayTraceFromEntity(world, entity, ClipContext.Fluid.NONE, false, reach);

        if (traceVanilla == null)
        {
            return null;
        }

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) traceVanilla;
            BlockPos posVanilla = blockHitResult.getBlockPos();

            // If there is a block in the world right behind the targeted schematic block, then use
            // that block as the click position
            if (PlacementUtils.isReplaceable(world, posVanilla, false) == false &&
                targetPos.equals(posVanilla.relative(blockHitResult.getDirection())))
            {
                return new BlockHitResult(blockHitResult.getLocation(), ((BlockHitResult) traceVanilla).getDirection(), posVanilla, false);
            }
        }

        for (Direction side : Direction.values())
        {
            BlockPos posSide = targetPos.relative(side);

            if (PlacementUtils.isReplaceable(world, posSide, false) == false)
            {
                Vec3 hitPos = getHitPositionForSidePosition(posSide, side);
                //return HitPosition.of(posSide, hitPos, side.getOpposite());
                return new BlockHitResult(hitPos, side.getOpposite(), posSide, false);
            }
        }

        return null;
    }

    private static Vec3 getHitPositionForSidePosition(BlockPos posSide, Direction sideFromTarget)
    {
        Direction.Axis axis = sideFromTarget.getAxis();
        double x = posSide.getX() + 0.5 - sideFromTarget.getStepX() * 0.5;
        double y = posSide.getY() + (axis == Direction.Axis.Y ? (sideFromTarget == Direction.DOWN ? 1.0 : 0.0) : 0.0);
        double z = posSide.getZ() + 0.5 - sideFromTarget.getStepZ() * 0.5;

        return new Vec3(x, y, z);
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
        boolean requireAdjacent = Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();
//        boolean requireAdjacent = false;

        return requireAdjacent ? getAdjacentClickPosition(targetBlockPos) : targetPosition;
    }

    @Nullable
    private static BlockHitResult getClickPositionForSlab(BlockHitResult targetPosition,
                                                          BlockState stateSchematic,
                                                          BlockState stateClient)
    {
        Minecraft mc = Minecraft.getInstance();
        SlabBlock slab = (SlabBlock) stateSchematic.getBlock();
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        Level worldClient = mc.level;
        boolean isDouble = stateSchematic.getValue(SlabBlock.TYPE).equals(SlabType.DOUBLE);

        if (isDouble)
        {
            if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient))
            {
                boolean isTop = stateClient.getValue(SlabBlock.TYPE) == SlabType.TOP;
                Direction side = isTop ? Direction.DOWN : Direction.UP;
                Vec3 hitPos = targetPosition.getLocation();
                //return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), side);
                return new BlockHitResult(new Vec3(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), side, targetBlockPos, false);
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
            boolean isTop = stateSchematic.getValue(SlabBlock.TYPE) == SlabType.TOP;
            return getClickPositionForSlabHalf(targetPosition, stateSchematic, isTop, worldClient);
        }

        return null;
    }

    @Nullable
    private static BlockHitResult getClickPositionForSlabHalf(BlockHitResult targetPosition, BlockState stateSchematic, boolean isTop, Level worldClient)
    {
        BlockPos targetBlockPos = targetPosition.getBlockPos();
        boolean requireAdjacent = Configs.Generic.EASY_PLACE_CLICK_ADJACENT.getBooleanValue();
//        boolean requireAdjacent = false;

        // Can click on air blocks, check if the slab can be placed by clicking on the target position itself,
        // or if it's a fluid block, then the block above or below, depending on the half
        if (requireAdjacent == false)
        {
            Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
            boolean isReplaceable = PlacementUtils.isReplaceable(worldClient, targetBlockPos, false);

            if (isReplaceable)
            {
                BlockPos posOffset = targetBlockPos.relative(clickSide);
                BlockState stateSide = worldClient.getBlockState(posOffset);

                // Clicking on the target position itself does not create a double slab above or below, so just click on the position itself
                if (clientBlockIsSameMaterialSingleSlab(stateSchematic, stateSide) == false)
                {
                    Vec3 hitPos = targetPosition.getLocation();
                    //return HitPosition.of(targetBlockPos, new Vec3d(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), clickSide);
                    return new BlockHitResult(new Vec3(hitPos.x, targetBlockPos.getY() + 0.5, hitPos.z), clickSide, targetBlockPos, false);
                }
            }
            else if (worldClient.getBlockState(targetBlockPos).liquid())
            {
                // Can click on the compensated position without creating a double slab there
                if (canClickOnAdjacentBlockToPlaceSingleSlabAt(targetBlockPos, stateSchematic, clickSide.getOpposite(), worldClient))
                {
                    BlockPos pos = targetBlockPos.relative(clickSide.getOpposite());
                    Vec3 hitPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    return new BlockHitResult(hitPos, clickSide, pos, false);
                }
            }
        }

        // Required to be clicking on an existing adjacent block,
        // or couldn't click on the target position itself without creating an adjacent double slab
        return getAdjacentClickPositionForSlab(targetBlockPos, stateSchematic, isTop, worldClient);
    }

    @Nullable
    private static BlockHitResult getAdjacentClickPositionForSlab(BlockPos targetBlockPos, BlockState stateSchematic, boolean isTop, Level worldClient)
    {
        Direction clickSide = isTop ? Direction.DOWN : Direction.UP;
        Direction clickSideOpposite = clickSide.getOpposite();
        BlockPos posSide = targetBlockPos.relative(clickSideOpposite);

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
                    posSide = targetBlockPos.relative(side);
                    Vec3 hitPos = getHitPositionForSidePosition(posSide, side);
                    double y = isTop ? 0.9 : 0.1;
                    //return HitPosition.of(posSide, new Vec3d(hitPos.x, posSide.getY() + y, hitPos.z), side.getOpposite());
                    return new BlockHitResult(new Vec3(hitPos.x, posSide.getY() + y, hitPos.z), side.getOpposite(), posSide, false);
                }
            }
        }

        return null;
    }

    private static boolean canClickOnAdjacentBlockToPlaceSingleSlabAt(BlockPos targetBlockPos, BlockState targetState, Direction side, Level worldClient)
    {
        BlockPos posSide = targetBlockPos.relative(side);
        BlockState stateSide = worldClient.getBlockState(posSide);

        return PlacementUtils.isReplaceable(worldClient, posSide, false) == false &&
               (side.getAxis() != Direction.Axis.Y ||
                clientBlockIsSameMaterialSingleSlab(targetState, stateSide) == false
                || stateSide.getValue(SlabBlock.TYPE) != targetState.getValue(SlabBlock.TYPE));
    }

    private static InteractionResult handleEasyPlace()
    {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.getCameraEntity();
        ClientLevel world = mc.level;
        double reach = getValidBlockRange(mc);
        RayTraceUtils.RayTraceWrapper traceWrapper;

	    if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue())
	    {
		    boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
		    traceWrapper = RayTraceUtils.getGenericTrace(world, entity, reach, true, targetFluids, false);
	    }
		else
	    {
		    traceWrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.level, mc.player, reach);

		    if (traceWrapper == null && placementRestrictionInEffect())
		    {
			    return InteractionResult.FAIL;
		    }
	    }

		BlockHitResult targetPosition = getTargetPosition(traceWrapper);

		// No position override, and didn't ray trace to a schematic block
		if (targetPosition == null)
		{
			if (traceWrapper != null && traceWrapper.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.VANILLA_BLOCK)
			{
				return placementRestrictionInEffect() ? InteractionResult.FAIL : InteractionResult.PASS;
			}

			return InteractionResult.PASS;
		}

		final BlockPos targetBlockPos = targetPosition.getBlockPos();
		Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
		BlockState stateSchematic = schematicWorld.getBlockState(targetBlockPos);
		BlockState stateClient = world.getBlockState(targetBlockPos);
		ItemStack requiredStack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic, schematicWorld, targetBlockPos);

		if (stateSchematic.is(BlockTags.AIR))
		{
			return InteractionResult.FAIL;
		}

		// The block is correct already, or it was recently placed, or some of the checks failed
		if (stateSchematic == stateClient || requiredStack.isEmpty() ||
			easyPlaceIsPositionCached(targetBlockPos) ||
			easyPlaceIsTooFast() ||
			canPlaceBlock(targetBlockPos, world, stateSchematic, stateClient) == false)
		{
			return InteractionResult.FAIL;
		}

		BlockHitResult clickPosition = getClickPosition(targetPosition, stateSchematic, stateClient);
		// TODO -- POST-REWRITE CODE (Broken?)
//        Hand hand = PickBlockUtils.doPickBlockForStack(requiredStack);

		// *** ADDED Easy Place Code from Pre-Rewrite ***
		InventoryUtils.schematicWorldPickBlock(requiredStack, targetBlockPos, world, mc);
		InteractionHand hand = EntityUtils.getUsedHandForItem(mc.player, requiredStack);

		// Didn't find a valid or safe click position, or was unable to pick block
		if (clickPosition == null || hand == null)
		{
			return InteractionResult.FAIL;
		}

		// *** ADDED Easy Place Code from Pre-Rewrite ***
		// Already placed to that position, possible server sync delay
//		if (EasyPlaceUtils.easyPlaceIsPositionCached(targetBlockPos))
//		{
//			return InteractionResult.FAIL;
//		}
//
//		// *** ADDED Easy Place Code from Pre-Rewrite ***
//		// Ignore action if too fast
//		if (EasyPlaceUtils.easyPlaceIsTooFast())
//		{
//			return InteractionResult.FAIL;
//		}

		boolean isSlab = stateSchematic.getBlock() instanceof SlabBlock;
		boolean usingAdjacentClickPosition = clickPosition.getBlockPos().equals(targetBlockPos) == false;
		BlockPos clickPos = clickPosition.getBlockPos();
		Vec3 hitPos = clickPosition.getLocation();
		Direction side = clickPosition.getDirection();
		Direction sideOrig = targetPosition.getDirection();

		// TODO -- POST-REWRITE CODE (No rotations?)
		// *** ADDED Easy Place Code from Pre-Rewrite for rotations ***
		EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
		double traceMaxRange = Configs.Generic.EASY_PLACE_VANILLA_REACH.getBooleanValue() ? 4.5 : 6;
		HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.level, mc.player, false, traceMaxRange);

		if (protocol == EasyPlaceProtocol.NONE || protocol == EasyPlaceProtocol.SLAB_ONLY)
		{
			// If there is a block in the world right behind the targeted schematic block, then use
			// that block as the click position
			if (traceVanilla != null && traceVanilla.getType() == HitResult.Type.BLOCK)
			{
				BlockHitResult hitResult = (BlockHitResult) traceVanilla;
				BlockPos posVanilla = hitResult.getBlockPos();
				Direction sideVanilla = hitResult.getDirection();
				BlockState stateVanilla = mc.level.getBlockState(posVanilla);
				Vec3 hit = traceVanilla.getLocation();
				BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(mc.player, hand, hitResult));

				if (stateVanilla.canBeReplaced(ctx) == false)
				{
					posVanilla = posVanilla.relative(sideVanilla);

					if (targetBlockPos.equals(posVanilla))
					{
						hitPos = hit;
						sideOrig = sideVanilla;
					}
				}
			}
		}

		// TODO -- POST-REWRITE CODE (No rotations?)
		if (usingAdjacentClickPosition == false && isSlab == false)
		{
			side = WorldUtils.applyPlacementFacing(stateSchematic, side, stateClient);

			// Fluid _blocks_ are not replaceable... >_>
			if (stateClient.canSurvive(world, targetBlockPos) == false &&
				stateClient.liquid())
			{
				clickPos = clickPos.relative(side, -1);
			}
		}

		// TODO -- POST-REWRITE CODE (No rotations?)
		// *** ADDED Easy Place Code from Pre-Rewrite for rotations ***
		// Support for special cases
		WorldUtils.PlacementProtocolData placementData = WorldUtils.applyPlacementProtocolAll(clickPos, stateSchematic, hitPos);
//		BlockPos pos = targetBlockPos;

		if (placementData.mustFail)
		{
			return InteractionResult.FAIL; //disallowed cases (e.g. trying to place torch with no support block)
		}

		if (placementData.handled)
		{
			clickPos = placementData.pos;
			side = placementData.side;
			hitPos = placementData.hitVec;
		}

		// TODO -- POST-REWRITE CODE (No rotations?)
//        if (isSlab == false)
//        {
//            hitPos = applyCarpetProtocolHitVec(clickPos, stateSchematic, hitPos);
//        }

		// TODO --> Move V3 / V2 / Slab handling to EasyPlaceUtils.
		if (protocol == EasyPlaceProtocol.V3)
		{
			hitPos = WorldUtils.applyPlacementProtocolV3(clickPos, stateSchematic, hitPos);
		}
		else if (protocol == EasyPlaceProtocol.V2 && isSlab == false)
		{
			// Carpet Accurate Block Placement protocol support, plus slab support
			hitPos = WorldUtils.applyCarpetProtocolHitVec(clickPos, stateSchematic, hitPos);
		}
		else if (protocol == EasyPlaceProtocol.SLAB_ONLY)
		{
			// Slab support only
			hitPos = WorldUtils.applyBlockSlabProtocol(clickPos, stateSchematic, hitPos);
		}

		// TODO -- POST-REWRITE CODE (No rotations?)
		//System.out.printf("targetPos: %s, clickPos: %s side: %s, hit: %s\n", targetBlockPos, clickPos, side, hitPos);
		stateClient = world.getBlockState(clickPos);
		boolean needsSneak = hasUseAction(stateClient.getBlock());
		boolean didFakeSneak = needsSneak && EntityUtils.setFakedSneakingState(true);
		Player player = mc.player;

		// Mark that this position has been handled (use the non-offset position that is checked above)
		cacheEasyPlacePosition(clickPos);

		BlockHitResult hitResult = new BlockHitResult(hitPos, side, clickPos, false);

		//if (GameWrap.getInteractionManager().processRightClickBlock(player, world, clickPos, side.getVanillaDirection(), hitPos.toVanilla(), hand) == EnumActionResult.SUCCESS)
		InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hitResult);

		if (result == InteractionResult.PASS)
		{
			if (InteractionResult.SUCCESS.swingSource().equals(InteractionResult.SwingSource.CLIENT) &&
				Configs.Generic.EASY_PLACE_SWING_HAND.getBooleanValue())
			{
				player.swing(hand);
			}
			//GameWrap.getClient().entityRenderer.itemRenderer.resetEquippedProgress(hand);
			mc.getEntityRenderDispatcher().getItemInHandRenderer().itemUsed(hand);

			if (isSlab && stateSchematic.getValue(SlabBlock.TYPE).equals(SlabType.DOUBLE))
			{
				stateClient = world.getBlockState(clickPos);

				if (stateClient.getBlock() instanceof SlabBlock && stateClient.getValue(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false)
				{
					// TODO -- POST-REWRITE CODE (No rotations?)
//                    side = stateClient.get(SlabBlock.TYPE) == SlabType.TOP ? Direction.DOWN : Direction.UP;
//                    hitPos = new Vec3d(targetBlockPos.getX(), targetBlockPos.getY() + 0.5, targetBlockPos.getZ());
//                    //System.out.printf("slab - pos: %s side: %s, hit: %s\n", pos, side, hitPos);
//                    hitResult = new BlockHitResult(hitPos, side, targetBlockPos, false);
//                    //GameWrap.getInteractionManager().processRightClickBlock(player, world, targetBlockPos, side.getVanillaDirection(), hitPos.toVanilla(), hand);
//                    mc.interactionManager.interactBlock(mc.player, hand, hitResult);

					if (stateClient.getBlock() instanceof SlabBlock && stateClient.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
					{
						side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
						hitResult = new BlockHitResult(hitPos, side, clickPos, false);
						mc.gameMode.useItemOn(mc.player, hand, hitResult);
					}
				}
			}

			if (didFakeSneak)
			{
				EntityUtils.setFakedSneakingState(false);
			}

			return InteractionResult.SUCCESS;
		}

        return InteractionResult.PASS;
    }

    private static boolean clientBlockIsSameMaterialSingleSlab(BlockState stateSchematic, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if ((blockSchematic instanceof SlabBlock) &&
            (blockClient instanceof SlabBlock) &&
            stateClient.getValue(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false)
        {
            SlabType propSchematic = stateSchematic.getValue(SlabBlock.TYPE);
            SlabType propClient = stateClient.getValue(SlabBlock.TYPE);

            return propSchematic == propClient && stateSchematic.getValue(SlabBlock.TYPE) == stateClient.getValue(SlabBlock.TYPE);
        }

        return false;
    }

    private static boolean canPlaceBlock(BlockPos targetPos, Level worldClient, BlockState stateSchematic, BlockState stateClient)
    {
        boolean isSlab = stateSchematic.getBlock() instanceof SlabBlock;

        if (isSlab)
        {
            if (PlacementUtils.isReplaceable(worldClient, targetPos, true) == false &&
                (stateSchematic.getValue(SlabBlock.TYPE).equals(SlabType.DOUBLE) == false
                || clientBlockIsSameMaterialSingleSlab(stateSchematic, stateClient) == false))
            {
                return false;
            }

            return true;
        }

        return PlacementUtils.isReplaceable(worldClient, targetPos, true);
    }

    private static Vec3 applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3 hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Optional<Direction> facingOptional = BlockUtils.getFirstPropertyFacingValue(state);

        if (facingOptional.isPresent())
        {
            x = facingOptional.get().ordinal() + 2 + pos.getX();
        }

        if (block instanceof RepeaterBlock)
        {
            x += ((state.getValue(RepeaterBlock.DELAY)) - 1) * 10;
        }
        else if (block instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.HALF) == Half.TOP)
        {
            x += 10;
        }
        else if (block instanceof ComparatorBlock && state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            x += 10;
        }
        else if (block instanceof StairBlock && state.getValue(StairBlock.HALF) == Half.TOP)
        {
            x += 10;
        }

        return new Vec3(x, y, z);
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Optional<EnumProperty<Direction>> propOptional = BlockUtils.getFirstDirectionProperty(stateSchematic);

        if (propOptional.isPresent())
        {
            side = stateSchematic.getValue(propOptional.get()).getOpposite();
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
	        MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();
            //MessageOutput output = Configs.InfoOverlays.EASY_PLACE_WARNINGS.getValue();
            //MessageDispatcher.warning(1000).type(output).translate("litematica.message.placement_restriction_fail");

	        if (type ==  MessageOutputType.MESSAGE)
	        {
		        InfoUtils.showInGameMessage(Message.MessageType.WARNING, "litematica.message.placement_restriction_fail");
	        }
			else if (type == MessageOutputType.ACTIONBAR)
	        {
		        InfoUtils.printActionbarMessage("litematica.message.placement_restriction_fail");
	        }
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
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.getCameraEntity();
        Level world = mc.level;

        if (world == null || entity == null || mc.player == null)
        {
            return false;
        }

        double reach = mc.player.blockInteractionRange();
        HitResult trace = fi.dy.masa.malilib.util.game.RayTraceUtils.getRayTraceFromEntity(world, entity, ClipContext.Fluid.NONE, false, reach);

        if (trace == null)
        {
            return false;
        }

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState stateClient = world.getBlockState(pos);

            if (stateClient.canSurvive(world, pos) == false)
            {
                pos = pos.relative(blockHitResult.getDirection());
                stateClient = world.getBlockState(pos);
            }

            // The targeted position is far enough from any schematic sub-regions to not need handling
            if (isPositionWithinRangeOfSchematicRegions(pos, 2) == false)
            {
                return false;
            }

            // Placement position is already occupied
            if (stateClient.canSurvive(world, pos) == false &&
                stateClient.liquid() == false)
            {
                return true;
            }

            Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();

            // The targeted position should be air or it's outside the current render range
            if (worldSchematic.isEmptyBlock(pos) || range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic, worldSchematic, pos);

            // The player is holding the wrong item for the targeted position
            return stack.isEmpty() || EntityUtils.getUsedHandForItem(mc.player, stack, true) == null;
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

                        if (x >= box.minX() - range && x <= box.maxX() + range &&
                            y >= box.minY() - range && y <= box.maxY() + range &&
                            z >= box.minZ() - range && z <= box.maxZ() + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    protected static boolean easyPlaceIsPositionCached(BlockPos pos)
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

	protected static void cacheEasyPlacePosition(BlockPos pos)
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

	protected static boolean easyPlaceIsTooFast()
	{
		return System.nanoTime() - easyPlaceLastPickBlockTime < 1000000L * Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue();
	}

	protected static void setEasyPlaceLastPickBlockTime()
	{
		easyPlaceLastPickBlockTime = System.nanoTime();
	}
}

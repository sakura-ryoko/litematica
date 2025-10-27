package fi.dy.masa.litematica.render.schematic.blocks;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

/**
 * This is mainly required for mods that Override Vanilla Block Models. (Fusion, etc.)
 */
public class FallbackBlocks
{
	public static HashMap<Block, Identifier> BLOCK_TO_ID = new HashMap<>();
	public static HashMap<Identifier, Block> ID_TO_BLOCK = new HashMap<>();
	public static HashMap<Identifier, StateManager<Block, BlockState>> ID_TO_STATE_MANAGER = new HashMap<>();

	// Glass Blocks
	public static Identifier BLACK_GLASS = registerBasic("black_glass_fallback", Blocks.BLACK_STAINED_GLASS);
	public static Identifier BLUE_GLASS = registerBasic("blue_glass_fallback", Blocks.BLUE_STAINED_GLASS);
	public static Identifier BROWN_GLASS = registerBasic("brown_glass_fallback", Blocks.BROWN_STAINED_GLASS);
	public static Identifier CYAN_GLASS = registerBasic("cyan_glass_fallback", Blocks.CYAN_STAINED_GLASS);
	public static Identifier GLASS = registerBasic("glass_fallback", Blocks.GLASS);
	public static Identifier GRAY_GLASS = registerBasic("gray_glass_fallback", Blocks.GRAY_STAINED_GLASS);
	public static Identifier GREEN_GLASS = registerBasic("green_glass_fallback", Blocks.GREEN_STAINED_GLASS);
	public static Identifier LIME_GLASS = registerBasic("lime_glass_fallback", Blocks.LIME_STAINED_GLASS);
	public static Identifier LT_BLUE_GLASS = registerBasic("lt_blue_glass_fallback", Blocks.LIGHT_BLUE_STAINED_GLASS);
	public static Identifier LT_GRAY_GLASS = registerBasic("lt_gray_glass_fallback", Blocks.LIGHT_GRAY_STAINED_GLASS);
	public static Identifier MAGENTA_GLASS = registerBasic("magenta_glass_fallback", Blocks.MAGENTA_STAINED_GLASS);
	public static Identifier ORANGE_GLASS = registerBasic("orange_glass_fallback", Blocks.ORANGE_STAINED_GLASS);
	public static Identifier PINK_GLASS = registerBasic("pink_glass_fallback", Blocks.PINK_STAINED_GLASS);
	public static Identifier PURPLE_GLASS = registerBasic("purple_glass_fallback", Blocks.PURPLE_STAINED_GLASS);
	public static Identifier RED_GLASS = registerBasic("red_glass_fallback", Blocks.RED_STAINED_GLASS);
	public static Identifier TINTED_GLASS = registerBasic("tinted_glass_fallback", Blocks.TINTED_GLASS);
	public static Identifier WHITE_GLASS = registerBasic("white_glass_fallback", Blocks.WHITE_STAINED_GLASS);
	public static Identifier YELLOW_GLASS = registerBasic("yellow_glass_fallback", Blocks.YELLOW_STAINED_GLASS);

	// Glass Panes
	public static Identifier BLACK_GLASS_PANE = registerHorizontalConnecting("black_glass_pane_fallback", Blocks.BLACK_STAINED_GLASS_PANE);
	public static Identifier BLUE_GLASS_PANE = registerHorizontalConnecting("blue_glass_pane_fallback", Blocks.BLUE_STAINED_GLASS_PANE);
	public static Identifier BROWN_GLASS_PANE = registerHorizontalConnecting("brown_glass_pane_fallback", Blocks.BROWN_STAINED_GLASS_PANE);
	public static Identifier CYAN_GLASS_PANE = registerHorizontalConnecting("cyan_glass_pane_fallback", Blocks.CYAN_STAINED_GLASS_PANE);
	public static Identifier GLASS_PANE = registerHorizontalConnecting("glass_pane_fallback", Blocks.GLASS_PANE);
	public static Identifier GRAY_GLASS_PANE = registerHorizontalConnecting("gray_glass_pane_fallback", Blocks.GRAY_STAINED_GLASS_PANE);
	public static Identifier GREEN_GLASS_PANE = registerHorizontalConnecting("green_glass_pane_fallback", Blocks.GREEN_STAINED_GLASS_PANE);
	public static Identifier LIME_GLASS_PANE = registerHorizontalConnecting("lime_glass_pane_fallback", Blocks.LIME_STAINED_GLASS_PANE);
	public static Identifier LT_BLUE_GLASS_PANE = registerHorizontalConnecting("lt_blue_glass_pane_fallback", Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
	public static Identifier LT_GRAY_GLASS_PANE = registerHorizontalConnecting("lt_gray_glass_pane_fallback", Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
	public static Identifier MAGENTA_GLASS_PANE = registerHorizontalConnecting("magenta_glass_pane_fallback", Blocks.MAGENTA_STAINED_GLASS_PANE);
	public static Identifier ORANGE_GLASS_PANE = registerHorizontalConnecting("orange_glass_pane_fallback", Blocks.ORANGE_STAINED_GLASS_PANE);
	public static Identifier PINK_GLASS_PANE = registerHorizontalConnecting("pink_glass_pane_fallback", Blocks.PINK_STAINED_GLASS_PANE);
	public static Identifier PURPLE_GLASS_PANE = registerHorizontalConnecting("purple_glass_pane_fallback", Blocks.PURPLE_STAINED_GLASS_PANE);
	public static Identifier RED_GLASS_PANE = registerHorizontalConnecting("red_glass_pane_fallback", Blocks.RED_STAINED_GLASS_PANE);
	public static Identifier WHITE_GLASS_PANE = registerHorizontalConnecting("white_glass_pane_fallback", Blocks.WHITE_STAINED_GLASS_PANE);
	public static Identifier YELLOW_GLASS_PANE = registerHorizontalConnecting("yellow_glass_pane_fallback", Blocks.YELLOW_STAINED_GLASS_PANE);

	private static Identifier registerBasic(String name, Block block)
	{
		Identifier id = Identifier.of(Reference.MOD_ID, name);

		BLOCK_TO_ID.put(block, id);
		ID_TO_BLOCK.put(id, block);
		ID_TO_STATE_MANAGER.put(id, new StateManager.Builder<Block, BlockState>(block).build(Block::getDefaultState, BlockState::new));

		return id;
	}

	private static Identifier registerHorizontalConnecting(String name, Block block)
	{
		StateManager.Builder<Block, BlockState> builder = new StateManager.Builder<>(block);
		Identifier id = Identifier.of(Reference.MOD_ID, name);

		BLOCK_TO_ID.put(block, id);
		ID_TO_BLOCK.put(id, block);

		// Add vanilla properties to State Manager; since Fusion removes them.
		builder.add(HorizontalConnectingBlock.NORTH);
		builder.add(HorizontalConnectingBlock.EAST);
		builder.add(HorizontalConnectingBlock.SOUTH);
		builder.add(HorizontalConnectingBlock.WEST);
		builder.add(HorizontalConnectingBlock.WATERLOGGED);
		ID_TO_STATE_MANAGER.put(id, builder.build(FallbackBlocks::defaultHorizontalConnectingBlockState, BlockState::new));

		return id;
	}

	public static BlockState defaultHorizontalConnectingBlockState(Block block)
	{
		return block.getDefaultState()
		            .with(HorizontalConnectingBlock.NORTH, false)
		            .with(HorizontalConnectingBlock.EAST, false)
		            .with(HorizontalConnectingBlock.SOUTH, false)
		            .with(HorizontalConnectingBlock.WEST, false)
		            .with(HorizontalConnectingBlock.WATERLOGGED, false);
	}

	public static void register()
	{
		Litematica.debugLog("FallbackBlockModels: initialized.");
	}
}

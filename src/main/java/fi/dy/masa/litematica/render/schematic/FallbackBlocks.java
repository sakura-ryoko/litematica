package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

/**
 * This is mainly required for mods that Override Vanilla Block Models. (Fusion, etc.)
 */
public class FallbackBlocks
{
	public static HashMap<Block, Identifier> BLOCK_TO_ID = new HashMap<>();
	public static HashMap<Identifier, Block> ID_TO_BLOCK = new HashMap<>();
	public static HashMap<Identifier, StateDefinition<Block, BlockState>> ID_TO_STATE_MANAGER = new HashMap<>();

	// Glass Blocks
	public static Identifier BLACK_GLASS = registerBasic("black_glass_fallback", Blocks.STAINED_GLASS.black());
	public static Identifier BLUE_GLASS = registerBasic("blue_glass_fallback", Blocks.STAINED_GLASS.blue());
	public static Identifier BROWN_GLASS = registerBasic("brown_glass_fallback", Blocks.STAINED_GLASS.brown());
	public static Identifier CYAN_GLASS = registerBasic("cyan_glass_fallback", Blocks.STAINED_GLASS.cyan());
	public static Identifier GLASS = registerBasic("glass_fallback", Blocks.GLASS);
	public static Identifier GRAY_GLASS = registerBasic("gray_glass_fallback", Blocks.STAINED_GLASS.gray());
	public static Identifier GREEN_GLASS = registerBasic("green_glass_fallback", Blocks.STAINED_GLASS.green());
	public static Identifier LIME_GLASS = registerBasic("lime_glass_fallback", Blocks.STAINED_GLASS.lime());
	public static Identifier LT_BLUE_GLASS = registerBasic("lt_blue_glass_fallback", Blocks.STAINED_GLASS.lightBlue());
	public static Identifier LT_GRAY_GLASS = registerBasic("lt_gray_glass_fallback", Blocks.STAINED_GLASS.lightGray());
	public static Identifier MAGENTA_GLASS = registerBasic("magenta_glass_fallback", Blocks.STAINED_GLASS.magenta());
	public static Identifier ORANGE_GLASS = registerBasic("orange_glass_fallback", Blocks.STAINED_GLASS.orange());
	public static Identifier PINK_GLASS = registerBasic("pink_glass_fallback", Blocks.STAINED_GLASS.pink());
	public static Identifier PURPLE_GLASS = registerBasic("purple_glass_fallback", Blocks.STAINED_GLASS.purple());
	public static Identifier RED_GLASS = registerBasic("red_glass_fallback", Blocks.STAINED_GLASS.red());
	public static Identifier TINTED_GLASS = registerBasic("tinted_glass_fallback", Blocks.TINTED_GLASS);
	public static Identifier WHITE_GLASS = registerBasic("white_glass_fallback", Blocks.STAINED_GLASS.white());
	public static Identifier YELLOW_GLASS = registerBasic("yellow_glass_fallback", Blocks.STAINED_GLASS.yellow());

	// Glass Panes
	public static Identifier BLACK_GLASS_PANE = registerHorizontalConnecting("black_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.black());
	public static Identifier BLUE_GLASS_PANE = registerHorizontalConnecting("blue_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.blue());
	public static Identifier BROWN_GLASS_PANE = registerHorizontalConnecting("brown_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.brown());
	public static Identifier CYAN_GLASS_PANE = registerHorizontalConnecting("cyan_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.cyan());
	public static Identifier GLASS_PANE = registerHorizontalConnecting("glass_pane_fallback", Blocks.GLASS_PANE);
	public static Identifier GRAY_GLASS_PANE = registerHorizontalConnecting("gray_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.gray());
	public static Identifier GREEN_GLASS_PANE = registerHorizontalConnecting("green_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.green());
	public static Identifier LIME_GLASS_PANE = registerHorizontalConnecting("lime_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.lime());
	public static Identifier LT_BLUE_GLASS_PANE = registerHorizontalConnecting("lt_blue_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.lightBlue());
	public static Identifier LT_GRAY_GLASS_PANE = registerHorizontalConnecting("lt_gray_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.lightGray());
	public static Identifier MAGENTA_GLASS_PANE = registerHorizontalConnecting("magenta_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.magenta());
	public static Identifier ORANGE_GLASS_PANE = registerHorizontalConnecting("orange_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.orange());
	public static Identifier PINK_GLASS_PANE = registerHorizontalConnecting("pink_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.pink());
	public static Identifier PURPLE_GLASS_PANE = registerHorizontalConnecting("purple_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.purple());
	public static Identifier RED_GLASS_PANE = registerHorizontalConnecting("red_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.red());
	public static Identifier WHITE_GLASS_PANE = registerHorizontalConnecting("white_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.white());
	public static Identifier YELLOW_GLASS_PANE = registerHorizontalConnecting("yellow_glass_pane_fallback", Blocks.STAINED_GLASS_PANE.yellow());

	private static Identifier registerBasic(String name, Block block)
	{
		Identifier id = Identifier.fromNamespaceAndPath(Reference.MOD_ID, name);

		BLOCK_TO_ID.put(block, id);
		ID_TO_BLOCK.put(id, block);
		ID_TO_STATE_MANAGER.put(id, new StateDefinition.Builder<Block, BlockState>(block).create(Block::defaultBlockState, BlockState::new));

		return id;
	}

	private static Identifier registerHorizontalConnecting(String name, Block block)
	{
		StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(block);
		Identifier id = Identifier.fromNamespaceAndPath(Reference.MOD_ID, name);

		BLOCK_TO_ID.put(block, id);
		ID_TO_BLOCK.put(id, block);

		// Add vanilla properties to State Manager; since Fusion removes them.
		builder.add(CrossCollisionBlock.NORTH);
		builder.add(CrossCollisionBlock.EAST);
		builder.add(CrossCollisionBlock.SOUTH);
		builder.add(CrossCollisionBlock.WEST);
		builder.add(CrossCollisionBlock.WATERLOGGED);
		ID_TO_STATE_MANAGER.put(id, builder.create(FallbackBlocks::defaultHorizontalConnectingBlockState, BlockState::new));

		return id;
	}

	public static BlockState defaultHorizontalConnectingBlockState(Block block)
	{
		return block.defaultBlockState()
		            .setValue(CrossCollisionBlock.NORTH, false)
		            .setValue(CrossCollisionBlock.EAST, false)
		            .setValue(CrossCollisionBlock.SOUTH, false)
		            .setValue(CrossCollisionBlock.WEST, false)
		            .setValue(CrossCollisionBlock.WATERLOGGED, false);
	}

	public static void register()
	{
		Litematica.debugLog("FallbackBlockModels: initialized.");
	}
}

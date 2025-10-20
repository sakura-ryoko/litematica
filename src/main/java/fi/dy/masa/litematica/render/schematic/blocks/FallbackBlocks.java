package fi.dy.masa.litematica.render.schematic.blocks;

import java.util.function.Function;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

/**
 * This is mainly required for mods that Override Vanilla Block Models. (Fusion, etc.)
 */
public class FallbackBlocks
{
	// Glass Blocks
	public static Block BLACK_GLASS = register("black_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block BLUE_GLASS = register("blue_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block BROWN_GLASS = register("brown_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block CYAN_GLASS = register("cyan_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block GLASS = register("glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block GRAY_GLASS = register("gray_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block GREEN_GLASS = register("green_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block LIME_GLASS = register("lime_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block LT_BLUE_GLASS = register("lt_blue_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block LT_GRAY_GLASS = register("lt_gray_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block MAGENTA_GLASS = register("magenta_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block ORANGE_GLASS = register("orange_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block PINK_GLASS = register("pink_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block PURPLE_GLASS = register("purple_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block RED_GLASS = register("red_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block TINTED_GLASS = register("tinted_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block WHITE_GLASS = register("white_glass_fallback", FallbackTransparentBlock::new, glassSettings());
	public static Block YELLOW_GLASS = register("yellow_glass_fallback", FallbackTransparentBlock::new, glassSettings());

	// Glass Panes
	public static Block BLACK_GLASS_PANE = register("black_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block BLUE_GLASS_PANE = register("blue_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block BROWN_GLASS_PANE = register("brown_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block CYAN_GLASS_PANE = register("cyan_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block GLASS_PANE = register("glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block GRAY_GLASS_PANE = register("gray_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block GREEN_GLASS_PANE = register("green_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block LIME_GLASS_PANE = register("lime_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block LT_BLUE_GLASS_PANE = register("lt_blue_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block LT_GRAY_GLASS_PANE = register("lt_gray_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block MAGENTA_GLASS_PANE = register("magenta_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block ORANGE_GLASS_PANE = register("orange_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block PINK_GLASS_PANE = register("pink_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block PURPLE_GLASS_PANE = register("purple_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block RED_GLASS_PANE = register("red_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block WHITE_GLASS_PANE = register("white_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());
	public static Block YELLOW_GLASS_PANE = register("yellow_glass_pane_fallback", FallbackPaneBlock::new, glassSettings());

	private static AbstractBlock.Settings glassSettings()
	{
		return AbstractBlock.Settings.create()
		                      .dropsNothing()
		                      .strength(0.3F)
		                      .nonOpaque();
	}

	private static Identifier getId(String name)
	{
		return Identifier.of(Reference.MOD_ID, name);
	}

	private static RegistryKey<Block> getKey(Identifier block)
	{
		return RegistryKey.of(RegistryKeys.BLOCK, block);
	}

	private static Block register(String name, Function<AbstractBlock.Settings, Block> func, AbstractBlock.Settings settings)
	{
		return register(getKey(getId(name)), func, settings);
	}

	private static Block register(RegistryKey<Block> key, Function<AbstractBlock.Settings, Block> func, AbstractBlock.Settings settings)
	{
		return Blocks.register(key, func, settings);
	}

	public static void register()
	{
		Litematica.LOGGER.info("FallbackBlockModels: initialized.");
	}
}

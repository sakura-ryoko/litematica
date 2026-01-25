package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.game.DebugHudUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler;
import fi.dy.masa.litematica.util.DebugHudMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaDebugHud implements DebugScreenEntry
{
	public static final Identifier LITEMATICA_DEBUG = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "litematica_renderer");
	public static final Identifier SECTION_ID = Identifier.withDefaultNamespace(Reference.MOD_ID);
	public static final LitematicaDebugHud INSTANCE = new LitematicaDebugHud();
	private boolean left;

	private LitematicaDebugHud()
	{
		this.left = true;
	}

	public DebugHudMode getMode()
	{
		return (DebugHudMode) Configs.Generic.DEBUG_HUD_MODE.getOptionListValue();
	}

	public void checkConfig()
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.debugEntries == null) return;

		if (this.getMode() == DebugHudMode.VANILLA)
		{
			DebugHudUtils.register(LITEMATICA_DEBUG, LitematicaDebugHud.INSTANCE);
		}
		else if (this.getMode() != DebugHudMode.VANILLA)
		{
			DebugHudUtils.unregister(LITEMATICA_DEBUG);
		}
	}

	public boolean isLeft()
	{
		return this.left;
	}

	public void toggleRight()
	{
		this.left = false;
	}

	public void toggleLeft()
	{
		this.left = true;
	}

//	public boolean isDefaultProfile()
//	{
//		MinecraftClient mc = MinecraftClient.getInstance();
//		if (mc.debugHudEntryList == null) return false;
//
//		List<Identifier> vis = new ArrayList<>(mc.debugHudEntryList.visibleEntries);
//		Set<Identifier> def = DebugHudEntries.PROFILES.get(DebugProfileType.DEFAULT).keySet();
//
//		for (Identifier entry : def)
//		{
//			vis.remove(entry);
//		}
//
//		return (vis.size() == 1 && vis.getFirst() == LITEMATICA_DEBUG);
//	}

	@Override
	public void display(@Nonnull DebugScreenDisplayer lines, @Nullable Level world, @Nullable LevelChunk clientChunk, @Nullable LevelChunk chunk)
	{
		if (this.getMode() == DebugHudMode.NONE)
		{
			return;
		}

		List<String> list = LitematicaDebugHud.INSTANCE.getDebugLines();

		if (!list.isEmpty())
		{
			lines.addToGroup(SECTION_ID, list);
		}
	}

	@Override
	public boolean isAllowed(boolean reducedDebugInfo)
	{
		return true;
	}

	public List<String> getDebugLines()
	{
		WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
		List<String> list = new ArrayList<>();
		String pre = GuiBase.TXT_GOLD;
		String red = GuiBase.TXT_RED;
		String rst = GuiBase.TXT_RST;

		if (worldSchematic != null)
		{
			// Loaded
			Pair<String, String> pair = EntityUtils.getEntityDebug();

			IWorldSchematicRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
			String str = this.getWorldDebug(worldSchematic);
			String pmStr = PlacementManagerDaemonHandler.INSTANCE.getDebugString();

			if (this.isLeft())
			{
				list.add(String.format("%s[Litematica]%s %s",
				                       pre, rst,
				                       renderer.getDebugInfoRenders()
				));

				list.add(String.format("%s[Litematica]%s %s %s",
				                       pre, rst,
				                       renderer.getDebugInfoEntities(),
				                       str
				));

//				if (Reference.DEBUG_MODE)
//				{
					list.add(String.format("%s[Litematica]%s %s",
					                       pre, rst,
					                       pmStr
					));
//				}

				if (!pair.getLeft().isEmpty())
				{
					list.add(String.format("%s[%s]%s %s",
					                       pre, pair.getLeft(),
					                       rst, pair.getRight()
					));
				}
			}
			else
			{
				list.add(String.format("%s %s[Litematica]%s",
				                       renderer.getDebugInfoRenders(),
				                       rst+pre, rst
				));

				list.add(String.format("%s %s %s[Litematica]%s",
				                       renderer.getDebugInfoEntities(),
				                       str,
				                       rst+pre, rst
				));

//				if (Reference.DEBUG_MODE)
//				{
					list.add(String.format("%s %s[Litematica]%s",
					                       pmStr,
					                       pre, rst
					));
//				}

				if (!pair.getLeft().isEmpty())
				{
					list.add(String.format("%s %s[%s]%s",
					                       pair.getRight(),
					                       rst+pre, pair.getLeft(),
					                       rst
					));
				}
			}
		}
		else
		{
			// Not loaded
			if (this.isLeft())
			{
				list.add(String.format("%s[Litematica]%s %s",
				                       pre, rst+red,
				                       StringUtils.translate("litematica.gui.message.debug_hud.not_loaded")
				));
			}
			else
			{
				list.add(String.format("%s%s %s[Litematica]%s",
				                       red,
				                       StringUtils.translate("litematica.gui.message.debug_hud.not_loaded"),
				                       rst+pre, rst
				));

			}
		}

		return list;
	}

	private @NonNull String getWorldDebug(WorldSchematic worldSchematic)
	{
		String str;

		if (Reference.DEBUG_MODE)
		{
			str = String.format("[%s] // TE: %02d C: %02d, CT: %02d, CV: %02d",
			                    worldSchematic.getEntityDebug(),
			                    worldSchematic.getChunkSource().getTileEntityCount(),
			                    worldSchematic.getChunkSource().getLoadedChunksCount(),
			                    DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
			                    DataManager.getSchematicPlacementManager().getLastVisibleChunksCount()
			);
		}
		else
		{
			str = String.format("E: %02d TE: %02d C: %02d, CT: %02d, CV: %02d",
			                    worldSchematic.getRegularEntityCount(),
			                    worldSchematic.getChunkSource().getTileEntityCount(),
			                    worldSchematic.getChunkSource().getLoadedChunksCount(),
			                    DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
			                    DataManager.getSchematicPlacementManager().getLastVisibleChunksCount()
			);
		}

		return str;
	}
}

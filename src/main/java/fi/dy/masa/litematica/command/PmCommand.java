package fi.dy.masa.litematica.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IClientCommandListener;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;

public class PmCommand implements IClientCommandListener
{
	public final static String PREFIX = Reference.MOD_ID+".pm_command";

	@Override
	public String getCommand()
	{
		return "#pm";
	}

	@Override
	public boolean execute(List<String> args, Minecraft mc)
	{
		if (mc.getCameraEntity() == null) return false;
		List<String> list = new ArrayList<>(args);      // Copy it first
		ChunkPos camPos = mc.getCameraEntity().chunkPosition();
		ChatComponent chat = mc.gui.getChat();
		list.removeFirst();

		if (!list.isEmpty())
		{
			String sub = list.getFirst();

			if (!sub.isEmpty())
			{
				list.removeFirst();

				if (sub.equalsIgnoreCase("ci"))
				{
					return this.processChunkDebug(list, camPos, chat);
				}
				else if (sub.equalsIgnoreCase("rb"))
				{
					return this.processChunkRebuild(list, camPos, chat);
				}
				else if (sub.equalsIgnoreCase("help"))
				{
					return this.processHelp(chat);
				}
				else
				{
					return this.processInvalid(chat);
				}
			}
			else
			{
				return this.processChunkDebug(list, camPos, chat);
			}
		}
		else
		{
			return this.processChunkDebug(list, camPos, chat);
		}
	}

	private boolean processInvalid(ChatComponent chat)
	{
		chat.addClientSystemMessage(StringUtils.translateAsText(PREFIX+".invalid_other"));
		return true;
	}

	private boolean processHelp(ChatComponent chat)
	{
		chat.addClientSystemMessage(StringUtils.translateAsText(PREFIX+".help")
		);

		return true;
	}

	private boolean processChunkDebug(List<String> args, ChunkPos camPos, ChatComponent chat)
	{
		if (args.size() >= 2)
		{
			String x = args.get(0);
			String z = args.get(1);

			try
			{
				final int cx = Integer.parseInt(x);
				final int cz = Integer.parseInt(z);

				DataManager.getSchematicPlacementManager().displayChunkDebugCmd(cx, cz, chat);
			}
			catch (NumberFormatException e)
			{
				chat.addClientSystemMessage(StringUtils.translateAsText(PREFIX+".invalid_args"));
			}
		}
		else
		{
			DataManager.getSchematicPlacementManager().displayChunkDebugCmd(camPos.x(), camPos.z(), chat);
		}

		return true;
	}

	private boolean processChunkRebuild(List<String> args, ChunkPos camPos, ChatComponent chat)
	{
		if (args.size() >= 2)
		{
			String x = args.get(0);
			String z = args.get(1);

			try
			{
				final int cx = Integer.parseInt(x);
				final int cz = Integer.parseInt(z);

				DataManager.getSchematicPlacementManager().markChunkForRebuildCmd(cx, cz, chat);
			}
			catch (NumberFormatException e)
			{
				chat.addClientSystemMessage(StringUtils.translateAsText(PREFIX+".invalid_args"));
			}
		}
		else
		{
			DataManager.getSchematicPlacementManager().markChunkForRebuildCmd(camPos.x(), camPos.z(), chat);
		}

		return true;
	}
}

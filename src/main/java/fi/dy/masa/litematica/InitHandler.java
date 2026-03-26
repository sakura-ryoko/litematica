package fi.dy.masa.litematica;

import fi.dy.masa.malilib.command.ClientCommandHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.*;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;

import fi.dy.masa.litematica.command.PmCommand;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.event.*;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );

        EntitiesDataStorage.getInstance().onGameInit();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        IRenderer renderer = new RenderHandler();
        RenderEventHandler.getInstance().registerInGameGuiRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldPreWeatherRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        ServerHandler.getInstance().registerServerHandler(new ServerListener());

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());
        TickHandler.getInstance().registerClientTickHandler(EntitiesDataStorage.getInstance());
        TickHandler.getInstance().registerClientTickHandler(PlacementManagerDaemonHandler.INSTANCE);

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        KeyCallbacks.init(Minecraft.getInstance());
        StatusInfoRenderer.init();

        ClientCommandHandler.INSTANCE.registerCommand(new PmCommand());

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();
    }
}

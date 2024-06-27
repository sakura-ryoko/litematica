package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadImmutable(DynamicRegistryManager.Immutable immutable)
    {
        // Save the DynamicRegistry before the IntegratedServer even launches, when possible
        SchematicWorldHandler.INSTANCE.setDynamicRegistryManager(immutable);
    }

    @Override
    public void onWorldLoadPre(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Save the settings before the integrated server gets shut down
        if (worldBefore != null)
        {
            DataManager.save();
        }
        if (worldAfter != null)
        {
            EntitiesDataStorage.getInstance().onWorldPre();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        SchematicWorldHandler.INSTANCE.recreateSchematicWorld(worldAfter == null);
        DataManager.getInstance().reset(worldAfter == null);
        EntitiesDataStorage.getInstance().reset(worldAfter == null);

        if (worldAfter != null)
        {
            DataManager.load();
            SchematicConversionMaps.computeMaps();
            EntitiesDataStorage.getInstance().onWorldJoin();
        }
        else
        {
            DataManager.clear();
        }
    }
}

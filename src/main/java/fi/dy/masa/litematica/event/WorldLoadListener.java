package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.datafixer.fix.BlockStateFlattening;
import net.minecraft.registry.DynamicRegistryManager;

import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.compat.jade.JadeCompat;
import fi.dy.masa.litematica.data.CachedTagManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

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
            JadeCompat.checkForJade();
            EntitiesDataStorage.getInstance().onWorldPre();
            DataManager.getInstance().onWorldPre(worldAfter.getRegistryManager());
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
            Litematica.debugLog("onWorldLoadPost(): Init BlockStateFlattening DataFixer [Test: {}]", BlockStateFlattening.lookupBlock("minecraft:air"));
            SchematicConversionMaps.computeMaps();
            EntitiesDataStorage.getInstance().onWorldJoin();
            CachedTagManager.startCache();
        }
        else
        {
            DataManager.clear();
        }
    }
}

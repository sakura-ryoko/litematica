package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.datafix.fixes.BlockStateData;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.compat.jade.JadeCompat;
import fi.dy.masa.litematica.data.CachedTagManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.render.LitematicaDebugHud;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.schematic.placement.TemporaryWorldManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadImmutable(RegistryAccess.Frozen immutable)
    {
        // Save the DynamicRegistry before the IntegratedServer even launches, when possible
        SchematicWorldHandler.INSTANCE.setDynamicRegistryManager(immutable);
    }

    @Override
    public void onWorldLoadPre(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
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
            DataManager.getInstance().onWorldPre(worldAfter.registryAccess());
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        SchematicWorldHandler.INSTANCE.recreateSchematicWorld(worldAfter == null);
        DataManager.getInstance().reset(worldAfter == null);
        EntitiesDataStorage.getInstance().reset(worldAfter == null);
        TemporaryWorldManager.INSTANCE.reset();

        if (worldAfter != null)
        {
            Litematica.debugLog("onWorldLoadPost(): Init BlockStateFlattening DataFixer [Test: {}]", BlockStateData.upgradeBlock("minecraft:air"));
            SchematicConversionMaps.computeMaps();
            DataManager.load();
            EntitiesDataStorage.getInstance().onWorldJoin();
            CachedTagManager.startCache();
	        LitematicaDebugHud.INSTANCE.checkConfig();
            DataManager.getSchematicPlacementManager().onWorldJoin();
        }
        else
        {
            TemporaryWorldManager.INSTANCE.clear();
            DataManager.clear();
        }
    }
}

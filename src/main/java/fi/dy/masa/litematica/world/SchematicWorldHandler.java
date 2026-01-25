package fi.dy.masa.litematica.world;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    public static final SchematicWorldHandler INSTANCE = new SchematicWorldHandler(LitematicaRenderer.getInstance()::getWorldRenderer);

    protected final Supplier<IWorldSchematicRenderer> rendererSupplier;
    @Nullable protected WorldSchematic world;
    @Nullable protected RegistryAccess.Frozen dynamicRegistryManager = RegistryAccess.EMPTY;

    // The supplier can return null, but it can't be null itself!
    public SchematicWorldHandler(Supplier<IWorldSchematicRenderer> rendererSupplier)
    {
        this.rendererSupplier = rendererSupplier;
    }

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return INSTANCE.getWorld();
    }

    @Nullable
    public WorldSchematic getWorld()
    {
        if (this.world == null)
        {
            this.world = createSchematicWorld(this.rendererSupplier.get());
        }

        return this.world;
    }

    public void setDynamicRegistryManager(@Nullable RegistryAccess.Frozen immutable)
    {
        if (immutable == null)
        {
            return;
        }

        this.dynamicRegistryManager = immutable;
    }

    /**
     * Store/Get the Dynamic Registry if we can get it
     */
    public RegistryAccess getRegistryManager()
    {
        return this.dynamicRegistryManager;
    }

    public static WorldSchematic createSchematicWorld(@Nullable IWorldSchematicRenderer worldRenderer)
    {
        Level world = Minecraft.getInstance().level;

        if (world == null)
        {
            return null;
        }

        // Use the DimensionType of the current client world
        ClientLevel.ClientLevelData levelInfo = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, true);

        return new WorldSchematic(levelInfo, world.registryAccess(), world.dimensionTypeRegistration(), worldRenderer);
    }

    public void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            Litematica.debugLog("Removing the schematic world...");
            if (this.world != null)
            {
                this.world.clearEntities();

                try
                {
                    this.world.close();
                }
                catch (Exception ignored) {}
            }

            this.world = null;
            LitematicaRenderer.getInstance().onSchematicWorldChanged(null);
        }
        else
        {
            Litematica.debugLog("(Re-)creating the schematic world...");
            @Nullable IWorldSchematicRenderer worldRenderer = this.world != null ? this.world.worldRenderer : LitematicaRenderer.getInstance().resetWorldRenderer();
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            this.world = createSchematicWorld(worldRenderer);
            Litematica.debugLog("Schematic world (re-)created: {}", this.world);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(this.world);
    }
}

package fi.dy.masa.litematica.schematic.placement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventListener;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;

/**
 * This was primarily created for mods like Syncmatica with complicated Mixin's that can be fragile.
 * So here we go adding an event callback system for Schematic Placement management.
 */
public class SchematicPlacementEventHandler implements ISchematicPlacementEventManager
{
    private static final SchematicPlacementEventHandler INSTANCE = new SchematicPlacementEventHandler();
    private final HashMap<ISchematicPlacementEventListener, List<SchematicPlacementEventFlag>> handlers = new HashMap<>();
    public static SchematicPlacementEventHandler getInstance() { return INSTANCE; }

    @Override
    public void registerSchematicPlacementEventListener(@Nonnull ISchematicPlacementEventListener listener,
                                                        @Nonnull List<SchematicPlacementEventFlag> flags)
    {
        if (flags.isEmpty())
        {
            Litematica.LOGGER.warn("registerSchematicPlacementEventListener: provided event flags for '{}' are empty.  Ignoring.",
                                   listener.getClass().getCanonicalName() != null ? listener.getClass().getCanonicalName() : listener.getClass().getName());
            return;
        }

        if (!this.handlers.containsKey(listener))
        {
            if (flags.contains(SchematicPlacementEventFlag.ALL_EVENTS))
            {
                this.handlers.put(listener, Arrays.stream(SchematicPlacementEventFlag.values()).toList());
            }
            else
            {
                this.handlers.put(listener, flags);
            }
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokePrePlacementChange(@Nonnull ISchematicPlacementEventListener listener,
                                         @Nonnull SchematicPlacement placement)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.placementManager.onPrePlacementChange(placement);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokePostPlacementChange(@Nonnull ISchematicPlacementEventListener listener,
                                          @Nonnull SchematicPlacement placement)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.placementManager.onPostPlacementChange(placement);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokePlacementModified(@Nonnull ISchematicPlacementEventListener listener,
                                        @Nonnull SchematicPlacement placement)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.onModified(placement.placementManager);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeSetSubRegionEnabled(@Nonnull ISchematicPlacementEventListener listener,
                                          @Nonnull SubRegionPlacement placement, boolean toggle)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.setEnabled(toggle);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeSetSubRegionOrigin(@Nonnull ISchematicPlacementEventListener listener,
                                         @Nonnull SubRegionPlacement placement, BlockPos pos)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.setPos(pos);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeSetSubRegionMirror(@Nonnull ISchematicPlacementEventListener listener,
                                         @Nonnull SubRegionPlacement placement, BlockMirror mirror)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.setMirror(mirror);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeSetSubRegionRotation(@Nonnull ISchematicPlacementEventListener listener,
                                           @Nonnull SubRegionPlacement placement, BlockRotation rot)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.setRotation(rot);
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeResetSubRegion(@Nonnull ISchematicPlacementEventListener listener,
                                     @Nonnull SubRegionPlacement placement)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.resetToOriginalValues();
        }
    }

    /**
     * Requires All Events Flag
     */
    @Override
    public void invokeSubRegionModified(@Nonnull ISchematicPlacementEventListener listener,
                                        @Nonnull SchematicPlacement placement,
                                        @Nonnull String regionName)
    {
        if (this.handlers.containsKey(listener) &&
            this.handlers.get(listener).contains(SchematicPlacementEventFlag.ALL_EVENTS))
        {
            placement.onModified(regionName, placement.placementManager);
        }
    }

    @ApiStatus.Internal
    public void onPlacementInit(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.INIT))
                    {
                        handler.onPlacementInit(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSubRegionInit(SubRegionPlacement subRegion)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.INIT_SUBREGION))
                    {
                        handler.onSubRegionInit(subRegion);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFor(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.CREATE_FOR))
                    {
                        handler.onPlacementCreateFor(placement, schematic, origin, name, enabled, enableRender);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateForConversion(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FOR_CONVERSION))
                    {
                        handler.onPlacementCreateForConversion(placement, schematic, origin);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFromJson(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, BlockRotation rotation, BlockMirror mirror, boolean enabled, boolean enableRender, JsonObject obj)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FROM_JSON))
                    {
                        handler.onPlacementCreateFromJson(placement, schematic, origin, name, rotation, mirror, enabled, enableRender, obj);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFromNbt(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, BlockRotation rotation, BlockMirror mirror, boolean enabled, boolean enableRender, NbtCompound nbt)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FROM_NBT))
                    {
                        handler.onPlacementCreateFromNbt(placement, schematic, origin, name, rotation, mirror, enabled, enableRender, nbt);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSavePlacementToJson(SchematicPlacement placement, JsonObject jsonObject)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TO_JSON))
                    {
                        handler.onSavePlacementToJson(placement, jsonObject);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSavePlacementToNbt(SchematicPlacement placement, NbtCompound nbt)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TO_NBT))
                    {
                        handler.onSavePlacementToNbt(placement, nbt);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSubRegionCreateFromJson(SubRegionPlacement subRegion, BlockPos origin, String name, BlockRotation rotation, BlockMirror mirror, boolean enabled, boolean enableRender, JsonObject obj)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SUBREGION_FROM_JSON))
                    {
                        handler.onSubRegionCreateFromJson(subRegion, origin, name, rotation, mirror, enabled, enableRender, obj);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSaveSubRegionToJson(SubRegionPlacement subRegion, JsonObject jsonObject)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SUBREGION_TO_JSON))
                    {
                        handler.onSaveSubRegionToJson(subRegion, jsonObject);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onToggleLocked(SchematicPlacement placement, boolean toggle)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TOGGLE_LOCKED))
                    {
                        handler.onToggleLocked(placement, toggle);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetName(SchematicPlacement placement, String name)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_NAME))
                    {
                        handler.onSetName(placement, name);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetEnabled(SchematicPlacement placement, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ENABLED))
                    {
                        handler.onSetEnabled(placement, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetRender(SchematicPlacement placement, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_RENDER))
                    {
                        handler.onSetRender(placement, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetOrigin(SchematicPlacement placement, BlockPos origin)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ORIGIN))
                    {
                        handler.onSetOrigin(placement, origin);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetMirror(SchematicPlacement placement, BlockMirror mirror)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_MIRROR))
                    {
                        handler.onSetMirror(placement, mirror);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetRotation(SchematicPlacement placement, BlockRotation rotation)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ROTATION))
                    {
                        handler.onSetRotation(placement, rotation);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementReset(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_RESET))
                    {
                        handler.onPlacementReset(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetSubRegionEnabled(SubRegionPlacement subRegion, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_SUBREGION_ENABLED))
                    {
                        handler.onSetSubRegionEnabled(subRegion, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetSubRegionRender(SubRegionPlacement subRegion, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_SUBREGION_RENDER))
                    {
                        handler.onSetSubRegionRender(subRegion, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetSubRegionOrigin(SubRegionPlacement subRegion, BlockPos origin)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_SUBREGION_ORIGIN))
                    {
                        handler.onSetSubRegionOrigin(subRegion, origin);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetSubRegionMirror(SubRegionPlacement subRegion, BlockMirror mirror)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_SUBREGION_MIRROR))
                    {
                        handler.onSetSubRegionMirror(subRegion, mirror);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetSubRegionRotation(SubRegionPlacement subRegion, BlockRotation rotation)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_SUBREGION_ROTATION))
                    {
                        handler.onSetSubRegionRotation(subRegion, rotation);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSubRegionReset(SubRegionPlacement subRegion)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_SUBREGION_RESET))
                    {
                        handler.onSubRegionReset(subRegion);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementSelected(@Nullable SchematicPlacement prevPlacement, @Nullable SchematicPlacement selected)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_SELECTED))
                    {
                        handler.onPlacementSelected(prevPlacement, selected);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementAdded(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_ADDED))
                    {
                        handler.onPlacementAdded(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementRemoved(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_REMOVED))
                    {
                        handler.onPlacementRemoved(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementUpdated(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_UPDATED))
                    {
                        handler.onPlacementUpdated(placement);
                    }
                }
        );
    }
}

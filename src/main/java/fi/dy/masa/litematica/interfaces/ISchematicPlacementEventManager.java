package fi.dy.masa.litematica.interfaces;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

public interface ISchematicPlacementEventManager
{
    void registerSchematicPlacementEventListener(@Nonnull ISchematicPlacementEventListener listener, @Nonnull List<SchematicPlacementEventFlag> flags);

    void invokePrePlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePostPlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePlacementModified(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokeSetSubRegionEnabled(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, boolean toggle);

    void invokeSetSubRegionOrigin(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, BlockPos pos);

    void invokeSetSubRegionMirror(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, BlockMirror mirror);

    void invokeSetSubRegionRotation(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, BlockRotation rot);

    void invokeSubRegionModified(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement, @Nonnull String subRegionName);

    void invokeResetSubRegion(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion);
}

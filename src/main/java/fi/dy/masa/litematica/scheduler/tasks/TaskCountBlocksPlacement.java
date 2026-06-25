package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.InclusionType;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final SchematicPlacement schematicPlacement;
    protected final boolean ignoreState;
    protected final InclusionType entitiesInclusionType;
    protected final InclusionType containersInclusionType;
    protected final Object2IntOpenHashMap<ItemType> entitiesTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<ItemType> containersTotal = new Object2IntOpenHashMap<>();

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(schematicPlacement, materialList, false);
    }

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList, boolean ignoreState)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        this.schematicPlacement = schematicPlacement;
        this.ignoreState = ignoreState;
        this.entitiesInclusionType = materialList.getEntitiesInclusionType();
        this.containersInclusionType = materialList.getContainersInclusionType();
        Collection<Box> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();

        // Filter/clamp the boxes to intersect with the render layer
        if (materialList.getMaterialListType() == BlockInfoListType.RENDER_LAYERS)
        {
            this.addPerChunkBoxes(boxes, DataManager.getRenderLayerRange());
        }
        else
        {
            this.addPerChunkBoxes(boxes);
        }

    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        boolean result = super.processChunk(pos);

        if (this.entitiesInclusionType != InclusionType.NONE || this.containersInclusionType != InclusionType.NONE)
        {
            this.countEntitiesInChunk(pos);
        }

        return result;
    }

    protected void countEntitiesInChunk(ChunkPos pos)
    {
        List<Entity> entities = this.schematicWorld.getEntitiesByChunk(pos.x, pos.z, EntityUtils.NOT_PLAYER);

        for (Entity entity : entities)
        {
            if (!this.layerRange.isPositionWithinRange(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ())))
            {
                continue;
            }

            if (this.entitiesInclusionType != InclusionType.NONE)
            {
                Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                Item item = BuiltInRegistries.ITEM.getValue(identifier);
                ItemType itemType = new ItemType(new ItemStack(item), false);

                this.entitiesTotal.addTo(itemType, 1);
            }

            if (this.containersInclusionType != InclusionType.NONE && entity instanceof Container container)
            {
                this.addContainerItems(container);
            }
        }
    }

    protected void addContainerItems(Container container)
    {
        Object2IntOpenHashMap<ItemType> items = MaterialListUtils.getInventoryItemCounts(container);

        for (ItemType itemType : items.keySet())
        {
            this.containersTotal.addTo(itemType, items.getInt(itemType));
        }
    }

    @Override
    protected List<MaterialListEntry> buildMaterialListEntries()
    {
        return MaterialListUtils.buildEntriesForPlacement(this.countsTotal, this.countsMissing, this.countsMismatch,
                                                           this.entitiesTotal, this.entitiesInclusionType,
                                                           this.containersTotal, this.containersInclusionType,
                                                           this.mc.player);
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir() == false)
        {
            BlockState stateClient = this.clientWorld.getBlockState(pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.isAir())
            {
                this.countsMissing.addTo(stateSchematic, 1);
            }
            else if (stateClient != stateSchematic &&
                    (this.ignoreState == false || stateClient.getBlock() != stateSchematic.getBlock()))
            {
                this.countsMissing.addTo(stateSchematic, 1);
                this.countsMismatch.addTo(stateSchematic, 1);
            }

            if (this.containersInclusionType != InclusionType.NONE)
            {
                BlockEntity blockEntity = this.schematicWorld.getBlockEntity(pos);

                if (blockEntity instanceof Container container)
                {
                    this.addContainerItems(container);
                }
            }
        }
    }
}

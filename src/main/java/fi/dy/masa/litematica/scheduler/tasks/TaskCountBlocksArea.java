package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.InclusionType;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    protected final InclusionType entitiesInclusionType;
    protected final InclusionType containersInclusionType;
    protected final Object2IntOpenHashMap<ItemType> entitiesTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<ItemType> containersTotal = new Object2IntOpenHashMap<>();

    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.entitiesInclusionType = materialList.getEntitiesInclusionType();
        this.containersInclusionType = materialList.getContainersInclusionType();

        this.addPerChunkBoxes(selection.getAllSubRegionBoxes());
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
        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            AABB aabb = new AABB(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);
            // Use the "best" world (the integrated server level in singleplayer) rather than the
            // client level, since the client is never told the contents of containers it hasn't opened
            List<Entity> entities = this.world.getEntities((Entity) null, aabb, EntityUtils.NOT_PLAYER);

            for (Entity entity : entities)
            {
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
        return MaterialListUtils.buildEntriesForAreaAnalyzer(this.countsTotal,
                                                              this.entitiesTotal, this.entitiesInclusionType,
                                                              this.containersTotal, this.containersInclusionType,
                                                              this.mc.player);
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateClient = this.clientWorld.getBlockState(pos);
        this.countsTotal.addTo(stateClient, 1);

        if (this.containersInclusionType != InclusionType.NONE)
        {
            // Level.getBlockEntity(pos) can miss block entities on the server level here (see
            // InventoryUtils.getTargetInventory for the same workaround), so go through the chunk instead
            BlockEntity blockEntity = this.world.getChunkAt(pos).getBlockEntity(pos);

            if (blockEntity instanceof Container container)
            {
                this.addContainerItems(container);
            }
        }
    }
}

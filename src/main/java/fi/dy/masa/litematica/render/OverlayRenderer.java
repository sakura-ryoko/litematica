package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.compat.jade.JadeCompat;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult.BlockMismatchInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class OverlayRenderer
{
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();

    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    public static final int[] KELLY_COLORS = {
            0xFFB300,    // Vivid Yellow
            0x803E75,    // Strong Purple
            0xFF6800,    // Vivid Orange
            0xA6BDD7,    // Very Light Blue
            0xC10020,    // Vivid Red
            0xCEA262,    // Grayish Yellow
            0x817066,    // Medium Gray
            // The following don't work well for people with defective color vision
            0x007D34,    // Vivid Green
            0xF6768E,    // Strong Purplish Pink
            0x00538A,    // Strong Blue
            0xFF7A5C,    // Strong Yellowish Pink
            0x53377A,    // Strong Violet
            0xFF8E00,    // Vivid Orange Yellow
            0xB32851,    // Strong Purplish Red
            0xF4C800,    // Vivid Greenish Yellow
            0x7F180D,    // Strong Reddish Brown
            0x93AA00,    // Vivid Yellowish Green
            0x593315,    // Deep Yellowish Brown
            0xF13A13,    // Vivid Reddish Orange
            0x232C16     // Dark Olive Green
        };

    private final Minecraft mc;
    private final Map<SchematicPlacement, ImmutableMap<String, Box>> placements = new HashMap<>();
    private final Color4f colorPos1 = new Color4f(1f, 0.0625f, 0.0625f);
    private final Color4f colorPos2 = new Color4f(0.0625f, 0.0625f, 1f);
    private final Color4f colorOverlapping = new Color4f(1f, 0.0625f, 1f);
    private final Color4f colorX = new Color4f(1f, 0.25f, 0.25f);
    private final Color4f colorY = new Color4f(0.25f, 1f, 0.25f);
    private final Color4f colorZ = new Color4f(0.25f, 0.25f, 1f);
    private final Color4f colorArea = new Color4f(1f, 1f, 1f);
    private final Color4f colorBoxPlacementSelected = new Color4f(0x16 / 255f, 1f, 1f);
    private final Color4f colorSelectedCorner = new Color4f(0f, 1f, 1f);
    private final Color4f colorAreaOrigin = new Color4f(1f, 0x90 / 255f, 0x10 / 255f);

    private long infoUpdateTime;
    private final List<String> blockInfoLines = new ArrayList<>();
    private int blockInfoX;
    private int blockInfoY;

    private OverlayRenderer()
    {
        this.mc = Minecraft.getInstance();
    }

    public static OverlayRenderer getInstance()
    {
        return INSTANCE;
    }

    public void updatePlacementCache()
    {
        this.placements.clear();
        List<SchematicPlacement> list = DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();

        for (SchematicPlacement placement : list)
        {
            if (placement.isEnabled())
            {
                this.placements.put(placement, placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
            }
        }
    }

    public void renderBoxes(ProfilerFiller profiler)
    {
        profiler.push("render");
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection currentSelection = sm.getCurrentSelection();
        boolean renderAreas = currentSelection != null && Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue();
        boolean renderPlacements = this.placements.isEmpty() == false && Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING.getBooleanValue();
        boolean isProjectMode = DataManager.getSchematicProjectsManager().hasProjectOpen();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = isProjectMode ? 3f : 1.5f;

        if (renderAreas || renderPlacements || isProjectMode)
        {
            profiler.popPush("render_areas");
            if (renderAreas)
            {
                profiler.push("selection_boxes");
                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (Box box : currentSelection.getAllSubRegionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, null);
                }

                BlockPos origin = currentSelection.getExplicitOrigin();

                if (origin != null)
                {
                    profiler.popPush("area_sides");
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = Color4f.fromColor(this.colorAreaOrigin, 0.4f);
                        fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(origin, origin, colorTmp);
                    }

                    profiler.popPush("block_outlines");
                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color, false);
                }

                profiler.pop();
            }

            profiler.popPush("render_placements");
            if (renderPlacements)
            {
                SchematicPlacementManager spm = DataManager.getSchematicPlacementManager();
                SchematicPlacement currentPlacement = spm.getSelectedSchematicPlacement();

                profiler.push("placement");
                for (Map.Entry<SchematicPlacement, ImmutableMap<String, Box>> entry : this.placements.entrySet())
                {
                    SchematicPlacement schematicPlacement = entry.getKey();
                    ImmutableMap<String, Box> boxMap = entry.getValue();
                    boolean origin = schematicPlacement.getSelectedSubRegionPlacement() == null;

                    profiler.popPush("selection_boxes");
                    for (Map.Entry<String, Box> entryBox : boxMap.entrySet())
                    {
                        String boxName = entryBox.getKey();
                        boolean boxSelected = schematicPlacement == currentPlacement && (origin || boxName.equals(schematicPlacement.getSelectedSubRegionName()));
                        BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                        this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, schematicPlacement);
                    }
                    profiler.popPush("block_outlines");

                    Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color, false);

                    profiler.popPush("area_sides");
                    if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX.getBooleanValue())
                    {
                        Box box = schematicPlacement.getEclosingBox();

                        if (schematicPlacement.shouldRenderEnclosingBox() && box != null)
                        {
                            fi.dy.masa.malilib.render.RenderUtils.renderAreaOutline(box.getPos1(), box.getPos2(), 1f, color, color, color);

                            if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX_SIDES.getBooleanValue())
                            {
                                float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
                                color = new Color4f(color.r, color.g, color.b, alpha);
                                fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(box.getPos1(), box.getPos2(), color);
                            }
                        }
                    }

                }

                profiler.pop();
            }

            profiler.popPush("render_projects");
            if (isProjectMode)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(project.getOrigin(), expand, 4f, this.colorOverlapping, false);
                }
            }
        }

        profiler.pop();
    }

    public void renderSelectionBox(Box box, BoxType boxType, float expand,
                                   float lineWidthBlockBox, float lineWidthArea, @Nullable SchematicPlacement placement)
    {
        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        if (pos1 == null && pos2 == null)
        {
            return;
        }

        Color4f color1;
        Color4f color2;
        Color4f colorX;
        Color4f colorY;
        Color4f colorZ;

        switch (boxType)
        {
            case AREA_SELECTED:
                colorX = this.colorX;
                colorY = this.colorY;
                colorZ = this.colorZ;
                break;
            case AREA_UNSELECTED:
                colorX = this.colorArea;
                colorY = this.colorArea;
                colorZ = this.colorArea;
                break;
            case PLACEMENT_SELECTED:
                colorX = this.colorBoxPlacementSelected;
                colorY = this.colorBoxPlacementSelected;
                colorZ = this.colorBoxPlacementSelected;
                break;
            case PLACEMENT_UNSELECTED:
                Color4f color = placement.getBoxesBBColor();
                colorX = color;
                colorY = color;
                colorZ = color;
                break;
            default:
                return;
        }

        Color4f sideColor;

        if (boxType == BoxType.PLACEMENT_SELECTED)
        {
            color1 = this.colorBoxPlacementSelected;
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else if (boxType == BoxType.PLACEMENT_UNSELECTED)
        {
            color1 = placement.getBoxesBBColor();
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else
        {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
            sideColor = Color4f.fromColor(Configs.Colors.AREA_SELECTION_BOX_SIDE_COLOR.getIntegerValue());
        }

        if (pos1 != null && pos2 != null)
        {
            if (pos1.equals(pos2) == false)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ);

                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue())
                    ||
                     ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                       Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES.getBooleanValue()))
                {
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos1, pos2, sideColor);
                }

                if (box.getSelectedCorner() == Corner.CORNER_1)
                {
                    Color4f color = Color4f.fromColor(this.colorPos1, 0.4f);
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos1, pos1, color);
                }
                else if (box.getSelectedCorner() == Corner.CORNER_2)
                {
                    Color4f color = Color4f.fromColor(this.colorPos2, 0.4f);
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos2, pos2, color);
                }

                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, false);
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, false);
            }
            else
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, false);
            }
        }
        else
        {
            if (pos1 != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, false);
            }

            if (pos2 != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, false);
            }
        }
    }

    public void renderSchematicVerifierMismatches(ProfilerFiller profiler)
    {
        profiler.push("render_mismatches");

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();

            List<MismatchRenderPos> list = verifier.getSelectedMismatchPositionsForRender();

            if (list.isEmpty() == false)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                BlockHitResult trace = RayTraceUtils.traceToPositions(posList, entity, 128);
                BlockPos posLook = trace != null && trace.getType() == HitResult.Type.BLOCK ? trace.getBlockPos() : null;
                this.renderSchematicMismatches(list, posLook, profiler);
            }
        }

        profiler.pop();
    }

    private void renderSchematicMismatches(List<MismatchRenderPos> posList, @Nullable BlockPos lookPos, ProfilerFiller profiler)
    {
        profiler.push("batched_lines");
        RenderContext ctx = new RenderContext(() -> "litematica:schematic_mistaches/batched_lines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);
        BufferBuilder buffer = ctx.getBuilder();
		float lineWidth = 2.0f;

        MismatchRenderPos lookedEntry = null;
        MismatchRenderPos prevEntry = null;
        boolean connections = Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();

        for (MismatchRenderPos entry : posList)
        {
            Color4f color = entry.type.getColor();

            if (entry.pos.equals(lookPos) == false)
            {
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(entry.pos, color, 0.002, lineWidth, buffer);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, lineWidth, buffer);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            if (connections && prevEntry != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), lineWidth, buffer);
            }

            try
            {
                MeshData meshData = buffer.build();

                if (meshData != null)
                {
                    ctx.draw(meshData, false, true);
                    meshData.close();
                }

                ctx.reset();
            }
            catch (Exception ignored) { }

            profiler.popPush("outlines");
	        lineWidth = 6f;
            buffer = ctx.start(() -> "litematica:schematic_mistaches/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);

            fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(lookPos, lookedEntry.type.getColor(), 0.002, lineWidth, buffer);
        }

        try
        {
            MeshData meshData = buffer.build();

            if (meshData != null)
            {
                ctx.draw(meshData, false, true);
                meshData.close();
            }

            ctx.reset();
        }
        catch (Exception ignored) { }

        profiler.popPush("sides");
        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue())
        {
            buffer = ctx.start(() -> "litematica:schematic_mistaches/side_quads", MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL);

            float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA.getDoubleValue();

            for (MismatchRenderPos entry : posList)
            {
                Color4f color = entry.type.getColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                fi.dy.masa.malilib.render.RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, buffer);
            }

            try
            {
                MeshData meshData = buffer.build();

                if (meshData != null)
                {
                    ctx.draw(meshData, false, false);
                    meshData.close();
                }

                ctx.close();
            }
            catch (Exception ignored) { }
        }

        profiler.pop();
    }

    public void renderHoverInfo(GuiContext ctx, ProfilerFiller profiler)
    {
        profiler.push("render_hover_info");

        if (mc.level != null && mc.player != null)
        {
            boolean infoOverlayKeyActive = Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld();
            boolean verifierOverlayRendered = false;

            profiler.popPush("render_verifier_overlay");
            if (infoOverlayKeyActive && Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                verifierOverlayRendered = this.renderVerifierOverlay(ctx);
            }

            boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_ENABLED.getBooleanValue();
            boolean renderBlockInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            profiler.popPush("generic_trace");
            if (renderBlockInfoLines || renderBlockInfoOverlay)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
                traceWrapper = RayTraceUtils.getGenericTrace(mc.level, entity, 10, true, targetFluids, false);
            }

            if (traceWrapper != null &&
                (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK ||
                 traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK))
            {
                profiler.popPush("render_block_lines");
                if (renderBlockInfoLines)
                {
                    this.renderBlockInfoLines(ctx, traceWrapper);
                }

                profiler.popPush("render_block_overlay");
                if (renderBlockInfoOverlay)
                {
                    this.renderBlockInfoOverlay(ctx, traceWrapper);
                }
            }
        }

        profiler.pop();
    }

    private void renderBlockInfoLines(GuiContext ctx, RayTraceWrapper traceWrapper)
    {
        long currentTime = System.currentTimeMillis();

        // Only update the text once per game tick
        if (currentTime - this.infoUpdateTime >= 50)
        {
            this.updateBlockInfoLines(traceWrapper);
            this.infoUpdateTime = currentTime;
        }

        int x = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_X.getIntegerValue();
        int y = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_Y.getIntegerValue();
        double fontScale = Configs.InfoOverlays.BLOCK_INFO_LINES_FONT_SCALE.getDoubleValue();
        int textColor = 0xFFFFFFFF;
        int bgColor = 0xA0505050;
        HudAlignment alignment = (HudAlignment) Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getOptionListValue();
        boolean useBackground = true;
        boolean useShadow = false;

        fi.dy.masa.malilib.render.RenderUtils.renderText(ctx, x, y, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines);
    }

    private boolean renderVerifierOverlay(GuiContext ctx)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            BlockHitResult trace = RayTraceUtils.traceToPositions(posList, entity, 128);

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
                BlockPos pos = trace.getBlockPos();

                if (DataManager.getInstance().hasIntegratedServer() == false)
                {
                    EntitiesDataStorage.getInstance().requestBlockEntity(mc.level, pos);
                }

                BlockMismatch mismatch = verifier.getMismatchForPosition(pos);

                if (mismatch != null && worldSchematic != null)
                {
                    BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                    BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();
                    int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
                    int invHeight = RenderUtils.renderInventoryOverlays(ctx, align, offY, worldSchematic, ctx.mc().level, pos);
                    this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight);
                    info.render(ctx, this.blockInfoX, this.blockInfoY);
                    return true;
                }
            }
        }

        return false;
    }

    private void renderBlockInfoOverlay(GuiContext ctx, RayTraceWrapper traceWrapper)
    {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState voidAir = Blocks.VOID_AIR.defaultBlockState();
        Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
        Level worldClient = WorldUtils.getBestWorld(ctx.mc());
        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();

        if (ctx.mc().level == null || worldClient == null || worldSchematic == null)
        {
            return;
        }
        BlockState stateClient = ctx.mc().level.getBlockState(pos);
        BlockState stateSchematic = worldSchematic.getBlockState(pos);
        boolean hasInvClient = InventoryUtils.getTargetInventory(worldClient, pos) != null;
        boolean hasInvSchematic = InventoryUtils.getTargetInventory(worldSchematic, pos) != null;
        int invHeight = 0;
        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();

        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
        ItemUtils.setItemForBlock(ctx.mc().level, pos, stateClient);

        if (hasInvClient && hasInvSchematic)
        {
            invHeight = RenderUtils.renderInventoryOverlays(ctx, align, offY, worldSchematic, worldClient, pos);
        }
        else if (hasInvClient)
        {
            invHeight = RenderUtils.renderInventoryOverlay(ctx, align, LeftRight.RIGHT, offY, worldClient, pos);
        }
        else if (hasInvSchematic)
        {
            invHeight = RenderUtils.renderInventoryOverlay(ctx, align, LeftRight.LEFT, offY, worldSchematic, pos);
        }

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air && stateSchematic != voidAir)
        {
            BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight);
            info.toggleUseBackgroundMask(true);
            info.render(ctx, this.blockInfoX, this.blockInfoY);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight);
            info.toggleUseBackgroundMask(true);
            info.render(ctx, this.blockInfoX, this.blockInfoY);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockInfo info = new BlockInfo(stateSchematic, "litematica.gui.label.block_info.state_schematic");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight);
            info.toggleUseBackgroundMask(true);
            info.render(ctx, this.blockInfoX, this.blockInfoY);
        }
    }

    public void requestBlockEntityAt(Level world, BlockPos pos)
    {
        if (!(world instanceof ServerLevel))
        {
            EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock)
            {
                ChestType type = state.getValue(ChestBlock.TYPE);

                if (type != ChestType.SINGLE)
                {
                    BlockPos posAdj = pos.relative(ChestBlock.getConnectedDirection(state));
                    EntitiesDataStorage.getInstance().requestBlockEntity(world, posAdj);
                }
            }
        }
    }

    public static int calculateCompatYShift()
    {
        // Shift Overlay Window down by getJadeShift() if Jade is active.
        if (JadeCompat.hasJade())
        {
            return JadeCompat.getJadeShift();
        }

        return 0;
    }

    protected void getOverlayPosition(BlockInfoAlignment align, int width, int height, int offY, int invHeight)
    {
        switch (align)
        {
            case CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = GuiUtils.getScaledWindowHeight() / 2 + offY;
                break;
            case TOP_CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = invHeight + offY + (invHeight > 0 ? offY : 0);
                this.blockInfoY += invHeight > 0 ? 0 : calculateCompatYShift();
                break;
        }
    }

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper)
    {
        this.blockInfoLines.clear();

        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();
        BlockState stateClient = mc.level.getBlockState(pos);
        BlockState voidAir = Blocks.VOID_AIR.defaultBlockState();

        Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
        BlockState stateSchematic = worldSchematic.getBlockState(pos);
        String ul = GuiBase.TXT_UNDERLINE;

        if (stateSchematic != stateClient && stateClient.isAir() == false && stateSchematic.isAir() == false && stateSchematic != voidAir)
        {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);

            this.blockInfoLines.add("");
            this.blockInfoLines.add(ul + "Client:");
            this.addBlockInfoLines(stateClient);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);
        }
    }

    private void addBlockInfoLines(BlockState state)
    {
        this.blockInfoLines.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        this.blockInfoLines.addAll(BlockUtils.getFormattedBlockStateProperties(state));
    }

    public void renderSchematicRebuildTargetingOverlay(ProfilerFiller profiler)
    {
        profiler.push("rebuild_trace");
        RayTraceWrapper traceWrapper = null;
        Color4f color = null;
        boolean direction = false;
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();

        if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL_EXCEPT.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_EXCEPT_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_BLOCK.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.level, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
            direction = true;
        }

        profiler.popPush("render_target_overlay");
        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();

            if (direction)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                        entity, pos, trace.getDirection(), trace.getLocation(), color);
            }
            else
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlaySimple(
                        entity, pos, trace.getDirection(), color);
            }
        }

        profiler.pop();
    }

    public void renderPreviewFrame(GuiContext ctx, ProfilerFiller profiler)
    {
        profiler.push("render_preview_frame");
        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        int x = width >= height ? (width - height) / 2 : 0;
        int y = height >= width ? (height - width) / 2 : 0;
        int longerSide = Math.min(width, height);

        fi.dy.masa.malilib.render.RenderUtils.drawOutline(ctx, x, y, longerSide, longerSide, 2, 0xFFFFFFFF);
        profiler.pop();
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED
    }
}

package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
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

    private final MinecraftClient mc;
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
        this.mc = MinecraftClient.getInstance();
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

    public void renderBoxes(Matrix4f matrix4f, Profiler profiler)
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
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.blend(true);
//            fi.dy.masa.malilib.render.RenderUtils.depthTest(true);
//            fi.dy.masa.malilib.render.RenderUtils.depthMask(false);

            profiler.swap("render_areas");
            if (renderAreas)
            {
                fi.dy.masa.malilib.render.RenderUtils.polygonOffset(true);
                fi.dy.masa.malilib.render.RenderUtils.polygonOffset(-1.2f, -0.2f);

                profiler.push("selection_boxes");
                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (Box box : currentSelection.getAllSubRegionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, null, matrix4f);
                }

                BlockPos origin = currentSelection.getExplicitOrigin();

                if (origin != null)
                {
                    profiler.swap("area_sides");
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = Color4f.fromColor(this.colorAreaOrigin, 0.4f);
                        fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(origin, origin, colorTmp, matrix4f);
                    }

                    profiler.swap("block_outlines");
                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color);
                }

                fi.dy.masa.malilib.render.RenderUtils.polygonOffset(0f, 0f);
                fi.dy.masa.malilib.render.RenderUtils.polygonOffset(false);
                profiler.pop();
            }

            profiler.swap("render_placements");
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

                    profiler.swap("selection_boxes");
                    for (Map.Entry<String, Box> entryBox : boxMap.entrySet())
                    {
                        String boxName = entryBox.getKey();
                        boolean boxSelected = schematicPlacement == currentPlacement && (origin || boxName.equals(schematicPlacement.getSelectedSubRegionName()));
                        BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                        this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, schematicPlacement, matrix4f);
                    }
                    profiler.swap("block_outlines");

                    Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color);

                    profiler.swap("area_sides");
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
                                fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(box.getPos1(), box.getPos2(), color, matrix4f);
                            }
                        }
                    }

                }

                profiler.pop();
            }

            profiler.swap("render_projects");
            if (isProjectMode)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(project.getOrigin(), expand, 4f, this.colorOverlapping);
                }
            }

            fi.dy.masa.malilib.render.RenderUtils.depthMask(true);
        }

        profiler.pop();
    }

    public void renderSelectionBox(Box box, BoxType boxType, float expand,
            float lineWidthBlockBox, float lineWidthArea, @Nullable SchematicPlacement placement, Matrix4f matrix4f)
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
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrix4f);
                }

                if (box.getSelectedCorner() == Corner.CORNER_1)
                {
                    Color4f color = Color4f.fromColor(this.colorPos1, 0.4f);
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos1, pos1, color, matrix4f);
                }
                else if (box.getSelectedCorner() == Corner.CORNER_2)
                {
                    Color4f color = Color4f.fromColor(this.colorPos2, 0.4f);
                    fi.dy.masa.malilib.render.RenderUtils.renderAreaSides(pos2, pos2, color, matrix4f);
                }

                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1);
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2);
            }
            else
            {
                //RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, matrix4f, this.mc);
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, matrix4f);
            }
        }
        else
        {
            if (pos1 != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1);
            }

            if (pos2 != null)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2);
            }
        }
    }

    public void renderSchematicVerifierMismatches(Matrix4f matrix4f, Profiler profiler)
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
                this.renderSchematicMismatches(list, posLook, matrix4f, profiler);
            }
        }

        profiler.pop();
    }

    private void renderSchematicMismatches(List<MismatchRenderPos> posList, @Nullable BlockPos lookPos, Matrix4f matrix4f, Profiler profiler)
    {
        profiler.push("batched_lines");
//        fi.dy.masa.malilib.render.RenderUtils.depthTest(false);
//        fi.dy.masa.malilib.render.RenderUtils.depthMask(false);

        RenderSystem.lineWidth(2f);

        // VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR
        RenderContext ctx = new RenderContext(MaLiLibPipelines.LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);
        BufferBuilder buffer = ctx.getBuilder();
        MatrixStack matrices = new MatrixStack();

        MismatchRenderPos lookedEntry = null;
        MismatchRenderPos prevEntry = null;
        boolean connections = Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();

        matrices.push();

        for (MismatchRenderPos entry : posList)
        {
            Color4f color = entry.type.getColor();

            if (entry.pos.equals(lookPos) == false)
            {
                //RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.pos, color, 0.002, buffer, this.mc);
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(entry.pos, color, 0.002, buffer, matrices);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                //RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, this.mc);
                fi.dy.masa.malilib.render.RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, matrices);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            if (connections && prevEntry != null)
            {
                //RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, this.mc);
                fi.dy.masa.malilib.render.RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, matrices);
            }

            matrices.pop();

            try
            {
                ctx.lineWidth(2f);
                ctx.draw(buffer.endNullable(), true);
                ctx.reset();
            }
            catch (Exception ignored) { }

            profiler.swap("outlines");

            /*
            buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
             */
            buffer = ctx.start(MaLiLibPipelines.LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);

            matrices.push();

            RenderSystem.lineWidth(6f);
            //RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPos, lookedEntry.type.getColor(), 0.002, buffer, this.mc);
            fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(lookPos, lookedEntry.type.getColor(), 0.002, buffer, matrices);

            matrices.pop();
        }

        try
        {
            ctx.lineWidth(6f);
            ctx.draw(buffer.endNullable(), true);
            ctx.reset();
        }
        catch (Exception ignored) { }

        profiler.swap("sides");
        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue())
        {
            fi.dy.masa.malilib.render.RenderUtils.blend(true);
            fi.dy.masa.malilib.render.RenderUtils.culling(false);

            //buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer = ctx.start(MaLiLibPipelines.getPositionColorSimple());

            float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA.getDoubleValue();

            for (MismatchRenderPos entry : posList)
            {
                Color4f color = entry.type.getColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                fi.dy.masa.malilib.render.RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, buffer);
            }

            try
            {
                ctx.draw(buffer.endNullable());
                ctx.close();
            }
            catch (Exception ignored) { }

//            fi.dy.masa.malilib.render.RenderUtils.blend(false);
        }

//        fi.dy.masa.malilib.render.RenderUtils.culling(true);
//        fi.dy.masa.malilib.render.RenderUtils.depthMask(true);
//        fi.dy.masa.malilib.render.RenderUtils.depthTest(true);
        profiler.pop();
    }

    public void renderHoverInfo(MinecraftClient mc, DrawContext drawContext, Profiler profiler)
    {
        profiler.push("render_hover_info");

        if (mc.world != null && mc.player != null)
        {
            boolean infoOverlayKeyActive = Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld();
            boolean verifierOverlayRendered = false;

            profiler.swap("render_verifier_overlay");
            if (infoOverlayKeyActive && Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                verifierOverlayRendered = this.renderVerifierOverlay(mc, drawContext);
            }

            boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_ENABLED.getBooleanValue();
            boolean renderBlockInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            profiler.swap("generic_trace");
            if (renderBlockInfoLines || renderBlockInfoOverlay)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
                traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 10, true, targetFluids, false);
            }

            if (traceWrapper != null &&
                (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK ||
                 traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK))
            {
                profiler.swap("render_block_lines");
                if (renderBlockInfoLines)
                {
                    this.renderBlockInfoLines(traceWrapper, mc, drawContext);
                }

                profiler.swap("render_block_overlay");
                if (renderBlockInfoOverlay)
                {
                    this.renderBlockInfoOverlay(traceWrapper, mc, drawContext);
                }
            }
        }

        profiler.pop();
    }

    private void renderBlockInfoLines(RayTraceWrapper traceWrapper, MinecraftClient mc, DrawContext drawContext)
    {
        long currentTime = System.currentTimeMillis();

        // Only update the text once per game tick
        if (currentTime - this.infoUpdateTime >= 50)
        {
            this.updateBlockInfoLines(traceWrapper, mc);
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

        fi.dy.masa.malilib.render.RenderUtils.renderText(x, y, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines, drawContext);
    }

    private boolean renderVerifierOverlay(MinecraftClient mc, DrawContext drawContext)
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
                World worldSchematic = SchematicWorldHandler.getSchematicWorld();
                BlockPos pos = trace.getBlockPos();

                if (DataManager.getInstance().hasIntegratedServer() == false)
                {
                    EntitiesDataStorage.getInstance().requestBlockEntity(mc.world, pos);
                }

                BlockMismatch mismatch = verifier.getMismatchForPosition(pos);

                if (mismatch != null && worldSchematic != null)
                {
                    BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                    BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();
                    int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
                    int invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, mc.world, pos, mc, drawContext);
                    this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
                    info.render(this.blockInfoX, this.blockInfoY, mc, drawContext);
                    return true;
                }
            }
        }

        return false;
    }

    private void renderBlockInfoOverlay(RayTraceWrapper traceWrapper, MinecraftClient mc, DrawContext drawContext)
    {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState voidAir = Blocks.VOID_AIR.getDefaultState();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        World worldClient = WorldUtils.getBestWorld(mc);
        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();

        if (mc.world == null || worldClient == null || worldSchematic == null)
        {
            return;
        }
        BlockState stateClient = mc.world.getBlockState(pos);
        BlockState stateSchematic = worldSchematic.getBlockState(pos);
        boolean hasInvClient = InventoryUtils.getTargetInventory(worldClient, pos) != null;
        boolean hasInvSchematic = InventoryUtils.getTargetInventory(worldSchematic, pos) != null;
        int invHeight = 0;
        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();

        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
        ItemUtils.setItemForBlock(mc.world, pos, stateClient);

        if (hasInvClient && hasInvSchematic)
        {
            invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, worldClient, pos, mc, drawContext);
        }
        else if (hasInvClient)
        {
            invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.RIGHT, offY, worldClient, pos, mc, drawContext);
        }
        else if (hasInvSchematic)
        {
            invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.LEFT, offY, worldSchematic, pos, mc, drawContext);
        }

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air && stateSchematic != voidAir)
        {
            BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.toggleUseBackgroundMask(true);
            info.render(this.blockInfoX, this.blockInfoY, mc, drawContext);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.toggleUseBackgroundMask(true);
            info.render(this.blockInfoX, this.blockInfoY, mc, drawContext);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockInfo info = new BlockInfo(stateSchematic, "litematica.gui.label.block_info.state_schematic");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.toggleUseBackgroundMask(true);
            info.render(this.blockInfoX, this.blockInfoY, mc, drawContext);
        }
    }

    public void requestBlockEntityAt(World world, BlockPos pos)
    {
        if (!(world instanceof ServerWorld))
        {
            EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock)
            {
                ChestType type = state.get(ChestBlock.CHEST_TYPE);

                if (type != ChestType.SINGLE)
                {
                    BlockPos posAdj = pos.offset(ChestBlock.getFacing(state));
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

    protected void getOverlayPosition(BlockInfoAlignment align, int width, int height, int offY, int invHeight, MinecraftClient mc)
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

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper, MinecraftClient mc)
    {
        this.blockInfoLines.clear();

        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();
        BlockState stateClient = mc.world.getBlockState(pos);
        BlockState voidAir = Blocks.VOID_AIR.getDefaultState();

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
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
        this.blockInfoLines.add(String.valueOf(Registries.BLOCK.getId(state.getBlock())));
        this.blockInfoLines.addAll(BlockUtils.getFormattedBlockStateProperties(state));
    }

    public void renderSchematicRebuildTargetingOverlay(Matrix4f matrix4f, Profiler profiler)
    {
        profiler.push("rebuild_trace");
        RayTraceWrapper traceWrapper = null;
        Color4f color = null;
        boolean direction = false;
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();

        if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL_EXCEPT.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_EXCEPT_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_BLOCK.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
            direction = true;
        }

        profiler.swap("render_target_overlay");
        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();

//            fi.dy.masa.malilib.render.RenderUtils.depthMask(false);
//            fi.dy.masa.malilib.render.RenderUtils.culling(false);
            fi.dy.masa.malilib.render.RenderUtils.blend(true);
            fi.dy.masa.malilib.render.RenderUtils.polygonOffset(true);
            fi.dy.masa.malilib.render.RenderUtils.polygonOffset(-0.8f, -1.8f);

            if (direction)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                        entity, pos, trace.getSide(), trace.getPos(), color, matrix4f);
            }
            else
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlaySimple(
                        entity, pos, trace.getSide(), color, matrix4f);
            }

            fi.dy.masa.malilib.render.RenderUtils.polygonOffset(false);
            fi.dy.masa.malilib.render.RenderUtils.blend(false);
//            fi.dy.masa.malilib.render.RenderUtils.culling(true);
//            fi.dy.masa.malilib.render.RenderUtils.depthMask(true);
        }

        profiler.pop();
    }

    public void renderPreviewFrame(MinecraftClient mc, DrawContext drawContext, Profiler profiler)
    {
        profiler.push("render_preview_frame");
        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        int x = width >= height ? (width - height) / 2 : 0;
        int y = height >= width ? (height - width) / 2 : 0;
        int longerSide = Math.min(width, height);

        fi.dy.masa.malilib.render.RenderUtils.drawOutline(x, y, longerSide, longerSide, 2, 0xFFFFFFFF);
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

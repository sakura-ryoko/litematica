package fi.dy.masa.litematica.render;

import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class RenderUtils
{
    private static final LocalRandom RAND = new LocalRandom(0);

    public static int getMaxStringRenderLength(List<String> list)
    {
        int length = 0;

        for (String str : list)
        {
            length = Math.max(length, StringUtils.getStringWidth(str));
        }

        return length;
    }

    static void startDrawingLines()
    {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.applyModelViewMatrix();
        //buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }

    public static void renderBlockOutline(BlockPos pos, float expand, float lineWidth, Color4f color, MinecraftClient mc)
    {
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        startDrawingLines();
        drawBlockBoundingBoxOutlinesBatchedLines(pos, color, expand, buffer, mc);

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception e)
        {
            Litematica.logger.error("renderBlockOutline: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
        }
    }

    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, Color4f color,
            double expand, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        float minX = (float) (pos.getX() - dx - expand);
        float minY = (float) (pos.getY() - dy - expand);
        float minZ = (float) (pos.getZ() - dz - expand);
        float maxX = (float) (pos.getX() - dx + expand + 1);
        float maxY = (float) (pos.getY() - dy + expand + 1);
        float maxZ = (float) (pos.getZ() - dz + expand + 1);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void drawConnectingLineBatchedLines(BlockPos pos1, BlockPos pos2, boolean center,
            Color4f color, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        float x1 = (float) (pos1.getX() - dx);
        float y1 = (float) (pos1.getY() - dy);
        float z1 = (float) (pos1.getZ() - dz);
        float x2 = (float) (pos2.getX() - dx);
        float y2 = (float) (pos2.getY() - dy);
        float z2 = (float) (pos2.getZ() - dz);

        if (center)
        {
            x1 += 0.5F;
            y1 += 0.5F;
            z1 += 0.5F;
            x2 += 0.5F;
            y2 += 0.5F;
            z2 += 0.5F;
        }

        buffer.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a);
        buffer.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a);
    }

    public static void renderBlockOutlineOverlapping(BlockPos pos, float expand, float lineWidth,
            Color4f color1, Color4f color2, Color4f color3, Matrix4f matrix4f, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        final float minX = (float) (pos.getX() - dx - expand);
        final float minY = (float) (pos.getY() - dy - expand);
        final float minZ = (float) (pos.getZ() - dz - expand);
        final float maxX = (float) (pos.getX() - dx + expand + 1);
        final float maxY = (float) (pos.getY() - dy + expand + 1);
        final float maxZ = (float) (pos.getZ() - dz + expand + 1);

        RenderSystem.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        startDrawingLines();

        // Min corner
        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
        buffer.vertex(maxX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);

        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
        buffer.vertex(minX, maxY, minZ).color(color1.r, color1.g, color1.b, color1.a);

        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
        buffer.vertex(minX, minY, maxZ).color(color1.r, color1.g, color1.b, color1.a);

        // Max corner
        buffer.vertex(minX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);

        buffer.vertex(maxX, minY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);

        buffer.vertex(maxX, maxY, minZ).color(color2.r, color2.g, color2.b, color2.a);
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);

        // The rest of the edges
        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);

        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);

        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);

        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a);

        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);

        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a);

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception e)
        {
            Litematica.logger.error("renderBlockOutlineOverlapping: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
        }
    }

    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
            Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
    {
        RenderSystem.lineWidth(lineWidth);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double minX = Math.min(pos1.getX(), pos2.getX()) - dx;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz;
        double maxX = Math.max(pos1.getX(), pos2.getX()) - dx + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) - dy + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) - dz + 1;

        drawBoundingBoxEdges((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        startDrawingLines();

        drawBoundingBoxLinesX(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorX);
        drawBoundingBoxLinesY(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorY);
        drawBoundingBoxLinesZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorZ);

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception ignored) { }
    }

    private static void drawBoundingBoxLinesX(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

    public static void renderAreaSides(BlockPos pos1, BlockPos pos2, Color4f color, Matrix4f matrix4f, MinecraftClient mc)
    {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        renderAreaSidesBatched(pos1, pos2, color, 0.002, buffer, mc);

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception ignored) { }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color,
            double expand, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;
        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color, buffer);
    }

    public static void renderAreaOutlineNoCorners(BlockPos pos1, BlockPos pos2,
            float lineWidth, Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
    {
        final int xMin = Math.min(pos1.getX(), pos2.getX());
        final int yMin = Math.min(pos1.getY(), pos2.getY());
        final int zMin = Math.min(pos1.getZ(), pos2.getZ());
        final int xMax = Math.max(pos1.getX(), pos2.getX());
        final int yMax = Math.max(pos1.getY(), pos2.getY());
        final int zMax = Math.max(pos1.getZ(), pos2.getZ());

        final double expand = 0.001;
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        final float dxMin = (float) (-dx - expand);
        final float dyMin = (float) (-dy - expand);
        final float dzMin = (float) (-dz - expand);
        final float dxMax = (float) (-dx + expand);
        final float dyMax = (float) (-dy + expand);
        final float dzMax = (float) (-dz + expand);

        final float minX = xMin + dxMin;
        final float minY = yMin + dyMin;
        final float minZ = zMin + dzMin;
        final float maxX = xMax + dxMax;
        final float maxY = yMax + dyMax;
        final float maxZ = zMax + dzMax;

        int start, end;

        RenderSystem.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        startDrawingLines();

        // Edges along the X-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
            buffer.vertex(end   + dxMax, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
            buffer.vertex(end   + dxMax, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
            buffer.vertex(end   + dxMax, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
            buffer.vertex(end   + dxMax, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
        }

        // Edges along the Y-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
            buffer.vertex(minX, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
            buffer.vertex(maxX + 1, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
            buffer.vertex(minX, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
            buffer.vertex(maxX + 1, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
        }

        // Edges along the Z-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
            buffer.vertex(minX, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
            buffer.vertex(maxX + 1, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
            buffer.vertex(minX, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
            buffer.vertex(maxX + 1, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
        }

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception ignored) { }
    }

    /**
     * Assumes a BufferBuilder in the GL_LINES mode has been initialized
     */
    public static void drawBlockModelOutlinesBatched(BakedModel model, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.PositionUtils.ALL_DIRECTIONS)
        {
            renderModelQuadOutlines(model, state, pos, side, color, expand, buffer);
        }

        renderModelQuadOutlines(model, state, pos, null, color, expand, buffer);
    }

    public static void renderModelQuadOutlines(BakedModel model, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer)
    {
        try
        {
            renderModelQuadOutlines(pos, buffer, color, model.getQuads(state, side, RAND));
        }
        catch (Exception ignore) {}
    }

    private static void renderModelQuadOutlines(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            renderQuadOutlinesBatched(pos, buffer, color, quads.get(i).getVertexData());
        }
    }

    private static void renderQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final int vertexSize = vertexData.length / 4;
        final float fx[] = new float[4];
        final float fy[] = new float[4];
        final float fz[] = new float[4];

        for (int index = 0; index < 4; ++index)
        {
            fx[index] = x + Float.intBitsToFloat(vertexData[index * vertexSize    ]);
            fy[index] = y + Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
            fz[index] = z + Float.intBitsToFloat(vertexData[index * vertexSize + 2]);
        }

        buffer.vertex(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a);
        buffer.vertex(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a);

        buffer.vertex(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a);
        buffer.vertex(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a);

        buffer.vertex(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a);
        buffer.vertex(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a);

        buffer.vertex(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a);
        buffer.vertex(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a);
    }

    public static void drawBlockModelQuadOverlayBatched(BakedModel model, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.PositionUtils.ALL_DIRECTIONS)
        {
            drawBlockModelQuadOverlayBatched(model, state, pos, side, color, expand, buffer);
        }

        drawBlockModelQuadOverlayBatched(model, state, pos, null, color, expand, buffer);
    }

    public static void drawBlockModelQuadOverlayBatched(BakedModel model, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer)
    {
        try
        {
            renderModelQuadOverlayBatched(pos, buffer, color, model.getQuads(state, side, RAND));
        }
        catch (Exception ignore) {}
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        //final int size = quads.size();

        for (BakedQuad quad : quads)
        {
            renderModelQuadOverlayBatched(pos, buffer, color, quad.getVertexData());
        }
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final int vertexSize = vertexData.length / 4;
        float fx, fy, fz;

        for (int index = 0; index < 4; ++index)
        {
            fx = x + Float.intBitsToFloat(vertexData[index * vertexSize    ]);
            fy = y + Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
            fz = z + Float.intBitsToFloat(vertexData[index * vertexSize + 2]);

            buffer.vertex(fx, fy, fz).color(color.r, color.g, color.b, color.a);
        }
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockBoxSideBatchedQuads(BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer)
    {
        float minX = (float) (pos.getX() - expand);
        float minY = (float) (pos.getY() - expand);
        float minZ = (float) (pos.getZ() - expand);
        float maxX = (float) (pos.getX() + expand + 1);
        float maxY = (float) (pos.getY() + expand + 1);
        float maxZ = (float) (pos.getZ() + expand + 1);

        switch (side)
        {
            case DOWN:
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                break;
        }
    }

    public static void drawBlockBoxEdgeBatchedLines(BlockPos pos, Direction.Axis axis, int cornerIndex, Color4f color, BufferBuilder buffer)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        buffer.vertex((float) minX, (float) minY, (float) minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex((float) maxX, (float) maxY, (float) maxZ).color(color.r, color.g, color.b, color.a);
    }

    public static int renderInventoryOverlays(BlockInfoAlignment align, int offY, World worldSchematic, World worldClient, BlockPos pos, MinecraftClient mc, DrawContext drawContext)
    {
        int heightSch = renderInventoryOverlay(align, LeftRight.LEFT, offY, worldSchematic, pos, mc, drawContext);
        int heightCli = renderInventoryOverlay(align, LeftRight.RIGHT, offY, worldClient, pos, mc, drawContext);

        return Math.max(heightSch, heightCli);
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
            World world, BlockPos pos, MinecraftClient mc, DrawContext drawContext)
    {
        Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);

        if (inv != null)
        {
            final InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryType(inv);
            final InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(type, inv.size());

            return renderInventoryOverlay(align, side, offY, inv, type, props, mc, drawContext);
        }

        return 0;
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
             Inventory inv, InventoryRenderType type, InventoryProperties props, MinecraftClient mc, DrawContext drawContext)
    {
        int xInv = 0;
        int yInv = 0;

        switch (align)
        {
            case CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = GuiUtils.getScaledWindowHeight() / 2 - props.height - offY;
                break;
            case TOP_CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = offY;
                break;
        }

        if      (side == LeftRight.LEFT)  { xInv -= (props.width / 2 + 4); }
        else if (side == LeftRight.RIGHT) { xInv += (props.width / 2 + 4); }

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, props.totalSlots, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, -1, mc, drawContext);

        return props.height;
    }

    /*
    private static void renderModelBrightnessColor(IBlockState state, IBakedModel model, float brightness, float r, float g, float b)
    {
        for (EnumFacing facing : EnumFacing.values())
        {
            renderModelBrightnessColorQuads(brightness, r, g, b, model.getQuads(state, facing, 0L));
        }

        renderModelBrightnessColorQuads(brightness, r, g, b, model.getQuads(state, null, 0L));
    }

    private static void renderModelBrightnessColorQuads(float brightness, float red, float green, float blue, List<BakedQuad> listQuads)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        int i = 0;

        for (int j = listQuads.size(); i < j; ++i)
        {
            BakedQuad quad = listQuads.get(i);
            bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            bufferbuilder.addVertexData(quad.getVertexData());

            if (quad.hasTintIndex())
            {
                bufferbuilder.putColorRGB_F4(red * brightness, green * brightness, blue * brightness);
            }
            else
            {
                bufferbuilder.putColorRGB_F4(brightness, brightness, brightness);
            }

            Vec3i direction = quad.getFace().getDirectionVec();
            bufferbuilder.putNormal(direction.getX(), direction.getY(), direction.getZ());

            tessellator.draw();
        }
    }
    */

    /*
    private static void renderModel(final IBlockState state, final IBakedModel model, final BlockPos pos, final int alpha)
    {
        //BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        //dispatcher.getBlockModelRenderer().renderModelBrightnessColor(model, 1f, 1f, 1f, 1f);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);

        for (final EnumFacing facing : EnumFacing.values())
        {
            renderQuads(state, pos, buffer, model.getQuads(state, facing, 0), alpha);
        }

        renderQuads(state, pos, buffer, model.getQuads(state, null, 0), alpha);
        tessellator.draw();
    }

    private static void renderQuads(final IBlockState state, final BlockPos pos, final BufferBuilder buffer, final List<BakedQuad> quads, final int alpha)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            final BakedQuad quad = quads.get(i);
            final int color = quad.getTintIndex() == -1 ? alpha | 0xffffff : getTint(state, pos, alpha, quad.getTintIndex());
            //LightUtil.renderQuadColor(buffer, quad, color);
            renderQuad(buffer, quad, color);
        }
    }

    public static void renderQuad(BufferBuilder buffer, BakedQuad quad, int auxColor)
    {
        buffer.addVertexData(quad.getVertexData());
        putQuadColor(buffer, quad, auxColor);
    }

    private static int getTint(final IBlockState state, final BlockPos pos, final int alpha, final int tintIndex)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return alpha | mc.getBlockColors().colorMultiplier(state, null, pos, tintIndex);
    }

    private static void putQuadColor(BufferBuilder buffer, BakedQuad quad, int color)
    {
        float cb = color & 0xFF;
        float cg = (color >>> 8) & 0xFF;
        float cr = (color >>> 16) & 0xFF;
        float ca = (color >>> 24) & 0xFF;
        VertexFormat format = DefaultVertexFormats.ITEM; //quad.getFormat();
        int size = format.getIntegerSize();
        int offset = format.getColorOffset() / 4; // assumes that color is aligned

        for (int i = 0; i < 4; i++)
        {
            int vc = quad.getVertexData()[offset + size * i];
            float vcr = vc & 0xFF;
            float vcg = (vc >>> 8) & 0xFF;
            float vcb = (vc >>> 16) & 0xFF;
            float vca = (vc >>> 24) & 0xFF;
            int ncr = Math.min(0xFF, (int)(cr * vcr / 0xFF));
            int ncg = Math.min(0xFF, (int)(cg * vcg / 0xFF));
            int ncb = Math.min(0xFF, (int)(cb * vcb / 0xFF));
            int nca = Math.min(0xFF, (int)(ca * vca / 0xFF));

            IBufferBuilder bufferMixin = (IBufferBuilder) buffer;
            bufferMixin.putColorRGBA(bufferMixin.getColorIndexAccessor(4 - i), ncr, ncg, ncb, nca);
        }
    }
    */

    /*
    public static void renderQuadColorSlow(BufferBuilder wr, BakedQuad quad, int auxColor)
    {
        ItemConsumer cons;

        if(wr == Tessellator.getInstance().getBuffer())
        {
            cons = getItemConsumer();
        }
        else
        {
            cons = new ItemConsumer(new VertexBufferConsumer(wr));
        }

        float b = (float)  (auxColor & 0xFF) / 0xFF;
        float g = (float) ((auxColor >>>  8) & 0xFF) / 0xFF;
        float r = (float) ((auxColor >>> 16) & 0xFF) / 0xFF;
        float a = (float) ((auxColor >>> 24) & 0xFF) / 0xFF;

        cons.setAuxColor(r, g, b, a);
        quad.pipe(cons);
    }

    public static void renderQuadColor(BufferBuilder wr, BakedQuad quad, int auxColor)
    {
        if (quad.getFormat().equals(wr.getVertexFormat())) 
        {
            wr.addVertexData(quad.getVertexData());
            ForgeHooksClient.putQuadColor(wr, quad, auxColor);
        }
        else
        {
            renderQuadColorSlow(wr, quad, auxColor);
        }
    }
    */
}

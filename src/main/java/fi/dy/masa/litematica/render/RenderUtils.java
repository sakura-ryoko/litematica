package fi.dy.masa.litematica.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;

import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.mixin.render.IMixinAbstractTexture;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PositionUtils;

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

    /*
    static void startDrawingLines()
    {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        //RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        //RenderSystem.applyModelViewMatrix();
        //buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }
     */

    // TODO --> MaLiLib
//    public static void renderBlockOutline(BlockPos pos, float expand, float lineWidth, Color4f color, MinecraftClient mc)
//    {
//        RenderSystem.lineWidth(lineWidth);
//        //RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
//
//        /*
//        startDrawingLines();
//         */
//        RenderContext ctx = new RenderContext(ShaderPipelines.DEBUG_LINE_STRIP);
//        BufferBuilder buffer = ctx.getBuilder();
//
//        drawBlockBoundingBoxOutlinesBatchedLines(pos, color, expand, buffer, mc);
//
//        try
//        {
//            ctx.draw(buffer.endNullable());
//            ctx.close();
//        }
//        catch (Exception e)
//        {
//            Litematica.LOGGER.error("renderBlockOutline: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
//        }
//    }

//    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, Color4f color,
//                                                                double expand, BufferBuilder buffer, MinecraftClient mc)
//    {
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        float minX = (float) (pos.getX() - dx - expand);
//        float minY = (float) (pos.getY() - dy - expand);
//        float minZ = (float) (pos.getZ() - dz - expand);
//        float maxX = (float) (pos.getX() - dx + expand + 1);
//        float maxY = (float) (pos.getY() - dy + expand + 1);
//        float maxZ = (float) (pos.getZ() - dz + expand + 1);
//
//        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
//    }

//    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, BlockPos relPos, Color4f color,
//                                                                double expand, BufferBuilder buffer)
//    {
//        Vec3d cameraPos = relPos.toCenterPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        float minX = (float) (pos.getX() - dx - expand);
//        float minY = (float) (pos.getY() - dy - expand);
//        float minZ = (float) (pos.getZ() - dz - expand);
//        float maxX = (float) (pos.getX() - dx + expand + 1);
//        float maxY = (float) (pos.getY() - dy + expand + 1);
//        float maxZ = (float) (pos.getZ() - dz + expand + 1);
//
//        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
//    }

//    public static void drawConnectingLineBatchedLines(BlockPos pos1, BlockPos pos2, boolean center,
//                                                      Color4f color, BufferBuilder buffer, MinecraftClient mc)
//    {
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        float x1 = (float) (pos1.getX() - dx);
//        float y1 = (float) (pos1.getY() - dy);
//        float z1 = (float) (pos1.getZ() - dz);
//        float x2 = (float) (pos2.getX() - dx);
//        float y2 = (float) (pos2.getY() - dy);
//        float z2 = (float) (pos2.getZ() - dz);
//
//        if (center)
//        {
//            x1 += 0.5F;
//            y1 += 0.5F;
//            z1 += 0.5F;
//            x2 += 0.5F;
//            y2 += 0.5F;
//            z2 += 0.5F;
//        }
//
//        buffer.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a);
//    }

//    public static void renderBlockOutlineOverlapping(BlockPos pos, float expand, float lineWidth,
//                                                     Color4f color1, Color4f color2, Color4f color3, Matrix4f matrix4f, MinecraftClient mc)
//    {
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        final float minX = (float) (pos.getX() - dx - expand);
//        final float minY = (float) (pos.getY() - dy - expand);
//        final float minZ = (float) (pos.getZ() - dz - expand);
//        final float maxX = (float) (pos.getX() - dx + expand + 1);
//        final float maxY = (float) (pos.getY() - dy + expand + 1);
//        final float maxZ = (float) (pos.getZ() - dz + expand + 1);
//
//        RenderSystem.lineWidth(lineWidth);
//
//        /*
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BuiltBuffer meshData;
//
//        startDrawingLines();
//         */
//
//        RenderContext ctx = new RenderContext(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BufferBuilder buffer = ctx.getBuilder();
//
//        // Min corner
//        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
//        buffer.vertex(maxX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
//
//        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
//        buffer.vertex(minX, maxY, minZ).color(color1.r, color1.g, color1.b, color1.a);
//
//        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a);
//        buffer.vertex(minX, minY, maxZ).color(color1.r, color1.g, color1.b, color1.a);
//
//        // Max corner
//        buffer.vertex(minX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
//
//        buffer.vertex(maxX, minY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
//
//        buffer.vertex(maxX, maxY, minZ).color(color2.r, color2.g, color2.b, color2.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a);
//
//        // The rest of the edges
//        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a);
//        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a);
//
//        try
//        {
//            /*
//            meshData = buffer.end();
//            BufferRenderer.drawWithGlobalProgram(meshData);
//            meshData.close();
//             */
//
//            ctx.drawWithShaders(buffer.end(), ShaderPipelines.DEBUG_LINE_STRIP);
//            ctx.close();
//
//        }
//        catch (Exception e)
//        {
//            Litematica.LOGGER.error("renderBlockOutlineOverlapping: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
//        }
//    }

//    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
//                                         Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
//    {
//        RenderSystem.lineWidth(lineWidth);
//
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        double minX = Math.min(pos1.getX(), pos2.getX()) - dx;
//        double minY = Math.min(pos1.getY(), pos2.getY()) - dy;
//        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz;
//        double maxX = Math.max(pos1.getX(), pos2.getX()) - dx + 1;
//        double maxY = Math.max(pos1.getY(), pos2.getY()) - dy + 1;
//        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) - dz + 1;
//
//        drawBoundingBoxEdges((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, colorX, colorY, colorZ);
//    }

//    private static void drawBoundingBoxEdges(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
//    {
//        /*
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BuiltBuffer meshData;
//
//        startDrawingLines();
//         */
//        RenderContext ctx = new RenderContext(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BufferBuilder buffer = ctx.getBuilder();
//
//        drawBoundingBoxLinesX(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorX);
//        drawBoundingBoxLinesY(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorY);
//        drawBoundingBoxLinesZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorZ);
//
//        try
//        {
//            /*
//            meshData = buffer.end();
//            BufferRenderer.drawWithGlobalProgram(meshData);
//            meshData.close();
//             */
//
//            ctx.drawWithShaders(buffer.end(), ShaderPipelines.DEBUG_LINE_STRIP);
//            ctx.close();
//
//        }
//        catch (Exception ignored) { }
//    }

//    private static void drawBoundingBoxLinesX(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
//    {
//        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//    }
//
//    private static void drawBoundingBoxLinesY(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
//    {
//        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//    }
//
//    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
//    {
//        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//
//        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
//        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
//    }

//    public static void renderAreaSides(BlockPos pos1, BlockPos pos2, Color4f color, Matrix4f matrix4f, MinecraftClient mc)
//    {
//        RenderSystem.enableBlend();
//        RenderSystem.disableCull();
//
//        /*
//        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//        BuiltBuffer meshData;
//         */
//        RenderContext ctx = new RenderContext(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//        BufferBuilder buffer = ctx.getBuilder();
//
//        renderAreaSidesBatched(pos1, pos2, color, 0.002, buffer, mc);
//
//        try
//        {
//            /*
//            meshData = buffer.end();
//            BufferRenderer.drawWithGlobalProgram(meshData);
//            meshData.close();
//             */
//
//            ctx.drawWithShaders(buffer.end(), ShaderPipelines.DEBUG_LINE_STRIP);
//            ctx.close();
//        }
//        catch (Exception ignored) { }
//
//        RenderSystem.enableCull();
//        RenderSystem.disableBlend();
//    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
//    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color,
//                                              double expand, BufferBuilder buffer, MinecraftClient mc)
//    {
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
//        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
//        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
//        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
//        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
//        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;
//
//        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color, buffer);
//    }
//
//    public static void renderAreaOutlineNoCorners(BlockPos pos1, BlockPos pos2,
//                                                  float lineWidth, Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
//    {
//        final int xMin = Math.min(pos1.getX(), pos2.getX());
//        final int yMin = Math.min(pos1.getY(), pos2.getY());
//        final int zMin = Math.min(pos1.getZ(), pos2.getZ());
//        final int xMax = Math.max(pos1.getX(), pos2.getX());
//        final int yMax = Math.max(pos1.getY(), pos2.getY());
//        final int zMax = Math.max(pos1.getZ(), pos2.getZ());
//
//        final double expand = 0.001;
//        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
//        final double dx = cameraPos.x;
//        final double dy = cameraPos.y;
//        final double dz = cameraPos.z;
//
//        final float dxMin = (float) (-dx - expand);
//        final float dyMin = (float) (-dy - expand);
//        final float dzMin = (float) (-dz - expand);
//        final float dxMax = (float) (-dx + expand);
//        final float dyMax = (float) (-dy + expand);
//        final float dzMax = (float) (-dz + expand);
//
//        final float minX = xMin + dxMin;
//        final float minY = yMin + dyMin;
//        final float minZ = zMin + dzMin;
//        final float maxX = xMax + dxMax;
//        final float maxY = yMax + dyMax;
//        final float maxZ = zMax + dzMax;
//
//        int start, end;
//
//        RenderSystem.lineWidth(lineWidth);
//
//        /*
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BuiltBuffer meshData;
//
//        startDrawingLines();
//         */
//
//        RenderContext ctx = new RenderContext(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//        BufferBuilder buffer = ctx.getBuilder();
//
//        // Edges along the X-axis
//        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMin + 1 : xMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMax : xMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(start + dxMin, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
//            buffer.vertex(end   + dxMax, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
//        }
//
//        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMin + 1 : xMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMax : xMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(start + dxMin, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
//            buffer.vertex(end   + dxMax, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a);
//        }
//
//        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMin + 1 : xMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMax : xMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(start + dxMin, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
//            buffer.vertex(end   + dxMax, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
//        }
//
//        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMin + 1 : xMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMax : xMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(start + dxMin, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
//            buffer.vertex(end   + dxMax, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a);
//        }
//
//        // Edges along the Y-axis
//        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
//        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(minX, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
//            buffer.vertex(minX, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
//        }
//
//        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(maxX + 1, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
//            buffer.vertex(maxX + 1, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a);
//        }
//
//        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
//        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(minX, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
//            buffer.vertex(minX, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
//        }
//
//        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(maxX + 1, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
//            buffer.vertex(maxX + 1, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a);
//        }
//
//        // Edges along the Z-axis
//        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
//        end   = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(minX, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//            buffer.vertex(minX, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//        }
//
//        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(maxX + 1, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//            buffer.vertex(maxX + 1, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//        }
//
//        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
//        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(minX, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//            buffer.vertex(minX, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//        }
//
//        start = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
//        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;
//
//        if (end > start)
//        {
//            buffer.vertex(maxX + 1, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//            buffer.vertex(maxX + 1, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a);
//        }
//
//        try
//        {
//            /*
//            meshData = buffer.end();
//            BufferRenderer.drawWithGlobalProgram(meshData);
//            meshData.close();
//             */
//
//            ctx.drawWithShaders(buffer.end(), ShaderPipelines.DEBUG_LINE_STRIP);
//            ctx.close();
//        }
//        catch (Exception ignored) { }
//    }

    /**
     * Assumes a BufferBuilder in the GL_LINES mode has been initialized
     */
    public static void drawDebugBlockModelOutlinesBatched(List<BlockModelPart> modelParts, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final BlockModelPart part : modelParts)
        {
            drawDebugBlockModelOutlinesBatched(part, state, pos, color, expand, buffer);
        }
    }

    public static void drawDebugBlockModelOutlinesBatched(BlockModelPart modelPart, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            renderDebugModelQuadOutlines(modelPart, state, pos, side, color, expand, buffer);
        }

        renderDebugModelQuadOutlines(modelPart, state, pos, null, color, expand, buffer);
    }

    public static void renderDebugModelQuadOutlines(BlockModelPart modelPart, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer)
    {
        try
        {
            // model.getQuads(state, side, RAND)
            renderDebugModelQuadOutlines(pos, buffer, color, modelPart.getQuads(side));
        }
        catch (Exception ignore) {}
    }

    public static void renderDebugModelQuadOutlines(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            renderDebugQuadOutlinesBatched(pos, buffer, color, quads.get(i).vertexData());
        }
    }

    public static void renderDebugQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData)
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

        buffer.vertex(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
    }

    public static void drawBlockModelOutlinesBatched(List<BlockModelPart> modelParts, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer, MatrixStack matricies)
    {
        for (final BlockModelPart part : modelParts)
        {
            drawlockModelOutlinesBatched(part, state, pos, color, expand, buffer, matricies);
        }
    }

    public static void drawlockModelOutlinesBatched(BlockModelPart modelPart, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer, MatrixStack matricies)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            renderModelQuadOutlines(modelPart, state, pos, side, color, expand, buffer, matricies);
        }

        renderModelQuadOutlines(modelPart, state, pos, null, color, expand, buffer, matricies);
    }

    public static void renderModelQuadOutlines(BlockModelPart modelPart, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer, MatrixStack matricies)
    {
        try
        {
            // model.getQuads(state, side, RAND)
            renderModelQuadOutlines(pos, buffer, color, modelPart.getQuads(side), matricies);
        }
        catch (Exception ignore) {}
    }

    public static void renderModelQuadOutlines(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads, MatrixStack matricies)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            renderQuadOutlinesBatched(pos, buffer, color, quads.get(i).vertexData(), matricies);
        }
    }

    public static void renderQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData, MatrixStack matricies)
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

        MatrixStack.Entry e = matricies.peek();

        buffer.vertex(e, fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
    }

    public static void drawBlockModelQuadOverlayBatched(List<BlockModelPart> modelParts, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final BlockModelPart part : modelParts)
        {
            drawBlockModelQuadOverlayBatched(part, state, pos, color, expand, buffer);
        }
    }

    public static void drawBlockModelQuadOverlayBatched(BlockModelPart modelPart, BlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
//            final int light = WorldRenderer.getLightmapCoordinates(worldIn, state, pos);
            drawBlockModelQuadOverlayBatched(modelPart, state, pos, side, color, expand, buffer);
        }

        drawBlockModelQuadOverlayBatched(modelPart, state, pos, null, color, expand, buffer);
    }

    public static void drawBlockModelQuadOverlayBatched(BlockModelPart modelPart, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, BufferBuilder buffer)
    {
        // modelPart.getQuads(state, side, RAND)

        renderModelQuadOverlayBatched(pos, buffer, color, modelPart.getQuads(side));
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        //final int size = quads.size();

        for (BakedQuad quad : quads)
        {
//            final float b = worldIn.getBrightness(quad.face(), quad.shade());
//            final int[] lo = new int[]{light, light, light, light};
//            final float[] bo = new float[]{b, b, b, b};

            renderModelQuadOverlayBatched(pos, buffer, color, quad);
        }
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, BakedQuad quad)
    {
        final int[] vertexData = quad.vertexData();
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

    public static void drawBlockBoxBatchedQuads(BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            drawBlockBoxSideBatchedQuads(pos, side, color, expand, buffer);
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

    public static void drawBlockBoxEdgeBatchedLines(BlockPos pos, Direction.Axis axis, int cornerIndex, Color4f color, BufferBuilder buffer, MatrixStack mastrices)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        MatrixStack.Entry e = mastrices.peek();

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        buffer.vertex(e, (float) minX, (float) minY, (float) minZ).color(color.r, color.g, color.b, color.a).normal(e,0.0f, 0.0f,0.0f);
        buffer.vertex(e, (float) maxX, (float) maxY, (float) maxZ).color(color.r, color.g, color.b, color.a).normal(e,0.0f, 0.0f,0.0f);
    }

    public static void drawBlockBoxEdgeBatchedDebugLines(BlockPos pos, Direction.Axis axis, int cornerIndex, Color4f color, BufferBuilder buffer)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        buffer.vertex((float) minX, (float) minY, (float) minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex((float) maxX, (float) maxY, (float) maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
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
        InventoryOverlay.Context ctx = InventoryUtils.getTargetInventory(world, pos);

        if (ctx != null && ctx.inv() != null)
        {
            final InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(ctx.type(), ctx.inv().size());

            //Litematica.logger.error("render(): type [{}], inv [{}], be [{}], nbt [{}]", ctx.type().name(), ctx.inv().size(), ctx.be() != null, ctx.nbt() != null ? ctx.nbt().getString("id") : new NbtCompound());

            // Try to draw Locked Slots on Crafter Grid
            if (ctx.type() == InventoryRenderType.CRAFTER)
            {
                Set<Integer> disabledSlots = new HashSet<>();

                if (ctx.nbt() != null && !ctx.nbt().isEmpty())
                {
                    disabledSlots = NbtBlockUtils.getDisabledSlotsFromNbt(ctx.nbt());
                }
                else if (ctx.be() instanceof CrafterBlockEntity cbe)
                {
                    disabledSlots = BlockUtils.getDisabledSlots(cbe);
                }

                return renderInventoryOverlay(align, side, offY, ctx.inv(), ctx.type(), props, disabledSlots, mc, drawContext);
            }
            else
            {
                return renderInventoryOverlay(align, side, offY, ctx.inv(), ctx.type(), props, mc, drawContext);
            }
        }

        return 0;
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
                                             Inventory inv, InventoryRenderType type, InventoryProperties props,
                                             MinecraftClient mc, DrawContext drawContext)
    {
        return renderInventoryOverlay(align, side, offY, inv, type, props, Set.of(), mc, drawContext);
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
                                             Inventory inv, InventoryRenderType type, InventoryProperties props, Set<Integer> disabledSlots,
                                             MinecraftClient mc, DrawContext drawContext)
    {
        int xInv = 0;
        int yInv = 0;
        int compatShift = OverlayRenderer.calculateCompatYShift();

        switch (align)
        {
            case CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = GuiUtils.getScaledWindowHeight() / 2 - props.height - offY;
                break;
            case TOP_CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = offY + compatShift;
                break;
        }

        if      (side == LeftRight.LEFT)  { xInv -= (props.width / 2 + 4); }
        else if (side == LeftRight.RIGHT) { xInv += (props.width / 2 + 4); }

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, props.totalSlots, mc, drawContext);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, inv.size(), disabledSlots, mc, drawContext);

        return props.height + compatShift;
    }

    public static void renderBackgroundMask(int startX, int startY, int width, int height, DrawContext drawContext)
    {
        fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(GuiBase.BG_TEXTURE, startX, startY, 0, 0, width, height, drawContext);
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

    public static void drawBlockBoundingBoxOutlinesBatchedDebugLines(BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        drawBoxAllEdgesBatchedDebugLines(pos, Vec3d.ZERO, color, expand, buffer);
    }

    public static void drawBoxAllEdgesBatchedDebugLines(BlockPos pos, Vec3d cameraPos, Color4f color, double expand, BufferBuilder buffer)
    {
        float minX = (float) (pos.getX() - expand - cameraPos.x);
        float minY = (float) (pos.getY() - expand - cameraPos.y);
        float minZ = (float) (pos.getZ() - expand - cameraPos.z);
        float maxX = (float) (pos.getX() + expand - cameraPos.x + 1);
        float maxY = (float) (pos.getY() + expand - cameraPos.y + 1);
        float maxZ = (float) (pos.getZ() + expand - cameraPos.z + 1);

        drawBoxAllEdgesBatchedDebugLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void drawBoxAllEdgesBatchedDebugLines(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color, BufferBuilder buffer)
    {
        // West side
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        // East side
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        // North side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        // South side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);

        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(0.0f, 0.0f, 0.0f);
    }
}

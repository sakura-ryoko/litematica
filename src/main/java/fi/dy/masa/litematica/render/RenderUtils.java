package fi.dy.masa.litematica.render;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.render.InventoryOverlayType;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PositionUtils;

public class RenderUtils
{
    private static final SingleThreadedRandomSource RAND = new SingleThreadedRandomSource(0);

    public static int getMaxStringRenderLength(List<String> list)
    {
        int length = 0;

        for (String str : list)
        {
            length = Math.max(length, StringUtils.getStringWidth(str));
        }

        return length;
    }

    /**
     * Assumes a BufferBuilder in the GL_LINES mode has been initialized
     */
    public static void drawDebugBlockModelOutlinesBatched(List<BlockModelPart> modelParts, BlockState state, BlockPos pos, Color4f color, double expand, float lineWidth, BufferBuilder buffer)
    {
        for (final BlockModelPart part : modelParts)
        {
            drawDebugBlockModelOutlinesBatched(part, state, pos, color, expand, lineWidth, buffer);
        }
    }

    public static void drawDebugBlockModelOutlinesBatched(BlockModelPart modelPart, BlockState state, BlockPos pos, Color4f color, double expand, float lineWidth, BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            renderDebugModelQuadOutlines(modelPart, state, pos, side, color, expand, lineWidth, buffer);
        }

        renderDebugModelQuadOutlines(modelPart, state, pos, null, color, expand, lineWidth, buffer);
    }

    public static void renderDebugModelQuadOutlines(BlockModelPart modelPart, BlockState state, BlockPos pos, Direction side, Color4f color, double expand, float lineWidth, BufferBuilder buffer)
    {
        try
        {
            renderDebugModelQuadOutlines(pos, buffer, color, lineWidth, modelPart.getQuads(side));
        }
        catch (Exception ignore) {}
    }

    public static void renderDebugModelQuadOutlines(BlockPos pos, BufferBuilder buffer,
                                                    Color4f color, float lineWidth,
                                                    List<BakedQuad> quads)
    {
	    for (BakedQuad quad : quads)
	    {
		    renderDebugQuadOutlinesBatched(pos, buffer, color, lineWidth, quad);
	    }
    }

    public static void renderDebugQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer,
                                                      Color4f color, float lineWidth,
                                                      BakedQuad quad)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
//        final int vertexSize = vertexData.length / 4;
        final float[] fx = new float[4];
        final float[] fy = new float[4];
        final float[] fz = new float[4];

        for (int index = 0; index < 4; index++)
        {
//            fx[index] = x + Float.intBitsToFloat(vertexData[index * vertexSize    ]);
//            fy[index] = y + Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
//            fz[index] = z + Float.intBitsToFloat(vertexData[index * vertexSize + 2]);

	        Vector3fc v3fc = quad.position(index);

	        fx[index] = x + v3fc.x();
	        fy[index] = y + v3fc.y();
	        fz[index] = z + v3fc.z();
        }

        buffer.addVertex(fx[0], fy[0], fz[0]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(fx[1], fy[1], fz[1]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(fx[1], fy[1], fz[1]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(fx[2], fy[2], fz[2]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(fx[2], fy[2], fz[2]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(fx[3], fy[3], fz[3]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(fx[3], fy[3], fz[3]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(fx[0], fy[0], fz[0]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    public static void drawBlockModelOutlinesBatched(List<BlockModelPart> modelParts, BlockState state, BlockPos pos,
                                                     Color4f color, double expand, float lineWidth,
                                                     BufferBuilder buffer, PoseStack matrices)
    {
        for (final BlockModelPart part : modelParts)
        {
            drawBlockModelOutlinesBatched(part, state, pos, color, expand, lineWidth, buffer, matrices);
        }
    }

    public static void drawBlockModelOutlinesBatched(BlockModelPart modelPart, BlockState state, BlockPos pos,
                                                     Color4f color, double expand, float lineWidth,
                                                     BufferBuilder buffer, PoseStack matrices)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            renderModelQuadOutlines(modelPart, state, pos, side, color, expand, lineWidth, buffer, matrices);
        }

        renderModelQuadOutlines(modelPart, state, pos, null, color, expand, lineWidth, buffer, matrices);
    }

    public static void renderModelQuadOutlines(BlockModelPart modelPart, BlockState state,
                                               BlockPos pos, Direction side,
                                               Color4f color, double expand, float lineWidth,
                                               BufferBuilder buffer,
                                               PoseStack matrices)
    {
        try
        {
            renderModelQuadOutlines(pos, buffer, color, lineWidth, modelPart.getQuads(side), matrices);
        }
        catch (Exception ignore) {}
    }

    public static void renderModelQuadOutlines(BlockPos pos, BufferBuilder buffer,
                                               Color4f color, float lineWidth,
                                               List<BakedQuad> quads,
                                               PoseStack matrices)
    {
	    for (BakedQuad quad : quads)
	    {
		    renderQuadOutlinesBatched(pos, buffer, color, lineWidth, quad, matrices);
	    }
    }

    public static void renderQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer,
                                                 Color4f color, float lineWidth,
                                                 BakedQuad quad,
                                                 PoseStack matrices)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
//        final int vertexSize = vertexData.length / 4;
        final float[] fx = new float[4];
        final float[] fy = new float[4];
        final float[] fz = new float[4];

        for (int index = 0; index < 4; index++)
        {
			Vector3fc v3fc = quad.position(index);
//            fx[index] = x + Float.intBitsToFloat(vertexData[index * vertexSize    ]);
//            fy[index] = y + Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
//            fz[index] = z + Float.intBitsToFloat(vertexData[index * vertexSize + 2]);

            fx[index] = x + v3fc.x();
            fy[index] = y + v3fc.y();
            fz[index] = z + v3fc.z();
        }

        PoseStack.Pose e = matrices.last();

        buffer.addVertex(e, fx[0], fy[0], fz[0]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(e, fx[1], fy[1], fz[1]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(e, fx[1], fy[1], fz[1]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(e, fx[2], fy[2], fz[2]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(e, fx[2], fy[2], fz[2]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(e, fx[3], fy[3], fz[3]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(e, fx[3], fy[3], fz[3]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(e, fx[0], fy[0], fz[0]).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    public static boolean stateModelHasQuads(BlockState state)
    {
        return modelHasQuads(Objects.requireNonNull(Minecraft.getInstance().getBlockRenderer().getBlockModel(state)));
    }

    public static boolean modelHasQuads(@Nonnull BlockStateModel model)
    {
        return hasQuads(model.collectParts(RAND));
    }

    public static boolean hasQuads(List<BlockModelPart> modelParts)
    {
        if (modelParts.isEmpty()) return false;
        int totalSize = 0;

        for (BlockModelPart part : modelParts)
        {
            for (Direction face : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
            {
                totalSize += part.getQuads(face).size();
            }

            totalSize += part.getQuads(null).size();
        }

        return totalSize > 0;
    }

    public static void drawBlockModelQuadOverlayBatched(List<BlockModelPart> modelParts,
                                                        BlockState state, BlockPos pos,
                                                        Color4f color, double expand,
                                                        BufferBuilder buffer)
    {
//        System.out.printf("drawBlockModelQuadOverlayBatched - pos [%s], parts [%d], state [%s]\n", pos.toShortString(), modelParts.size(), state.toString());

        for (final BlockModelPart part : modelParts)
        {
            drawBlockModelQuadOverlayBatched(part, state, pos, color, expand, buffer);
        }
    }

    public static void drawBlockModelQuadOverlayBatched(BlockModelPart modelPart,
                                                        BlockState state, BlockPos pos,
                                                        Color4f color, double expand,
                                                        BufferBuilder buffer)
    {
        for (final Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            drawBlockModelQuadOverlayBatched(modelPart, state, pos, side, color, expand, buffer);
        }

        drawBlockModelQuadOverlayBatched(modelPart, state, pos, null, color, expand, buffer);
    }

    public static void drawBlockModelQuadOverlayBatched(BlockModelPart modelPart,
                                                        BlockState state, BlockPos pos,
                                                        Direction side,
                                                        Color4f color, double expand,
                                                        BufferBuilder buffer)
    {
//        System.out.printf("drawBlockModelQuadOverlayBatched - pos [%s], side [%s], state [%s]\n", pos.toShortString(), side != null ? side.asString() : "<null>", state.toString());
        renderModelQuadOverlayBatched(pos, buffer, color, modelPart.getQuads(side));
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        //final int size = quads.size();
//        System.out.printf("renderModelQuadOverlayBatched - pos [%s], quads [%d]\n", pos.toShortString(), quads.size());

        for (final BakedQuad quad : quads)
        {
            renderModelQuadOverlayBatched(pos, buffer, color, quad);
        }
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, BakedQuad quad)
    {
//        final int[] vertexData = quad.vertexData();
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
//        final int vertexSize = vertexData.length / 4;
        float fx, fy, fz;

        for (int index = 0; index < 4; index++)
        {
//            fx = x + Float.intBitsToFloat(vertexData[index * vertexSize    ]);
//            fy = y + Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
//            fz = z + Float.intBitsToFloat(vertexData[index * vertexSize + 2]);

	        Vector3fc v3fc = quad.position(index);

            fx = x + v3fc.x();
            fy = y + v3fc.y();
            fz = z + v3fc.z();

            buffer.addVertex(fx, fy, fz).setColor(color.r, color.g, color.b, color.a);
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
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;
        }
    }

    public static void drawBlockBoxEdgeBatchedLines(BlockPos pos, Direction.Axis axis, int cornerIndex, Color4f color, float lineWidth, BufferBuilder buffer)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        buffer.addVertex((float) minX, (float) minY, (float) minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex((float) maxX, (float) maxY, (float) maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
    }

    public static void drawBlockBoxEdgeBatchedDebugLines(BlockPos pos, Direction.Axis axis, int cornerIndex, Color4f color, float lineWidth, BufferBuilder buffer)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        buffer.addVertex((float) minX, (float) minY, (float) minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex((float) maxX, (float) maxY, (float) maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    public static int renderInventoryOverlays(GuiContext ctx, BlockInfoAlignment align, int offY, Level worldSchematic, Level worldClient, BlockPos pos)
    {
        int heightSch = renderInventoryOverlay(ctx, align, LeftRight.LEFT, offY, worldSchematic, pos);
        int heightCli = renderInventoryOverlay(ctx, align, LeftRight.RIGHT, offY, worldClient, pos);

        return Math.max(heightSch, heightCli);
    }

    public static int renderInventoryOverlay(GuiContext guiCtx, BlockInfoAlignment align, LeftRight side, int offY, Level world, BlockPos pos)
    {
        InventoryOverlayContext ctx = InventoryUtils.getTargetInventory(world, pos);

        if (ctx != null && ctx.inv() != null)
        {
            final InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(ctx.type(), ctx.inv().getContainerSize());

//            Litematica.LOGGER.error("render(): type [{}], inv [{}], be [{}], nbt [{}]", ctx.type().name(), ctx.inv().size(), ctx.be() != null, ctx.nbt() != null ? ctx.nbt().getString("id") : new NbtCompound());

            // Try to draw Locked Slots on Crafter Grid
            if (ctx.type() == InventoryOverlayType.CRAFTER)
            {
                Set<Integer> disabledSlots = new HashSet<>();

                if (ctx.data() != null && !ctx.data().isEmpty())
                {
                    disabledSlots = DataBlockUtils.getDisabledSlots(ctx.data());
                }
                else if (ctx.be() instanceof CrafterBlockEntity cbe)
                {
                    disabledSlots = BlockUtils.getDisabledSlots(cbe);
                }

                return renderInventoryOverlay(guiCtx, align, side, offY, ctx.inv(), ctx.type(), props, disabledSlots);
            }
            else
            {
                return renderInventoryOverlay(guiCtx, align, side, offY, ctx.inv(), ctx.type(), props);
            }
        }

        return 0;
    }

    public static int renderInventoryOverlay(GuiContext ctx, BlockInfoAlignment align, LeftRight side, int offY,
                                             Container inv, InventoryOverlayType type, InventoryProperties props)
    {
        return renderInventoryOverlay(ctx, align, side, offY, inv, type, props, Set.of());
    }

    public static int renderInventoryOverlay(GuiContext ctx, BlockInfoAlignment align, LeftRight side, int offY,
											 Container inv, InventoryOverlayType type, InventoryProperties props, Set<Integer> disabledSlots)
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

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(ctx, type, xInv, yInv, props.slotsPerRow, props.totalSlots);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(ctx, type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, inv.getContainerSize(), disabledSlots);

        return props.height + compatShift;
    }

    public static void renderBackgroundMask(GuiContext ctx, int startX, int startY, int width, int height)
    {
        fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(ctx, GuiBase.BG_TEXTURE, startX, startY, 0, 0, width, height);
    }

    public static void drawBlockBoundingBoxOutlinesBatchedDebugLines(BlockPos pos, Color4f color, double expand, float lineWidth, BufferBuilder buffer)
    {
        drawBoxAllEdgesBatchedDebugLines(pos, Vec3.ZERO, color, expand, lineWidth, buffer);
    }

    public static void drawBoxAllEdgesBatchedDebugLines(BlockPos pos, Vec3 cameraPos, Color4f color, double expand, float lineWidth, BufferBuilder buffer)
    {
        float minX = (float) (pos.getX() - expand - cameraPos.x);
        float minY = (float) (pos.getY() - expand - cameraPos.y);
        float minZ = (float) (pos.getZ() - expand - cameraPos.z);
        float maxX = (float) (pos.getX() + expand - cameraPos.x + 1);
        float maxY = (float) (pos.getY() + expand - cameraPos.y + 1);
        float maxZ = (float) (pos.getZ() + expand - cameraPos.z + 1);

        drawBoxAllEdgesBatchedDebugLines(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, buffer);
    }

    public static void drawBoxAllEdgesBatchedDebugLines(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color, float lineWidth, BufferBuilder buffer)
    {
        // West side
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        // East side
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        // North side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        // South side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }
}

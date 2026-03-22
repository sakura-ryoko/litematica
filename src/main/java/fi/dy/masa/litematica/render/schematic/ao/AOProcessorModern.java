package fi.dy.masa.litematica.render.schematic.ao;

import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A "Modern" AO Processor based upon 24w36a through 1.21.11+ that utilizes the "brightness cache"
 */
public class AOProcessorModern extends AOProcessor
{
	public final float[] shape = new float[AOSizeModern.values().length];

	@Override
	public void prepareSmooth(BlockAndTintGetter world, BlockState state, BlockPos center, BakedQuad quad, QuadInstance instance)
	{
		this.prepareShape(world, state, center, quad, true);
		Direction face = quad.direction();
		BlockPos basePos = this.cubic ? center.relative(face) : center;
		AONeighborInfoModern info = AONeighborInfoModern.getNeighbourInfo(face);
		BlockPos.MutableBlockPos pos = this.scratchPos;
		pos.setWithOffset(basePos, info.corners[0]);
		BlockState state0 = world.getBlockState(pos);
		int light0 = this.lightmap.brightnessCache.getLight(state0, world, pos);
		float shade0 = this.lightmap.brightnessCache.getShade(state0, world, pos);
		pos.setWithOffset(basePos, info.corners[1]);
		BlockState state1 = world.getBlockState(pos);
		int light1 = this.lightmap.brightnessCache.getLight(state1, world, pos);
		float shade1 = this.lightmap.brightnessCache.getShade(state1, world, pos);
		pos.setWithOffset(basePos, info.corners[2]);
		BlockState state2 = world.getBlockState(pos);
		int light2 = this.lightmap.brightnessCache.getLight(state2, world, pos);
		float shade2 = this.lightmap.brightnessCache.getShade(state2, world, pos);
		pos.setWithOffset(basePos, info.corners[3]);
		BlockState state3 = world.getBlockState(pos);
		int light3 = this.lightmap.brightnessCache.getLight(state3, world, pos);
		float shade3 = this.lightmap.brightnessCache.getShade(state3, world, pos);
		BlockState corner0 = world.getBlockState(pos.setWithOffset(basePos, info.corners[0]).move(face));
		boolean translucent0 = !corner0.isViewBlocking(world, pos) || corner0.getLightDampening() == 0;
		BlockState corner1 = world.getBlockState(pos.setWithOffset(basePos, info.corners[1]).move(face));
		boolean translucent1 = !corner1.isViewBlocking(world, pos) || corner1.getLightDampening() == 0;
		BlockState corner2 = world.getBlockState(pos.setWithOffset(basePos, info.corners[2]).move(face));
		boolean translucent2 = !corner2.isViewBlocking(world, pos) || corner2.getLightDampening() == 0;
		BlockState corner3 = world.getBlockState(pos.setWithOffset(basePos, info.corners[3]).move(face));
		boolean translucent3 = !corner3.isViewBlocking(world, pos) || corner3.getLightDampening() == 0;
		float shadeCorner02;
		int lightCorner02;

		if (!translucent2 && !translucent0)
		{
			shadeCorner02 = shade0;
			lightCorner02 = light0;
		}
		else
		{
			pos.setWithOffset(basePos, info.corners[0]).move(info.corners[2]);
			BlockState state02 = world.getBlockState(pos);
			shadeCorner02 = this.lightmap.brightnessCache.getShade(state02, world, pos);
			lightCorner02 = this.lightmap.brightnessCache.getLight(state02, world, pos);
		}

		float shadeCorner03;
		int lightCorner03;

		if (!translucent3 && !translucent0)
		{
			shadeCorner03 = shade0;
			lightCorner03 = light0;
		}
		else
		{
			pos.setWithOffset(basePos, info.corners[0]).move(info.corners[3]);
			BlockState state03 = world.getBlockState(pos);
			shadeCorner03 = this.lightmap.brightnessCache.getShade(state03, world, pos);
			lightCorner03 = this.lightmap.brightnessCache.getLight(state03, world, pos);
		}

		float shadeCorner12;
		int lightCorner12;

		if (!translucent2 && !translucent1)
		{
			shadeCorner12 = shade0;
			lightCorner12 = light0;
		}
		else
		{
			pos.setWithOffset(basePos, info.corners[1]).move(info.corners[2]);
			BlockState state12 = world.getBlockState(pos);
			shadeCorner12 = this.lightmap.brightnessCache.getShade(state12, world, pos);
			lightCorner12 = this.lightmap.brightnessCache.getLight(state12, world, pos);
		}

		float shadeCorner13;
		int lightCorner13;

		if (!translucent3 && !translucent1)
		{
			shadeCorner13 = shade0;
			lightCorner13 = light0;
		}
		else
		{
			pos.setWithOffset(basePos, info.corners[1]).move(info.corners[3]);
			BlockState state13 = world.getBlockState(pos);
			shadeCorner13 = this.lightmap.brightnessCache.getShade(state13, world, pos);
			lightCorner13 = this.lightmap.brightnessCache.getLight(state13, world, pos);
		}

		int lightCenter = this.lightmap.brightnessCache.getLight(state, world, center);
		pos.setWithOffset(center, face);
		BlockState nextState = world.getBlockState(pos);

		if (this.cubic || !nextState.isSolidRender())
		{
			lightCenter = this.lightmap.brightnessCache.getLight(nextState, world, pos);
		}

		float shadeCenter = this.cubic
		                    ? this.lightmap.brightnessCache.getShade(world.getBlockState(basePos), world, basePos)
		                    : this.lightmap.brightnessCache.getShade(world.getBlockState(center), world, center);
		AOVertexMap remap = AOVertexMap.getVertexTranslations(face);

		if (this.hasNeighbors && info.doNonCubicWeight)
		{
			float tempShade1 = (shade3 + shade0 + shadeCorner03 + shadeCenter) * 0.25F;
			float tempShade2 = (shade2 + shade0 + shadeCorner02 + shadeCenter) * 0.25F;
			float tempShade3 = (shade2 + shade1 + shadeCorner12 + shadeCenter) * 0.25F;
			float tempShade4 = (shade3 + shade1 + shadeCorner13 + shadeCenter) * 0.25F;
			float vert0weight01 = this.shape[info.vert0Weights[0].index] * this.shape[info.vert0Weights[1].index];
			float vert0weight23 = this.shape[info.vert0Weights[2].index] * this.shape[info.vert0Weights[3].index];
			float vert0weight45 = this.shape[info.vert0Weights[4].index] * this.shape[info.vert0Weights[5].index];
			float vert0weight67 = this.shape[info.vert0Weights[6].index] * this.shape[info.vert0Weights[7].index];
			float vert1weight01 = this.shape[info.vert1Weights[0].index] * this.shape[info.vert1Weights[1].index];
			float vert1weight23 = this.shape[info.vert1Weights[2].index] * this.shape[info.vert1Weights[3].index];
			float vert1weight45 = this.shape[info.vert1Weights[4].index] * this.shape[info.vert1Weights[5].index];
			float vert1weight67 = this.shape[info.vert1Weights[6].index] * this.shape[info.vert1Weights[7].index];
			float vert2weight01 = this.shape[info.vert2Weights[0].index] * this.shape[info.vert2Weights[1].index];
			float vert2weight23 = this.shape[info.vert2Weights[2].index] * this.shape[info.vert2Weights[3].index];
			float vert2weight45 = this.shape[info.vert2Weights[4].index] * this.shape[info.vert2Weights[5].index];
			float vert2weight67 = this.shape[info.vert2Weights[6].index] * this.shape[info.vert2Weights[7].index];
			float vert3weight01 = this.shape[info.vert3Weights[0].index] * this.shape[info.vert3Weights[1].index];
			float vert3weight23 = this.shape[info.vert3Weights[2].index] * this.shape[info.vert3Weights[3].index];
			float vert3weight45 = this.shape[info.vert3Weights[4].index] * this.shape[info.vert3Weights[5].index];
			float vert3weight67 = this.shape[info.vert3Weights[6].index] * this.shape[info.vert3Weights[7].index];
			instance.setColor(
					remap.vert0,
					ARGB.gray(Math.clamp(tempShade1 * vert0weight01 + tempShade2 * vert0weight23 + tempShade3 * vert0weight45 + tempShade4 * vert0weight67, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert1,
					ARGB.gray(Math.clamp(tempShade1 * vert1weight01 + tempShade2 * vert1weight23 + tempShade3 * vert1weight45 + tempShade4 * vert1weight67, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert2,
					ARGB.gray(Math.clamp(tempShade1 * vert2weight01 + tempShade2 * vert2weight23 + tempShade3 * vert2weight45 + tempShade4 * vert2weight67, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert3,
					ARGB.gray(Math.clamp(tempShade1 * vert3weight01 + tempShade2 * vert3weight23 + tempShade3 * vert3weight45 + tempShade4 * vert3weight67, 0.0F, 1.0F))
			);

			int _tc1 = LightCoordsUtil.smoothBlend(light3, light0, lightCorner03, lightCenter);
			int _tc2 = LightCoordsUtil.smoothBlend(light2, light0, lightCorner02, lightCenter);
			int _tc3 = LightCoordsUtil.smoothBlend(light2, light1, lightCorner12, lightCenter);
			int _tc4 = LightCoordsUtil.smoothBlend(light3, light1, lightCorner13, lightCenter);
			instance.setLightCoords(
					remap.vert0, LightCoordsUtil.smoothWeightedBlend(_tc1, _tc2, _tc3, _tc4, vert0weight01, vert0weight23, vert0weight45, vert0weight67)
			);
			instance.setLightCoords(
					remap.vert1, LightCoordsUtil.smoothWeightedBlend(_tc1, _tc2, _tc3, _tc4, vert1weight01, vert1weight23, vert1weight45, vert1weight67)
			);
			instance.setLightCoords(
					remap.vert2, LightCoordsUtil.smoothWeightedBlend(_tc1, _tc2, _tc3, _tc4, vert2weight01, vert2weight23, vert2weight45, vert2weight67)
			);
			instance.setLightCoords(
					remap.vert3, LightCoordsUtil.smoothWeightedBlend(_tc1, _tc2, _tc3, _tc4, vert3weight01, vert3weight23, vert3weight45, vert3weight67)
			);
		}
		else
		{
			float lightLevel1 = (shade3 + shade0 + shadeCorner03 + shadeCenter) * 0.25F;
			float lightLevel2 = (shade2 + shade0 + shadeCorner02 + shadeCenter) * 0.25F;
			float lightLevel3 = (shade2 + shade1 + shadeCorner12 + shadeCenter) * 0.25F;
			float lightLevel4 = (shade3 + shade1 + shadeCorner13 + shadeCenter) * 0.25F;
			instance.setLightCoords(remap.vert0, LightCoordsUtil.smoothBlend(light3, light0, lightCorner03, lightCenter));
			instance.setLightCoords(remap.vert1, LightCoordsUtil.smoothBlend(light2, light0, lightCorner02, lightCenter));
			instance.setLightCoords(remap.vert2, LightCoordsUtil.smoothBlend(light2, light1, lightCorner12, lightCenter));
			instance.setLightCoords(remap.vert3, LightCoordsUtil.smoothBlend(light3, light1, lightCorner13, lightCenter));
			instance.setColor(remap.vert0, ARGB.gray(lightLevel1));
			instance.setColor(remap.vert1, ARGB.gray(lightLevel2));
			instance.setColor(remap.vert2, ARGB.gray(lightLevel3));
			instance.setColor(remap.vert3, ARGB.gray(lightLevel4));
		}

		CardinalLighting lighting = world.cardinalLighting();
		instance.scaleColor(quad.materialInfo().shade() ? lighting.byFace(face) : lighting.up());
	}

	@Override
	public void prepareFlat(BlockAndTintGetter world, BlockState state, BlockPos pos, int light, BakedQuad quad, QuadInstance instance)
	{
		if (light == -1)
		{
			this.prepareShape(world, state, pos, quad, false);
			BlockPos lightPos = this.cubic ? this.scratchPos.setWithOffset(pos, quad.direction()) : pos;
			instance.setLightCoords(this.getLight(world, state, lightPos));
		}
		else
		{
			instance.setLightCoords(light);
		}

		CardinalLighting lighting = world.cardinalLighting();
		float directionalBrightness = quad.materialInfo().shade() ? lighting.byFace(quad.direction()) : lighting.up();
		instance.setColor(ARGB.gray(directionalBrightness));
	}

	@Override
	public void prepareShape(BlockAndTintGetter world, BlockState state, BlockPos pos, BakedQuad quad, boolean useAO)
	{
		float minX = 32.0F;
		float minY = 32.0F;
		float minZ = 32.0F;
		float maxX = -32.0F;
		float maxY = -32.0F;
		float maxZ = -32.0F;

		for (int i = 0; i < 4; i++)
		{
			Vector3fc position = quad.position(i);
			float x = position.x();
			float y = position.y();
			float z = position.z();
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		if (useAO)
		{
			this.shape[AOSizeModern.WEST.index] = minX;
			this.shape[AOSizeModern.EAST.index] = maxX;
			this.shape[AOSizeModern.DOWN.index] = minY;
			this.shape[AOSizeModern.UP.index] = maxY;
			this.shape[AOSizeModern.NORTH.index] = minZ;
			this.shape[AOSizeModern.SOUTH.index] = maxZ;
			this.shape[AOSizeModern.FLIP_WEST.index] = 1.0F - minX;
			this.shape[AOSizeModern.FLIP_EAST.index] = 1.0F - maxX;
			this.shape[AOSizeModern.FLIP_DOWN.index] = 1.0F - minY;
			this.shape[AOSizeModern.FLIP_UP.index] = 1.0F - maxY;
			this.shape[AOSizeModern.FLIP_NORTH.index] = 1.0F - minZ;
			this.shape[AOSizeModern.FLIP_SOUTH.index] = 1.0F - maxZ;
		}

		float minEpsilon = 1.0E-4F;
		float maxEpsilon = 0.9999F;

		this.hasNeighbors = switch (quad.direction())
		{
			case DOWN, UP -> minX >= minEpsilon || minZ >= minEpsilon || maxX <= maxEpsilon || maxZ <= maxEpsilon;
			case NORTH, SOUTH -> minX >= minEpsilon || minY >= minEpsilon || maxX <= maxEpsilon || maxY <= maxEpsilon;
			case WEST, EAST -> minY >= minEpsilon || minZ >= minEpsilon || maxY <= maxEpsilon || maxZ <= maxEpsilon;
		};

		this.cubic = switch (quad.direction())
		{
			case DOWN -> minY == maxY && (minY < minEpsilon || state.isCollisionShapeFullBlock(world, pos));
			case UP -> minY == maxY && (maxY > maxEpsilon || state.isCollisionShapeFullBlock(world, pos));
			case NORTH -> minZ == maxZ && (minZ < minEpsilon || state.isCollisionShapeFullBlock(world, pos));
			case SOUTH -> minZ == maxZ && (maxZ > maxEpsilon || state.isCollisionShapeFullBlock(world, pos));
			case WEST -> minX == maxX && (minX < minEpsilon || state.isCollisionShapeFullBlock(world, pos));
			case EAST -> minX == maxX && (maxX > maxEpsilon || state.isCollisionShapeFullBlock(world, pos));
		};
	}
}

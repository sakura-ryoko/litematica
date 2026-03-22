package fi.dy.masa.litematica.render.schematic.ao;

import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AO Legacy AO Processor from Masa (2018)
 * -- Ported/merged to be compatible with the "AO Lightmap" system for 1.21.11+
 */
public class AOProcessorLegacy extends AOProcessor
{
	public final float[] shape = new float[AOSizeLegacy.values().length];     // field_58158

	@Override
	public void prepareSmooth(BlockAndTintGetter world, BlockState state, BlockPos center, BakedQuad quad, QuadInstance instance)
	{
		this.prepareShape(world, state, center, quad, true);
		Direction face = quad.direction();
		BlockPos basePos = this.cubic ? center.relative(face) : center;
		AONeighborInfoLegacy info = AONeighborInfoLegacy.getNeighbourInfo(face);
		AOVertexMap remap = AOVertexMap.getVertexTranslations(face);
		int i, j, k, l, i1, i3, j1, k1, l1;
		i = j = k = l = i1 = i3 = j1 = k1 = l1 = ((15 << 20) | (15 << 4));
		float b1 = 1.0F;    // tempShade1
		float b2 = 1.0F;    // tempShade2
		float b3 = 1.0F;    // tempShade3
		float b4 = 1.0F;    // tempShade4

		if (this.hasNeighbors && info.doNonCubicWeight)
		{
			float f13 = this.shape[info.vert0Weights[0].shape] * this.shape[info.vert0Weights[1].shape]; // vert0weight01
			float f14 = this.shape[info.vert0Weights[2].shape] * this.shape[info.vert0Weights[3].shape]; // vert0weight23
			float f15 = this.shape[info.vert0Weights[4].shape] * this.shape[info.vert0Weights[5].shape]; // vert0weight45
			float f16 = this.shape[info.vert0Weights[6].shape] * this.shape[info.vert0Weights[7].shape]; // vert0weight67
			float f17 = this.shape[info.vert1Weights[0].shape] * this.shape[info.vert1Weights[1].shape]; // vert1weight01
			float f18 = this.shape[info.vert1Weights[2].shape] * this.shape[info.vert1Weights[3].shape]; // vert1weight23
			float f19 = this.shape[info.vert1Weights[4].shape] * this.shape[info.vert1Weights[5].shape]; // vert1weight45
			float f20 = this.shape[info.vert1Weights[6].shape] * this.shape[info.vert1Weights[7].shape]; // vert1weight67
			float f21 = this.shape[info.vert2Weights[0].shape] * this.shape[info.vert2Weights[1].shape]; // vert2weight01
			float f22 = this.shape[info.vert2Weights[2].shape] * this.shape[info.vert2Weights[3].shape]; // vert2weight23
			float f23 = this.shape[info.vert2Weights[4].shape] * this.shape[info.vert2Weights[5].shape]; // vert2weight45
			float f24 = this.shape[info.vert2Weights[6].shape] * this.shape[info.vert2Weights[7].shape]; // vert2weight67
			float f25 = this.shape[info.vert3Weights[0].shape] * this.shape[info.vert3Weights[1].shape]; // vert3weight01
			float f26 = this.shape[info.vert3Weights[2].shape] * this.shape[info.vert3Weights[3].shape]; // vert3weight23
			float f27 = this.shape[info.vert3Weights[4].shape] * this.shape[info.vert3Weights[5].shape]; // vert3weight45
			float f28 = this.shape[info.vert3Weights[6].shape] * this.shape[info.vert3Weights[7].shape]; // vert3weight67
//			this.fs[remap.vert0] = b1 * f13 + b2 * f14 + b3 * f15 + b4 * f16;
//			this.fs[remap.vert1] = b1 * f17 + b2 * f18 + b3 * f19 + b4 * f20;
//			this.fs[remap.vert2] = b1 * f21 + b2 * f22 + b3 * f23 + b4 * f24;
//			this.fs[remap.vert3] = b1 * f25 + b2 * f26 + b3 * f27 + b4 * f28;
			instance.setColor(
					remap.vert0,
					ARGB.gray(Math.clamp(b1 * f13 + b2 * f14 + b3 * f15 + b4 * f16, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert1,
					ARGB.gray(Math.clamp(b1 * f17 + b2 * f18 + b3 * f19 + b4 * f20, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert2,
					ARGB.gray(Math.clamp(b1 * f21 + b2 * f22 + b3 * f23 + b4 * f24, 0.0F, 1.0F))
			);
			instance.setColor(
					remap.vert3,
					ARGB.gray(Math.clamp(b1 * f25 + b2 * f26 + b3 * f27 + b4 * f28, 0.0F, 1.0F))
			);
			int i2 = this.getAoBrightness(l, i, j1, i3);    // _tc1
			int j2 = this.getAoBrightness(k, i, i1, i3);    // _tc2
			int k2 = this.getAoBrightness(k, j, k1, i3);    // _tc3
			int l2 = this.getAoBrightness(l, j, l1, i3);    // _tc4
//			this.is[remap.vert0] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
//			this.is[remap.vert1] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
//			this.is[remap.vert2] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
//			this.is[remap.vert3] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
			instance.setLightCoords(
					remap.vert0, this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16)
			);
			instance.setLightCoords(
					remap.vert1, this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20)
			);
			instance.setLightCoords(
					remap.vert2, this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24)
			);
			instance.setLightCoords(
					remap.vert3, this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28)
			);
		}
		else
		{
//			this.is[remap.vert0] = this.getAoBrightness(l, i, j1, i3);
//			this.is[remap.vert1] = this.getAoBrightness(k, i, i1, i3);
//			this.is[remap.vert2] = this.getAoBrightness(k, j, k1, i3);
//			this.is[remap.vert3] = this.getAoBrightness(l, j, l1, i3);
//			this.fs[remap.vert0] = b1;
//			this.fs[remap.vert1] = b2;
//			this.fs[remap.vert2] = b3;
//			this.fs[remap.vert3] = b4;
			instance.setLightCoords(remap.vert0, this.getAoBrightness(l, i, j1, i3));
			instance.setLightCoords(remap.vert1, this.getAoBrightness(k, i, i1, i3));
			instance.setLightCoords(remap.vert2, this.getAoBrightness(k, j, k1, i3));
			instance.setLightCoords(remap.vert3, this.getAoBrightness(l, j, l1, i3));
			instance.setColor(remap.vert0, ARGB.gray(b1));
			instance.setColor(remap.vert1, ARGB.gray(b2));
			instance.setColor(remap.vert2, ARGB.gray(b3));
			instance.setColor(remap.vert3, ARGB.gray(b4));

		}

//		float b = world.getShade(face, hasShade);
//
//		for (int index = 0; index < this.fs.length; ++index)
//		{
//			this.fs[index] *= b;
//		}
		CardinalLighting lighting = world.cardinalLighting();
		instance.scaleColor(quad.materialInfo().shade() ? lighting.byFace(face) : lighting.up());
	}

	/**
	 * Get ambient occlusion brightness
	 */
	private int getAoBrightness(int br1, int br2, int br3, int br4)
	{
		if (br1 == 0)
		{
			br1 = br4;
		}

		if (br2 == 0)
		{
			br2 = br4;
		}

		if (br3 == 0)
		{
			br3 = br4;
		}

		return br1 + br2 + br3 + br4 >> 2 & 16711935;
	}

	private int getVertexBrightness(int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_, float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_)
	{
		int i = (int) ((float) (p_178203_1_ >> 16 & 255) * p_178203_5_ + (float) (p_178203_2_ >> 16 & 255) * p_178203_6_ + (float) (p_178203_3_ >> 16 & 255) * p_178203_7_ + (float) (p_178203_4_ >> 16 & 255) * p_178203_8_) & 255;
		int j = (int) ((float) (p_178203_1_ & 255) * p_178203_5_ + (float) (p_178203_2_ & 255) * p_178203_6_ + (float) (p_178203_3_ & 255) * p_178203_7_ + (float) (p_178203_4_ & 255) * p_178203_8_) & 255;
		return i << 16 | j;
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
			this.shape[Direction.WEST.get3DDataValue()] = minX;
			this.shape[Direction.EAST.get3DDataValue()] = maxX;
			this.shape[Direction.DOWN.get3DDataValue()] = minY;
			this.shape[Direction.UP.get3DDataValue()] = maxY;
			this.shape[Direction.NORTH.get3DDataValue()] = minZ;
			this.shape[Direction.SOUTH.get3DDataValue()] = maxZ;

			this.shape[Direction.WEST.get3DDataValue() + 6] = 1.0F - minX;
			this.shape[Direction.EAST.get3DDataValue() + 6] = 1.0F - maxX;
			this.shape[Direction.DOWN.get3DDataValue() + 6] = 1.0F - minY;
			this.shape[Direction.UP.get3DDataValue() + 6] = 1.0F - maxY;
			this.shape[Direction.NORTH.get3DDataValue() + 6] = 1.0F - minZ;
			this.shape[Direction.SOUTH.get3DDataValue() + 6] = 1.0F - maxZ;
		}

		float min = 1.0E-4F;
		float max = 0.9999F;

		this.hasNeighbors = switch (quad.direction())
		{
			case DOWN, UP -> minX >= min || minZ >= min || maxX <= max || maxZ <= max;
			case NORTH, SOUTH -> minX >= min || minY >= min || maxX <= max || maxY <= max;
			case WEST, EAST -> minY >= min || minZ >= min || maxY <= max || maxZ <= max;
		};

		this.cubic = switch (quad.direction())
		{
			case DOWN -> minY == maxY && (minY < min || state.isCollisionShapeFullBlock(world, pos));
			case UP -> minY == maxY && (maxY > max || state.isCollisionShapeFullBlock(world, pos));
			case NORTH -> minZ == maxZ && (minZ < min || state.isCollisionShapeFullBlock(world, pos));
			case SOUTH -> minZ == maxZ && (maxZ > max || state.isCollisionShapeFullBlock(world, pos));
			case WEST -> minX == maxX && (minX < min || state.isCollisionShapeFullBlock(world, pos));
			case EAST -> minX == maxX && (maxX > max || state.isCollisionShapeFullBlock(world, pos));
		};
	}
}

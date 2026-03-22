package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@FunctionalInterface
public interface IBlockOutputSchematic
{
	void put(float x, float y, float z, BakedQuad quad, QuadInstance instance);
}

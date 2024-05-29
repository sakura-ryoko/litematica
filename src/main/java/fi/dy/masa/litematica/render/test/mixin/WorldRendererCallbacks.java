package fi.dy.masa.litematica.render.test.mixin;

import org.joml.Matrix4f;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;

public class WorldRendererCallbacks
{
    public void onReload()
    {
    }

    public void onPostSetupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator)
    {
    }

    public void onRenderLayer(RenderLayer layer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix)
    {
    }
}

package fi.dy.masa.litematica.util;

import net.minecraft.world.entity.Entity;

public interface IEntityHitboxDebugRendererInvoker
{
	void litematica$addEntityHitbox(Entity entity, float partialTicks, boolean serverEntity);
}

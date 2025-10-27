package fi.dy.masa.litematica.mixin.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fi.dy.masa.litematica.util.IEntityInvoker;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntityInvoker
{
	@Shadow protected boolean touchingWater;

	public MixinEntity()
	{
		super();
	}

	@Override
	public void litematica$toggleTouchingWater(boolean toggle)
	{
		if (toggle != this.touchingWater)
		{
			this.touchingWater = toggle;
		}
	}
}

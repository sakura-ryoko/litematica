package fi.dy.masa.litematica.mixin.entity;

import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fi.dy.masa.litematica.util.IAvatarInvoker;

@Mixin(ClientMannequin.class)
public abstract class MixinClientMannequin extends Mannequin
		implements IAvatarInvoker
{
	@Shadow protected abstract void updateSkin();

	public MixinClientMannequin(EntityType<Mannequin> entityType, Level level)
	{
		super(entityType, level);
	}

	@Override
	public void litematica$tryUpdateSkin()
	{
		this.updateSkin();
	}
}

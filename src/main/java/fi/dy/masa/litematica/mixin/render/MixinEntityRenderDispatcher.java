package fi.dy.masa.litematica.mixin.render;

import java.util.Map;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.IEntityRendererInvoker;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher implements IEntityRendererInvoker
{
	@Shadow private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers;
	@Shadow private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;
	@Shadow public abstract <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity);

	@Override
	public <E extends Entity> EntityRenderer<? super E, ?> litematica_getEntityRendererNullSafe(E entity)
	{
		PlayerSkin skin = DefaultPlayerSkin.get(entity.getUUID());

		if (entity instanceof ClientMannequin cme)
		{
			cme.tick();
			skin = cme.getSkin() != null ? cme.getSkin() : skin;
			return this.litematica_getMannequinRendererBySkin(skin);
		}
		else if (entity instanceof AbstractClientPlayer acp)
		{
			skin = acp.getSkin() != null ? acp.getSkin() : skin;
			return this.litematica_getPlayerRendererBySkin(skin);
		}

		return this.getRenderer(entity);
	}

	@Override
	public <E extends Entity> EntityRenderState litematica_getRenderStateNullSafe(E entity, float tickProgress)
	{
		EntityRenderer<? super E, ?> renderer = this.litematica_getEntityRendererNullSafe(entity);

		if (renderer == null)
		{
			// Try the Vanilla method too? *shrug*
			renderer = this.getRenderer(entity);
		}

		if (renderer != null)
		{
			try
			{
				return renderer.createRenderState(entity, tickProgress);
			}
			catch (Exception err)
			{
				Litematica.LOGGER.error("litematica_getRenderState: Exception getting Entity Render State; {}", err.getLocalizedMessage());
			}
		}

		return null;
	}

	@Override
	public <E extends Entity> boolean litematica_shouldRender(E entity, Frustum culler, double camX, double camY, double camZ)
	{
		if (!entity.shouldRender(camX, camY, camZ))
		{
			return false;
		}
		else if (!this.effectedByCullingWrapper(entity))
		{
			return true;
		}
		else
		{
			AABB bb = entity.getBoundingBox().inflate(0.5D);

			if (bb.hasNaN() || bb.getSize() == 0.0D)
			{
				bb = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
			}

			if (culler.isVisible(bb))
			{
				return true;
			}
			else
			{
				if (entity instanceof Leashable le)
				{
					Entity lead = le.getLeashHolder();

					if (lead != null)
					{
						AABB leadBb = lead.getBoundingBox();
						return culler.isVisible(leadBb) || culler.isVisible(bb.minmax(leadBb));
					}
				}

				return false;
			}
		}
	}

	@Unique
	private <E extends Entity> boolean effectedByCullingWrapper(E e)
	{
		if (e instanceof EnderDragon || e instanceof EnderDragonPart || e instanceof FishingHook || e instanceof LightningBolt)
		{
			return false;
		}
		else if (e instanceof Display d)
		{
			return d.affectedByCulling();
		}

		return true;
	}

	@Unique
	@SuppressWarnings("unchecked")
	private <E extends Entity> EntityRenderer<? super E, ?> litematica_getPlayerRendererBySkin(PlayerSkin skin)
	{
		if (this.playerRenderers.containsKey(skin.model()))
		{
			return (EntityRenderer<? super E, ?>) this.playerRenderers.get(skin.model());
		}
		else if (this.playerRenderers.containsKey(PlayerModelType.WIDE))
		{
			return (EntityRenderer<? super E, ?>) this.playerRenderers.get(PlayerModelType.WIDE);
		}

		return null;
	}

	@Unique
	@SuppressWarnings("unchecked")
	private <E extends Entity> EntityRenderer<? super E, ?> litematica_getMannequinRendererBySkin(PlayerSkin skin)
	{
		if (this.mannequinRenderers.containsKey(skin.model()))
		{
			return (EntityRenderer<? super E, ?>) this.mannequinRenderers.get(skin.model());
		}
		else if (this.mannequinRenderers.containsKey(PlayerModelType.WIDE))
		{
			return (EntityRenderer<? super E, ?>) this.mannequinRenderers.get(PlayerModelType.WIDE);
		}

		return null;
	}
}

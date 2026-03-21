package fi.dy.masa.litematica.mixin.hud;

import java.util.Collection;
import java.util.List;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fi.dy.masa.litematica.render.LitematicaDebugHud;
import fi.dy.masa.litematica.util.DebugHudMode;

// Original method (Works)
@Mixin(DebugScreenOverlay.class)
public abstract class MixinDebugScreenOverlay
{
	@Shadow @Final private Minecraft minecraft;

	@WrapOperation(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
	               at = @At(value = "INVOKE",
					   target = "Ljava/util/Collection;isEmpty()Z",
					   ordinal = 0))
	private boolean litematica_fixF3WhenAllDisabled(Collection<Identifier> instance, Operation<Boolean> original)
	{
		if (LitematicaDebugHud.INSTANCE.getMode() == DebugHudMode.DEFAULT)
		{
			return false;
		}

		return original.call(instance);
	}

	@ModifyArg(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
			   at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V",
						ordinal = 0),
			   index = 1)
	private List<String> litematica_addDebugLines_Left(List<String> text)
	// Left side
    {
		// Always display only when F3 is open, whenever Default mode is ON.
		if (this.minecraft.debugEntries.isOverlayVisible())
		{
			if (LitematicaDebugHud.INSTANCE.getMode() == DebugHudMode.DEFAULT)
			{
				List<String> list = LitematicaDebugHud.INSTANCE.getDebugLines();

				if (!list.isEmpty())
				{
					int size = text.size();

					if (size > 3)
					{
						size -= 3;
					}
					else
					{
						size = 0;
						// Insert mode, but do not go beyond '0'
					}

					for (String entry : list)
					{
						text.add(size++, entry);
					}
				}
			}
		}

		return text;
	}
}

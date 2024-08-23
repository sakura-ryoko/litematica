package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.render.schematic.BufferBuilderPatch;
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderPatch {
    @Unique
    private float offsetY = 0.0f;

    @ModifyArg(method = "vertex(FFF)Lnet/minecraft/client/render/VertexConsumer;", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;memPutFloat(JF)V", ordinal = 1), index = 1)
    private float modifyOffsetY(float value) {
        return value + offsetY;
    }

    @Override
    public void setOffsetY(float offset) {
        this.offsetY = offset;
    }
}

package fi.dy.masa.litematica.mixin.block;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.datafix.fixes.BlockStateData;
import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;

@Mixin(BlockStateData.class)
public abstract class MixinBlockStateData
{
    @Inject(method = "register", at = @At("HEAD"))
    private static void litematica_onAddEntry(int id, Dynamic<?> tag, Dynamic<?>[] legacy, CallbackInfo ci)
    {
        List<Dynamic<?>> oldDynamics = Arrays.stream(legacy).toList();
		SchematicConversionMaps.addDynamicEntry(id, tag, oldDynamics);
    }
}

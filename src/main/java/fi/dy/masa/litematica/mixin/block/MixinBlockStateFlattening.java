package fi.dy.masa.litematica.mixin.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.datafixer.fix.BlockStateFlattening;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;

@Mixin(BlockStateFlattening.class)
public abstract class MixinBlockStateFlattening
{
    @Inject(method = "putStates", at = @At("HEAD"))
    private static void litematica_onAddEntry(int oldIdAndMeta, Dynamic<?> newStateDynamic, Dynamic<?>[] oldStateDynamics, CallbackInfo ci)
    {
        String fixedNBT = newStateDynamic.asString("");
        List<String> oldStates = new ArrayList<>();

        for (Dynamic<?> entry : oldStateDynamics)
        {
            String result = entry.get("Name").asString("");

            if (!result.isEmpty())
            {
                oldStates.add(result);
            }
        }

        // TODO we should probably implement using the Dynamic<?> interface directly
        SchematicConversionMaps.addEntry(oldIdAndMeta, fixedNBT, oldStates.toArray(new String[0]));
    }
}

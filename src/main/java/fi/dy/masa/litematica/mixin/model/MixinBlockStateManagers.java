package fi.dy.masa.litematica.mixin.model;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.schematic.blocks.FallbackBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateManagers;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(BlockStateManagers.class)
public class MixinBlockStateManagers {

    @Shadow private static Map<Identifier, StateManager<Block, BlockState>> STATIC_MANAGERS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void staticInit(CallbackInfo ci) {
        if (!(STATIC_MANAGERS instanceof HashMap)) {
            STATIC_MANAGERS = new HashMap<>(STATIC_MANAGERS);
        }
        STATIC_MANAGERS.put(Identifier.of(Reference.MOD_ID, "black_glass_fallback"), FallbackBlocks.FAKE_BLACK_GLASS_MANAGER);
    }

}

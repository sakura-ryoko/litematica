package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.network.DataQueryHandler;

@Mixin(DataQueryHandler.class)
public interface IMixinDataQueryHandler
{
    @Accessor("expectedTransactionId")
    int currentTransactionId();
}

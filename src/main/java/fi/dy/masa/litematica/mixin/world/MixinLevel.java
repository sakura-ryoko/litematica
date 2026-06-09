package fi.dy.masa.litematica.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import fi.dy.masa.litematica.util.invoker.IWorldUpdateSuppressor;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public class MixinLevel implements IWorldUpdateSuppressor
{
    @Unique private boolean litematica_preventBlockUpdates;

    @Override
    public boolean litematica_getShouldPreventBlockUpdates()
    {
        return this.litematica_preventBlockUpdates;
    }

    @Override
    public void litematica_setShouldPreventBlockUpdates(boolean preventUpdates)
    {
        this.litematica_preventBlockUpdates = preventUpdates;
    }
}

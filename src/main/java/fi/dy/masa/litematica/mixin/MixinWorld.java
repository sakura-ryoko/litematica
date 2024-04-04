package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.World;
import fi.dy.masa.litematica.util.IWorldUpdateSuppressor;
import org.spongepowered.asm.mixin.Unique;

@Mixin(World.class)
public class MixinWorld implements IWorldUpdateSuppressor
{
    @Unique
    private boolean litematica$preventBlockUpdates;

    @Override
    public boolean litematica$getShouldPreventBlockUpdates()
    {
        return this.litematica$preventBlockUpdates;
    }

    @Override
    public void litematica$setShouldPreventBlockUpdates(boolean preventUpdates)
    {
        this.litematica$preventBlockUpdates = preventUpdates;
    }
}

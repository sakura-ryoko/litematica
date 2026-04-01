package fi.dy.masa.litematica.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;

@Mixin(LevelTicks.class)
public interface IMixinLevelTicks<T>
{
    @Accessor("allContainers")
    Long2ObjectMap<LevelChunkTicks<T>> litematica_getChunkTickSchedulers();
}

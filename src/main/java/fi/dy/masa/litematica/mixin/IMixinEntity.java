package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

@Mixin(Entity.class)
public interface IMixinEntity
{
    @Accessor("world")
    void litematica_setWorld(World world);

    @Invoker("readCustomDataFromNbt")
    void readCustomDataFromNbt(NbtCompound nbt);
}

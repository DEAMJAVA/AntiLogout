package net.deamjava.anti_logout.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(Entity.class)
public interface MixinEntity {
    @Invoker("unsetRemoved")
    void invokeUnsetRemoved();
}

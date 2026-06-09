package net.deamjava.anti_logout.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayer.class)
public interface MixinServerPlayerInvoker {
    @Invoker("updateInvisibilityStatus")
    void invokeUpdateInvisibilityStatus();
}
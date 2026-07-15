package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.manager.CombatTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayer {

    @Inject(method = "attack", at = @At("HEAD"))
    private void antiLogout$onAttack(Entity target, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer self && target instanceof ServerPlayer) {
            CombatTracker.INSTANCE.recordHit(self);
        }
    }
}
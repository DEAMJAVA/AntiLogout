package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.config.ConfigManager;
import net.deamjava.anti_logout.manager.CombatTracker;
import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer implements ILingeringPlayer {

    @Unique
    private boolean antiLogout$lingering = false;

    @Override
    public boolean isAntiLogoutLingering() {
        return antiLogout$lingering;
    }

    @Override
    public void setAntiLogoutLingering(boolean value) {
        this.antiLogout$lingering = value;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void antiLogout$tickHead(CallbackInfo ci) {
        if (antiLogout$lingering) {
            ((MixinEntity)(Object) this).invokeUnsetRemoved();
        }
    }


    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void antiLogout$onHurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer)(Object) this;

        if (antiLogout$lingering) {
            var cfg = ConfigManager.INSTANCE.getConfig();

            int lingerDuration = cfg.getCombatOnly()
                    ? cfg.getLingerDuration()
                    : -1;

            CombatTracker.INSTANCE.resetLinger(self.getUUID(), lingerDuration);
        } else {
            if (source.getEntity() instanceof ServerPlayer) {
                CombatTracker.INSTANCE.recordHit(self);
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void antiLogout$onAttack(Entity target, CallbackInfo ci) {
        if (target instanceof ServerPlayer) {
            ServerPlayer self = (ServerPlayer)(Object) this;
            CombatTracker.INSTANCE.recordHit(self);
        }
    }
}
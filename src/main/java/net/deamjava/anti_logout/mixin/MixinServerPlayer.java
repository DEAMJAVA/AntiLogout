package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.config.ConfigManager;
import net.deamjava.anti_logout.manager.CombatTracker;
import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
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

        ServerPlayer self = (ServerPlayer)(Object) this;
        if (value && ConfigManager.INSTANCE.getConfig().getLingeringHideNameTag()) {
            self.setInvisible(true);
        } else if (!value) {
            ((MixinServerPlayerInvoker)(Object) self).invokeUpdateInvisibilityStatus();
        }
    }



    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void antiLogout$onHurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer)(Object) this;

        if (antiLogout$lingering) {
            var cfg = ConfigManager.INSTANCE.getConfig();

            if (cfg.getLingeringImmuneToMobs()) {
                Entity attacker = source.getEntity();
                boolean isMob = attacker != null && !(attacker instanceof ServerPlayer);
                if (isMob) {
                    cir.setReturnValue(false);
                    return;
                }
            }

            if (cfg.getLingeringImmuneToPlayers()) {
                Entity attacker = source.getEntity();
                if (attacker instanceof ServerPlayer) {
                    cir.setReturnValue(false);
                    return;
                }
            }

            int lingerDuration = cfg.getCombatOnly() ? cfg.getLingerDuration() : -1;
            CombatTracker.INSTANCE.resetLinger(self.getUUID(), lingerDuration);

        } else {
            if (source.getEntity() instanceof ServerPlayer) {
                CombatTracker.INSTANCE.recordHit(self);
            }
        }
    }


    @Inject(method = "tick", at = @At("HEAD"))
    private void antiLogout$tickHead(CallbackInfo ci) {
        if (!antiLogout$lingering) return;

        ((MixinEntity)(Object) this).invokeUnsetRemoved();

        ServerPlayer self = (ServerPlayer)(Object) this;
        var cfg = ConfigManager.INSTANCE.getConfig();

        if (cfg.getLingeringAutoCrouch()) {
            self.setPose(Pose.CROUCHING);
            self.setShiftKeyDown(true);
        }
    }

    @Inject(method = "getKnownMovement", at = @At("HEAD"), cancellable = true)
    private void antiLogout$knownMovementForLingering(CallbackInfoReturnable<Vec3> cir) {
        if (!antiLogout$lingering) return;
        Vec3 delta = ((ServerPlayer)(Object) this).getDeltaMovement();
        if (delta.horizontalDistanceSqr() > 0.0001) {
            cir.setReturnValue(delta);
        }
    }

}
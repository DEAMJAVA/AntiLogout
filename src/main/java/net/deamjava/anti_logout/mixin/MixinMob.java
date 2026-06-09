package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.config.ConfigManager;
import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void antiLogout$preventTargetingLingering(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        if (!ConfigManager.INSTANCE.getConfig().getLingeringImmuneToMobs()) return;
        if (target instanceof ILingeringPlayer lp && lp.isAntiLogoutLingering()) {
            ci.cancel();
        }
    }
}
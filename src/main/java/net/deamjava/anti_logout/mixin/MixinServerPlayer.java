package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


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
}

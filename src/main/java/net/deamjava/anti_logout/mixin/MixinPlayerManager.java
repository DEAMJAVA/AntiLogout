package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerManager {

    @Inject(
            method = "remove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;removePlayerImmediately(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
            ),
            cancellable = true
    )
    private void antiLogout$cancelRemoveFromLevel(ServerPlayer player, CallbackInfo ci) {
        if (((ILingeringPlayer) player).isAntiLogoutLingering()) {
            ci.cancel();
            PlayerList playerList = (PlayerList)(Object) this;
            playerList.getPlayers().remove(player);
            ((PlayerListAccessor)(Object) this).getPlayersByUUID().remove(player.getUUID());
        }
    }
}


package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.AntiLogout;
import net.deamjava.anti_logout.config.ConfigManager;
import net.deamjava.anti_logout.manager.CombatTracker;
import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.deamjava.anti_logout.manager.LingeringPlayerManager;
import net.deamjava.anti_logout.manager.ReconnectTransferContext;
import net.deamjava.anti_logout.manager.WhitelistManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class MixinPlayerManager {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void antiLogout$onRemove(ServerPlayer player, CallbackInfo ci) {
        boolean debugLogs = ConfigManager.INSTANCE.getConfig().getDebugLogs();

        if (((ILingeringPlayer) player).isAntiLogoutLingering()) {
            boolean allowed = ReconnectTransferContext.INSTANCE.isAllowingLingeringRemoval(player.getUUID());
            if (!allowed) {
                if (debugLogs) {
                    AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] remove canceled for lingering player name={} uuid={}",
                            player.getName().getString(), player.getUUID());
                }
                ci.cancel();
                return;
            }
            if (debugLogs) {
                AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] remove allowed for lingering player name={} uuid={} (reconnect window)",
                        player.getName().getString(), player.getUUID());
            }
            return;
        }

        UUID uuid = player.getUUID();

        if (WhitelistManager.INSTANCE.isWhitelisted(uuid)) return;

        boolean combatOnly = ConfigManager.INSTANCE.getConfig().getCombatOnly();
        if (combatOnly) {
            int timeout = ConfigManager.INSTANCE.getConfig().getCombatTimeout();
            if (!CombatTracker.INSTANCE.isInCombat(player, timeout)) {
                return;
            }
        }

        LingeringPlayerManager.INSTANCE.addLingeringPlayer(player);
        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] marked player as lingering name={} uuid={}",
                    player.getName().getString(), player.getUUID());
        }
        ci.cancel();
    }
}
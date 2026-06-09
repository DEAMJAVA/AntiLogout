package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.AntiLogout;
import net.deamjava.anti_logout.config.ConfigManager;
import net.deamjava.anti_logout.manager.LingeringPlayerManager;
import net.deamjava.anti_logout.manager.ReconnectTransferContext;
import net.minecraft.network.Connection;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class MixinPlayerListLogin {

    @Shadow
    private List<ServerPlayer> players;

    @Shadow
    private Map<UUID, ServerPlayer> playersByUUID;

    @Inject(method = "disconnectAllPlayersWithProfile", at = @At("HEAD"), cancellable = true)
    private void antiLogout$disconnectDupesWithoutLingeringStall(UUID playerId, CallbackInfoReturnable<Boolean> cir) {
        boolean debugLogs = ConfigManager.INSTANCE.getConfig().getDebugLogs();
        ReconnectTransferContext.INSTANCE.markReconnectForUuid(playerId);

        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] duplicate-login disconnect requested uuid={}", playerId);
        }

        Set<ServerPlayer> dupes = new LinkedHashSet<>();
        for (ServerPlayer p : this.players) {
            if (p.getUUID().equals(playerId)) dupes.add(p);
        }
        ServerPlayer byUuid = this.playersByUUID.get(playerId);
        if (byUuid != null) dupes.add(byUuid);

        boolean disconnectedAny = false;
        int detachedLingering = 0;
        for (ServerPlayer dupe : dupes) {
            if (LingeringPlayerManager.INSTANCE.isLingering(dupe.getUUID())) {
                detachedLingering++;
                this.players.remove(dupe);
                ServerPlayer mapped = this.playersByUUID.get(dupe.getUUID());
                if (mapped == dupe) {
                    this.playersByUUID.remove(dupe.getUUID());
                }
                if (debugLogs) {
                    AntiLogout.INSTANCE.getLogger().info(
                            "[anti-logout][debug] detached lingering dupe from PlayerList tracking name={} uuid={}",
                            dupe.getName().getString(), dupe.getUUID()
                    );
                }
                continue;
            }

            dupe.connection.disconnect(PlayerList.DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
            disconnectedAny = true;
            if (debugLogs) {
                AntiLogout.INSTANCE.getLogger().info(
                        "[anti-logout][debug] disconnected non-lingering dupe name={} uuid={}",
                        dupe.getName().getString(), dupe.getUUID()
                );
            }
        }

        boolean handledAny = disconnectedAny || detachedLingering > 0;

        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info(
                    "[anti-logout][debug] duplicate-login disconnect result uuid={} disconnectedAny={} detachedLingering={} handledAny={}",
                    playerId, disconnectedAny, detachedLingering, handledAny
            );
        }

        cir.setReturnValue(handledAny);
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void antiLogout$onPlayerReconnect(
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie,
            CallbackInfo ci) {

        boolean debugLogs = ConfigManager.INSTANCE.getConfig().getDebugLogs();
        long startNs = System.nanoTime();
        UUID uuid = player.getUUID();

        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] placeNewPlayer HEAD name={} uuid={}",
                    player.getName().getString(), uuid);
        }

        if (!LingeringPlayerManager.INSTANCE.isLingering(uuid)) return;

        ServerPlayer lingering = LingeringPlayerManager.INSTANCE.getLingeringPlayer(uuid);
        if (lingering == null) return;


        boolean limboManaged = false;
        try {
            Class<?> compat = Class.forName("net.deamjava.anti_logout.compat.AntiLogoutCompat");
            Method method = compat.getMethod("isManagedByLimbo", UUID.class);
            limboManaged = (boolean) method.invoke(null, uuid);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            AntiLogout.INSTANCE.getLogger().warn(
                    "[anti-logout] AntiLogoutCompat check failed: " + e.getMessage());
        }

        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] reconnect start name={} uuid={} limboManaged={}",
                    player.getName().getString(), uuid, limboManaged);
        }

        if (!limboManaged) {
            // Copy state from lingering ghost onto the reconnecting player
            try {
                ProblemReporter.Collector reporter = new ProblemReporter.Collector();

                TagValueOutput output = TagValueOutput.createWithContext(
                        reporter,
                        lingering.registryAccess()
                );
                lingering.saveWithoutId(output);

                ValueInput input = TagValueInput.create(
                        reporter,
                        player.registryAccess(),
                        output.buildResult()
                );
                player.load(input);

            } catch (Exception e) {
                AntiLogout.INSTANCE.getLogger().warn(
                        "Failed to copy lingering state for " + player.getName().getString() + ": " + e.getMessage()
                );
            }

            player.snapTo(
                    lingering.getX(),
                    lingering.getY(),
                    lingering.getZ(),
                    lingering.getYRot(),
                    lingering.getXRot()
            );
        }

        ReconnectTransferContext.INSTANCE.withAllowedLingeringRemoval(() -> {
            try {
                ((PlayerList)(Object) this).remove(lingering);
                if (debugLogs) {
                    AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] removed lingering via PlayerList.remove name={} uuid={}",
                            lingering.getName().getString(), uuid);
                }
            } catch (Exception e) {
                if (debugLogs) {
                    AntiLogout.INSTANCE.getLogger().warn(
                            "[anti-logout][debug] failed removing lingering via PlayerList.remove name=" + lingering.getName().getString()
                                    + " uuid=" + uuid + " err=" + e.getMessage()
                    );
                }
            }
            return null;
        });

        LingeringPlayerManager.INSTANCE.removeLingeringPlayer(uuid);

        long tookMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (debugLogs) {
            AntiLogout.INSTANCE.getLogger().info("[anti-logout][debug] reconnect end name={} uuid={} tookMs={}",
                    player.getName().getString(), uuid, tookMs);
        }
    }
}
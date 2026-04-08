package net.deamjava.anti_logout.mixin;

import net.deamjava.anti_logout.AntiLogout;
import net.deamjava.anti_logout.manager.ILingeringPlayer;
import net.deamjava.anti_logout.manager.LingeringPlayerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class MixinPlayerListLogin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void antiLogout$onPlayerReconnect(
            net.minecraft.network.Connection connection,
            ServerPlayer player,
            net.minecraft.server.network.CommonListenerCookie cookie,
            CallbackInfo ci) {

        UUID uuid = player.getUUID();
        if (LingeringPlayerManager.INSTANCE.isLingering(uuid)) {
            ServerPlayer lingering = LingeringPlayerManager.INSTANCE.getLingeringPlayer(uuid);
            if (lingering != null) {
                try {
                    net.minecraft.util.ProblemReporter.Collector reporter =
                            new net.minecraft.util.ProblemReporter.Collector();

                    net.minecraft.world.level.storage.TagValueOutput output =
                            net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                                    reporter,
                                    lingering.registryAccess()
                            );
                    lingering.saveWithoutId(output);

                    net.minecraft.world.level.storage.ValueInput input =
                            net.minecraft.world.level.storage.TagValueInput.create(
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

                ((ILingeringPlayer) lingering).setAntiLogoutLingering(false);
                ((PlayerList)(Object) this).remove(lingering);
            }
            LingeringPlayerManager.INSTANCE.removeLingeringPlayer(uuid);
        }
    }
}
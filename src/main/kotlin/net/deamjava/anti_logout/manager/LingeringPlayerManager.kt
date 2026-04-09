package net.deamjava.anti_logout.manager

import net.deamjava.anti_logout.AntiLogout.logger
import net.deamjava.anti_logout.config.ConfigManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LingeringPlayerManager {

    private val lingeringPlayers: ConcurrentHashMap<UUID, ServerPlayer> = ConcurrentHashMap()

    fun addLingeringPlayer(player: ServerPlayer) {
        lingeringPlayers[player.uuid] = player
        (player as ILingeringPlayer).setAntiLogoutLingering(true)

        var lingerDuration = -1
        if (ConfigManager.config.combatOnly) {
            lingerDuration = ConfigManager.config.lingerDuration
        }
        CombatTracker.startLinger(player.uuid, lingerDuration)
    }

    fun isLingering(uuid: UUID): Boolean = lingeringPlayers.containsKey(uuid)

    fun getLingeringPlayer(uuid: UUID): ServerPlayer? = lingeringPlayers[uuid]

    fun getLingeringPlayerByName(name: String): ServerPlayer? =
        lingeringPlayers.values.firstOrNull { it.gameProfile.name.equals(name, ignoreCase = true) }

    fun removeLingeringPlayer(uuid: UUID) {
        val player = lingeringPlayers.remove(uuid) ?: return
        (player as ILingeringPlayer).setAntiLogoutLingering(false)
        CombatTracker.clearLinger(uuid)
        CombatTracker.clearCombat(uuid)
    }

    fun forceRemoveFromWorld(player: ServerPlayer) {
        try {
            player.level().removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER)
        } catch (e: Exception) {
            logger.warn("Failed to remove lingering player ${player.name.string}: ${e.message}")
        }
    }

    fun clearAll(server: MinecraftServer) {
        lingeringPlayers.values.forEach { forceRemoveFromWorld(it) }
        lingeringPlayers.clear()
    }

    fun tick(server: MinecraftServer) {
        val toRemove = mutableListOf<UUID>()

        lingeringPlayers.values.forEach { player ->
            try {
                // Check linger expiry
                if (CombatTracker.isLingerExpired(player.uuid)) {
                    toRemove.add(player.uuid)
                    return@forEach
                }
                player.doTick()
            } catch (e: Exception) {
                logger.warn("Failed to tick lingering player ${player.name.string}: ${e.message}")
            }
        }

        toRemove.forEach { uuid ->
            val player = lingeringPlayers[uuid]
            if (player != null) {
                logger.info("Lingering player ${player.name.string} timed out, removing.")
                // Do a full PlayerList removal (allowed for lingering) so both
                // the world entity and PlayerList tracking entries are cleaned up.
                ReconnectTransferContext.withAllowedLingeringRemoval {
                    server.playerList.remove(player)
                }
            }
            removeLingeringPlayer(uuid)
        }
    }

    fun getAllLingering(): Collection<ServerPlayer> = lingeringPlayers.values
}
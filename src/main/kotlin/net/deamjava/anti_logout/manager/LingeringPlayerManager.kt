package net.deamjava.anti_logout.manager

import net.deamjava.anti_logout.AntiLogout.logger
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
    }

    fun isLingering(uuid: UUID): Boolean = lingeringPlayers.containsKey(uuid)

    fun getLingeringPlayer(uuid: UUID): ServerPlayer? = lingeringPlayers[uuid]

    fun removeLingeringPlayer(uuid: UUID) {
        val player = lingeringPlayers.remove(uuid) ?: return
        (player as ILingeringPlayer).setAntiLogoutLingering(false)
    }

    fun clearAll(server: MinecraftServer) {
        lingeringPlayers.values.forEach { player ->
            try {
                player.level().removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER)
            } catch (e: Exception) {
                logger.warn("Failed to remove lingering player ${player.name.string}: ${e.message}")
            }
        }
        lingeringPlayers.clear()
    }

    fun tick(server: MinecraftServer) {
        lingeringPlayers.values.forEach { player ->
            try {
                player.doTick()
            } catch (e: Exception) {
                logger.warn("Failed to tick lingering player ${player.name.string}: ${e.message}")
            }
        }
    }

}
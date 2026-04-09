package net.deamjava.anti_logout.manager

import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CombatTracker {

    private val lastCombatHit: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    private val lingerExpiry: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    fun recordHit(player: ServerPlayer) {
        lastCombatHit[player.uuid] = System.currentTimeMillis()
    }

    fun isInCombat(player: ServerPlayer, combatTimeoutSeconds: Int): Boolean {
        val last = lastCombatHit[player.uuid] ?: return false
        return (System.currentTimeMillis() - last) < combatTimeoutSeconds * 1000L
    }

    fun clearCombat(uuid: UUID) {
        lastCombatHit.remove(uuid)
    }

    fun startLinger(uuid: UUID, lingerDurationSeconds: Int) {
        if (lingerDurationSeconds < 0) {
            lingerExpiry[uuid] = -1L
        } else {
            lingerExpiry[uuid] = System.currentTimeMillis() + lingerDurationSeconds * 1000L
        }
    }

    fun resetLinger(uuid: UUID, lingerDurationSeconds: Int) {
        startLinger(uuid, lingerDurationSeconds)
    }

    fun isLingerExpired(uuid: UUID): Boolean {
        val expiry = lingerExpiry[uuid] ?: return true
        if (expiry < 0) return false // infinite
        return System.currentTimeMillis() > expiry
    }

    fun clearLinger(uuid: UUID) {
        lingerExpiry.remove(uuid)
    }
}
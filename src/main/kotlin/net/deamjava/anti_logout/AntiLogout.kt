package net.deamjava.anti_logout

import net.deamjava.anti_logout.manager.LingeringPlayerManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory

object AntiLogout : ModInitializer {
	val logger = LoggerFactory.getLogger("anti-logout")
	const val MOD_ID = "anti_logout"

	override fun onInitialize() {
		logger.info("Anti-Logout mod initializing...")

		ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
			val player = handler.player
			if (!LingeringPlayerManager.isLingering(player.uuid)) {
				logger.info("Player ${player.name.string} disconnected — keeping entity in world.")
				LingeringPlayerManager.addLingeringPlayer(player)
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			logger.info("Server stopping — clearing all lingering players.")
			LingeringPlayerManager.clearAll(server)
		}
		ServerTickEvents.END_SERVER_TICK.register { server ->
			LingeringPlayerManager.tick(server)
		}

	}
}
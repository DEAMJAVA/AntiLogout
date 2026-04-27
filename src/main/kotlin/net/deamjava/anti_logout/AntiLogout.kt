package net.deamjava.anti_logout

import net.deamjava.anti_logout.command.AloCommand
import net.deamjava.anti_logout.config.ConfigManager
import net.deamjava.anti_logout.manager.LingeringPlayerManager
import net.deamjava.anti_logout.manager.WhitelistManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import org.slf4j.LoggerFactory

object AntiLogout : ModInitializer {

	val logger = LoggerFactory.getLogger("anti-logout")

	override fun onInitialize() {
		ConfigManager.load()
		if (ConfigManager.config.debugLogs) {
			logger.info("AntiLogout debug logging is enabled.")
		}
		WhitelistManager.load()
		CarpetHook.tryInit()

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			AloCommand.register(dispatcher)
		}

		ServerTickEvents.END_SERVER_TICK.register { server ->
			LingeringPlayerManager.tick(server)
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			LingeringPlayerManager.clearAll(server)
		}
	}
}
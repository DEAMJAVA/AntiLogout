package net.deamjava.anti_logout.config

import com.google.gson.GsonBuilder
import net.deamjava.anti_logout.AntiLogout.logger
import java.io.File

data class AntiLogoutConfig(
    val combatOnly: Boolean = false,
    val combatTimeout: Int = 30,
    val lingerDuration: Int = 60,
    val debugLogs: Boolean = false,
    val lingeringImmuneToMobs: Boolean = true,
    val lingeringImmuneToPlayers: Boolean = false,
    val lingeringAutoCrouch: Boolean = false,
    val lingeringHideNameTag: Boolean = false
)

object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File("config/anti-logout/config.json")

    var config: AntiLogoutConfig = AntiLogoutConfig()
        private set

    fun load() {
        try {
            if (!configFile.exists()) {
                configFile.parentFile.mkdirs()
                configFile.writeText(gson.toJson(AntiLogoutConfig()))
                logger.info("Created default anti-logout config.")
                return
            }
            config = gson.fromJson(configFile.readText(), AntiLogoutConfig::class.java)
            logger.info("Loaded anti-logout config: $config")
        } catch (e: Exception) {
            logger.error("Failed to load anti-logout config, using defaults: ${e.message}")
            config = AntiLogoutConfig()
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            logger.error("Failed to save anti-logout config: ${e.message}")
        }
    }
}
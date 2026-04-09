package net.deamjava.anti_logout.manager

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.deamjava.anti_logout.AntiLogout.logger
import java.io.File
import java.util.UUID

object WhitelistManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val whitelistFile = File("config/anti-logout/whitelist.json")

    private val whitelist: MutableMap<UUID, String> = mutableMapOf()

    fun load() {
        try {
            if (!whitelistFile.exists()) {
                whitelistFile.parentFile.mkdirs()
                whitelistFile.writeText("{}")
                return
            }
            val type = object : TypeToken<Map<String, String>>() {}.type
            val raw: Map<String, String> = gson.fromJson(whitelistFile.readText(), type) ?: emptyMap()
            whitelist.clear()
            raw.forEach { (k, v) -> whitelist[UUID.fromString(k)] = v }
            logger.info("Loaded ${whitelist.size} anti-logout whitelist entries.")
        } catch (e: Exception) {
            logger.error("Failed to load whitelist: ${e.message}")
        }
    }

    fun save() {
        try {
            val raw = whitelist.map { (k, v) -> k.toString() to v }.toMap()
            whitelistFile.writeText(gson.toJson(raw))
        } catch (e: Exception) {
            logger.error("Failed to save whitelist: ${e.message}")
        }
    }

    fun add(uuid: UUID, name: String) {
        whitelist[uuid] = name
        save()
    }

    fun remove(uuid: UUID) {
        whitelist.remove(uuid)
        save()
    }

    fun isWhitelisted(uuid: UUID): Boolean = whitelist.containsKey(uuid)

    fun getAll(): Map<UUID, String> = whitelist.toMap()
}
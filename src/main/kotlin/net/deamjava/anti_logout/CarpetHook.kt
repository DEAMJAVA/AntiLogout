package net.deamjava.anti_logout

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.level.ServerPlayer

object CarpetHook {

    private var available = false
    private var fakePlayerClass: Class<*>? = null

    fun tryInit() {
        if (!FabricLoader.getInstance().isModLoaded("carpet")) return
        try {
            fakePlayerClass = Class.forName("carpet.patches.EntityPlayerMPFake")
            available = true
            println("[Anti Logout] Carpet integration enabled.")
        } catch (e: ClassNotFoundException) {
            println("[Anti Logout] Carpet loaded but EntityPlayerMPFake not found: ${e.message}")
        }
    }

    fun isFakePlayer(player: ServerPlayer): Boolean {
        if (!available) return false
        return fakePlayerClass?.isInstance(player) == true
    }


    fun isFakePlayer(username: String): Boolean {
        if (!available) return false
        return false
    }
}
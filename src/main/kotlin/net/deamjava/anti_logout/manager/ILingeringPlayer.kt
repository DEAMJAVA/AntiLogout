package net.deamjava.anti_logout.manager

interface ILingeringPlayer {
    fun isAntiLogoutLingering(): Boolean
    fun setAntiLogoutLingering(value: Boolean)
}
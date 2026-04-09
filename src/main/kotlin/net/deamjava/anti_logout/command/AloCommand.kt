package net.deamjava.anti_logout.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.deamjava.anti_logout.manager.LingeringPlayerManager
import net.deamjava.anti_logout.manager.WhitelistManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel

object AloCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("alo")
                .requires { source ->
                    source.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))
                }
                .then(buildWhitelistNode())
                .then(buildStatusNode())
        )
    }

    private fun buildWhitelistNode() =
        Commands.literal("whitelist")
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                            .suggests { ctx, builder ->
                                SharedSuggestionProvider.suggest(
                                    ctx.source.server.playerList.players.map { it.gameProfile.name() },
                                    builder
                                )
                            }
                            .executes { ctx ->
                                val name = StringArgumentType.getString(ctx, "player")
                                val server = ctx.source.server
                                val target = server.playerList.getPlayer(name)
                                if (target == null) {
                                    ctx.source.sendFailure(Component.literal("§cPlayer '§f$name§c' is not online. Only online players can be whitelisted."))
                                    return@executes 0
                                }
                                val profileName = target.gameProfile.name()
                                WhitelistManager.add(target.getUUID(), profileName)
                                ctx.source.sendSuccess(
                                    { Component.literal("§aAdded §f$profileName §ato the anti-logout whitelist.") }, true
                                )
                                1
                            }
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                            .suggests { _, builder ->
                                SharedSuggestionProvider.suggest(
                                    WhitelistManager.getAll().values,
                                    builder
                                )
                            }
                            .executes { ctx ->
                                val name = StringArgumentType.getString(ctx, "player")
                                val entry = WhitelistManager.getAll().entries
                                    .firstOrNull { it.value.equals(name, ignoreCase = true) }
                                if (entry == null) {
                                    ctx.source.sendFailure(Component.literal("§c'§f$name§c' is not on the anti-logout whitelist."))
                                    return@executes 0
                                }
                                WhitelistManager.remove(entry.key)
                                ctx.source.sendSuccess(
                                    { Component.literal("§aRemoved §f$name §afrom the anti-logout whitelist.") }, true
                                )
                                1
                            }
                    )
            )
            .then(
                Commands.literal("list")
                    .executes { ctx ->
                        val all = WhitelistManager.getAll()
                        if (all.isEmpty()) {
                            ctx.source.sendSuccess({ Component.literal("§7The anti-logout whitelist is empty.") }, false)
                        } else {
                            val names = all.values.joinToString("§7, §f")
                            ctx.source.sendSuccess(
                                { Component.literal("§aWhitelisted players §7(${all.size})§a: §f$names") }, false
                            )
                        }
                        1
                    }
            )

    private fun buildStatusNode() =
        Commands.literal("status")
            .executes { ctx ->
                val lingering = LingeringPlayerManager.getAllLingering()
                if (lingering.isEmpty()) {
                    ctx.source.sendSuccess({ Component.literal("§7No players are currently lingering.") }, false)
                } else {
                    val names = lingering.joinToString("§7, §f") { it.gameProfile.name() }
                    ctx.source.sendSuccess(
                        { Component.literal("§aLingering players §7(${lingering.size})§a: §f$names") }, false
                    )
                }
                1
            }
}
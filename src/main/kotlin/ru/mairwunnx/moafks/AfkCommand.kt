package ru.mairwunnx.moafks

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

/** `/afk [reason]`: goes AFK, or comes back when already AFK. Needs `moafks.afk`, which everyone has. */
class AfkCommand(private val plugin: MoAfksPlugin) : BasicCommand {
  override fun permission() = PERMISSION

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val player = source.executor as? Player ?: source.sender as? Player
      ?: return plugin.messenger.send(source.sender, Message.PLAYERS_ONLY)
    when {
      plugin.afk.isAfk(player) -> plugin.afk.exit(player, quiet = false)
      !plugin.settings.manual.enabled -> plugin.messenger.send(player, Message.MANUAL_DISABLED)
      else -> {
        val reason = args.joinToString(" ").trim().take(plugin.settings.manual.reasonLength.coerceAtLeast(0)).ifBlank { null }
        plugin.afk.enter(player, manual = true, reason = reason)
      }
    }
  }

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> = emptyList()

  companion object {
    const val PERMISSION = "moafks.afk"
  }
}

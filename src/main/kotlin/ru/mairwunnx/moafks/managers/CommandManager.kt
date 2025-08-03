@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.moafks.managers

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import java.io.Closeable

class CommandManager(private val plugin: PluginUnit) : Closeable {

  init {
    plugin.registerCommand("afk", "Toggle AFK", AfkCommand())
    plugin.registerCommand("moafks", "Mo'Afks admin", MoAfksCommand())
  }

  private inner class AfkCommand : BasicCommand {
    override fun permission(): String = "moafks.afk"

    override fun execute(commandSourceStack: CommandSourceStack, args: Array<String>) {
      val sender: CommandSender = commandSourceStack.sender
      val player = sender as? Player ?: return
      val reason = args.joinToString(" ").ifBlank { null }

      if (plugin.afk.isAfk(player)) {
        plugin.afk.exit(player)
      } else {
        val ok = plugin.afk.enterManual(player, reason)
        if (!ok) plugin.logger.debug { "Failed to enter manual AFK for ${player.name}" }
      }
    }

    override fun suggest(commandSourceStack: CommandSourceStack, args: Array<String>) = emptyList<String>()
  }

  private inner class MoAfksCommand : BasicCommand {
    override fun permission(): String? = null

    override fun execute(commandSourceStack: CommandSourceStack, args: Array<String>) {
      val sender = commandSourceStack.sender
      val cfg = plugin.configuration[GeneralConfigurationModel::class.java]

      if (args.isEmpty()) {
        sender.sendMessage(cfg.system.messages.incorrect)
        return
      }

      when (args[0].lowercase()) {
        "reload" -> {
          if (!sender.hasPermission("moafks.reload")) {
            sender.sendMessage(cfg.system.messages.restricted)
            return
          }
          runCatching {
            plugin.reload()
          }.onSuccess {
            sender.sendMessage(cfg.system.messages.reloaded)
          }.onFailure {
            plugin.logger.error({ "Reload failed" }, it)
          }
        }

        else -> sender.sendMessage(cfg.system.messages.incorrect)
      }
    }

    override fun suggest(commandSourceStack: CommandSourceStack, args: Array<String>): Collection<String> {
      return if (args.size == 1 && "reload".startsWith(args[0], ignoreCase = true)) {
        listOf("reload")
      } else emptyList()
    }
  }

  override fun close() {
  }
}
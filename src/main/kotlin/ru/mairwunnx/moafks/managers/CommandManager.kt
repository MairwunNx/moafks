package ru.mairwunnx.moafks.managers

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import java.io.Closeable

class CommandManager(private val plugin: PluginUnit) : Closeable {
  init {
    plugin.getCommand("moafks")?.setExecutor(MoAfksCommandHandler())
    plugin.getCommand("afk")?.setExecutor(AfkCommandHandler())
  }

  inner class MoAfksCommandHandler : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
      val config = plugin.configuration[GeneralConfigurationModel::class]

      if (args.isEmpty()) {
        sender.sendMessage(config.system.messages.incorrect)
        return true
      }

      when (args[0].lowercase()) {
        "reload" -> {
          if (!sender.hasPermission("moafks.reload")) {
            sender.sendMessage(config.system.messages.restricted)
            return true
          }

          plugin.logger.info { "🔄 Reloading Mo'Afks plugin" }

          runCatching {
            plugin.onDisable()
            plugin.onEnable()
          }.onSuccess {
            plugin.logger.info { "✅ Mo'Afks reloaded" }
            sender.sendMessage(config.system.messages.reloaded)
          }.onFailure {
            plugin.logger.error({ "Reload failed" }, it)
          }

          return true
        }
      }
      sender.sendMessage(config.system.messages.incorrect)
      return true
    }
  }

  inner class AfkCommandHandler : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
      val config = plugin.configuration[GeneralConfigurationModel::class]

      if (sender !is Player) return true
      if (!sender.hasPermission("moafks.afk")) {
        sender.sendMessage(config.system.messages.restricted)
        return true
      }
      
      val reason = args.joinToString(" ").ifBlank { null }
      
      if (plugin.afk.isAfk(sender)) {
        plugin.afk.exit(sender)
      } else {
        val success = plugin.afk.enterManual(sender, reason)
        if (!success) {
          plugin.logger.debug { "Failed to enter manual AFK for ${sender.name}" }
        }
      }
      
      return true
    }
  }

  override fun close() {
    plugin.getCommand("moafks")?.setExecutor(null)
    plugin.getCommand("afk")?.setExecutor(null)
  }
}
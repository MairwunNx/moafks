package ru.mairwunnx.moafks

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * `/moafks reload` reloads the config and languages; `/moafks list` shows who is AFK and for how
 * long; `/moafks locale [player]` shows the language a player gets and why. All need `moafks.admin`;
 * 1.x's `moafks.reload` still works.
 */
class MoAfksCommand(private val plugin: MoAfksPlugin) : BasicCommand {
  override fun canUse(sender: CommandSender) = sender.hasPermission(ADMIN) || sender.hasPermission(LEGACY)

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    val messenger = plugin.messenger
    when (args.firstOrNull()?.lowercase()) {
      "reload" -> messenger.send(sender, if (plugin.reload()) Message.RELOADED else Message.RELOAD_FAILED)
      "list" -> list(sender)
      "locale" -> {
        val target = args.getOrNull(1)?.let(Bukkit::getPlayerExact) ?: sender as? Player
          ?: return messenger.send(sender, Message.PLAYER_NOT_FOUND, Placeholder.unparsed("player", args.getOrNull(1) ?: "?"))
        messenger.send(
          sender, Message.LOCALE_INFO,
          Placeholder.unparsed("player", target.name),
          Placeholder.unparsed("language", messenger.languageOf(target)),
          Placeholder.unparsed("country", plugin.sessions.country(target) ?: "?"),
          Placeholder.unparsed("client", target.locale().toString()),
        )
      }
      else -> messenger.send(sender, Message.USAGE)
    }
  }

  private fun list(sender: CommandSender) {
    val messenger = plugin.messenger
    val afk = plugin.afk.afk().sortedBy { it.second.since }
    if (afk.isEmpty()) return messenger.send(sender, Message.LIST_EMPTY)
    val code = messenger.languageOf(sender)
    val entries = afk.mapNotNull { (player, state) ->
      messenger.render(
        code, Message.LIST_ENTRY, *messenger.about(player),
        messenger.time(code, state.seconds()),
        Placeholder.unparsed("reason", state.reason.orEmpty()),
      )
    }
    messenger.send(
      sender, Message.LIST,
      Placeholder.unparsed("count", "${afk.size}"),
      Placeholder.component("players", Component.join(JoinConfiguration.commas(true), entries)),
    )
  }

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> = when (args.size) {
    0, 1 -> listOf("reload", "list", "locale").filter { it.startsWith(args.firstOrNull().orEmpty(), ignoreCase = true) }
    2 if args[0].equals("locale", ignoreCase = true) ->
      Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
    else -> emptyList()
  }

  private companion object {
    const val ADMIN = "moafks.admin"
    const val LEGACY = "moafks.reload"
  }
}

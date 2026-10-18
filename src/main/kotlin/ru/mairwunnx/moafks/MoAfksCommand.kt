package ru.mairwunnx.moafks

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * `/moafks language [code|auto]` lets every player pick the language Mo’Afks speaks to them.
 * `/moafks reload` reloads the config and languages; `/moafks list` shows who is AFK and for how
 * long; `/moafks locale [player]` shows the language a player gets and why. Those three need
 * `moafks.admin`; 1.x's `moafks.reload` still works. Without it, `/moafks` shows the languages.
 */
class MoAfksCommand(private val plugin: MoAfksPlugin) : BasicCommand {
  private fun admin(sender: CommandSender) = sender.hasPermission(ADMIN) || sender.hasPermission(LEGACY)

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    val messenger = plugin.messenger
    val sub = args.firstOrNull()?.lowercase()
    if (sub == "language" || !admin(sender)) {
      val player = source.executor as? Player ?: sender as? Player ?: return messenger.send(sender, Message.PLAYERS_ONLY)
      return language(player, args.getOrNull(1).takeIf { sub == "language" })
    }
    when (sub) {
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

  /** With [typed], sets the language (`auto` hands it back to Mo’Afks); without it, lists the languages. */
  private fun language(player: Player, typed: String?) {
    val messenger = plugin.messenger
    if (typed == null) {
      val list = if (messenger.chosen(player) == null) Message.LANGUAGE_LIST_AUTO else Message.LANGUAGE_LIST_CHOSEN
      val code = messenger.languageOf(player)
      return messenger.send(player, list, Placeholder.unparsed("language", Languages.name(code)), languages(code))
    }
    if (typed.equals("auto", ignoreCase = true)) {
      messenger.choose(player, null)
      return messenger.send(player, Message.LANGUAGE_AUTO, Placeholder.unparsed("language", Languages.name(messenger.languageOf(player))))
    }
    val code = plugin.resolver.match(typed)
      ?: return messenger.send(player, Message.LANGUAGE_UNKNOWN, Placeholder.unparsed("code", typed), languages(messenger.languageOf(player)))
    messenger.choose(player, code)
    messenger.send(player, Message.LANGUAGE_SET, Placeholder.unparsed("language", Languages.name(code)))
  }

  /** `<languages>`: every language by its own name, the [current] one marked; a click picks one. */
  private fun languages(current: String): TagResolver {
    val messenger = plugin.messenger
    val entries = plugin.languages.ordered().mapNotNull { code ->
      messenger.render(
        current, if (code == current) Message.LANGUAGE_ENTRY_CURRENT else Message.LANGUAGE_ENTRY,
        Placeholder.unparsed("name", Languages.name(code)),
        Placeholder.unparsed("code", code),
      )?.clickEvent(ClickEvent.runCommand("/moafks language $code"))
    }
    return Placeholder.component("languages", Component.join(JoinConfiguration.commas(true), entries))
  }

  private fun list(sender: CommandSender) {
    val messenger = plugin.messenger
    val afk = plugin.afk.afk().sortedBy { it.second.since }
    if (afk.isEmpty()) return messenger.send(sender, Message.LIST_EMPTY)
    val code = messenger.languageOf(sender)
    val entries = afk.mapNotNull { (player, state) ->
      messenger.render(
        code, Message.LIST_ENTRY, player,
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

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
    val admin = admin(source.sender)
    return when (args.size) {
      0, 1 -> (if (admin) listOf("language", "reload", "list", "locale") else listOf("language"))
        .filter { it.startsWith(args.firstOrNull().orEmpty(), ignoreCase = true) }
      2 if args[0].equals("language", ignoreCase = true) ->
        (plugin.languages.ordered() + "auto").filter { it.startsWith(args[1], ignoreCase = true) }
      2 if admin && args[0].equals("locale", ignoreCase = true) ->
        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
      else -> emptyList()
    }
  }

  private companion object {
    const val ADMIN = "moafks.admin"
    const val LEGACY = "moafks.reload"
  }
}

package ru.mairwunnx.moafks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Renders messages for each reader in their own language. Placeholders that depend on the language,
 * like `<time>`, come from a function of the language code, so a broadcast builds them per recipient.
 */
class Messenger(private val plugin: MoAfksPlugin) {
  fun languageOf(sender: CommandSender): String =
    (sender as? Player)?.let { plugin.resolver.resolve(plugin.sessions.country(it), it.locale()) } ?: plugin.resolver.default

  fun render(code: String, message: Message, vararg resolvers: TagResolver): Component? =
    plugin.languages.render(code, message, *resolvers)

  fun send(to: CommandSender, message: Message, vararg resolvers: TagResolver) {
    render(languageOf(to), message, *resolvers)?.let(to::sendMessage)
  }

  /** A message about [subject] to [subject]; [extra] fills the language-dependent placeholders. */
  fun tell(subject: Player, message: Message, extra: (String) -> Array<TagResolver> = { emptyArray() }) {
    val code = languageOf(subject)
    render(code, message, *about(subject), *extra(code))?.let(subject::sendMessage)
  }

  /** A message about [subject] to everyone else online and the console, each in their own language. */
  fun announce(subject: Player, message: Message, extra: (String) -> Array<TagResolver> = { emptyArray() }) {
    Bukkit.getOnlinePlayers().forEach { reader ->
      if (reader.uniqueId == subject.uniqueId) return@forEach
      val code = languageOf(reader)
      render(code, message, *about(subject), *extra(code))?.let(reader::sendMessage)
    }
    val code = plugin.resolver.default
    render(code, message, *about(subject), *extra(code))?.let(Bukkit.getConsoleSender()::sendMessage)
  }

  /** `<time>` for a span of seconds, in the reader's language. */
  fun time(code: String, seconds: Long): TagResolver {
    val (message, values) = Rules.duration(seconds)
    val text = render(code, message, *values.map { (k, v) -> Placeholder.unparsed(k, v) }.toTypedArray()) ?: Component.text("${seconds}s")
    return Placeholder.component("time", text)
  }

  fun about(subject: Player): Array<TagResolver> = arrayOf(
    Placeholder.unparsed("player", subject.name),
    Placeholder.component("display_name", subject.displayName()),
    Placeholders.resolver(subject),
  )
}

package ru.mairwunnx.moafks

import net.kyori.adventure.pointer.Pointered
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.Locale

/** The keys a language file can set. A key missing from one language comes from the default language, then from English. */
enum class Message(val key: String) {
  AUTO_ENTER("auto_enter"),
  AUTO_ENTER_BROADCAST("auto_enter_broadcast"),
  MANUAL_ENTER("manual_enter"),
  MANUAL_ENTER_REASON("manual_enter_reason"),
  MANUAL_ENTER_BROADCAST("manual_enter_broadcast"),
  MANUAL_ENTER_BROADCAST_REASON("manual_enter_broadcast_reason"),
  EXIT("exit"),
  EXIT_BROADCAST("exit_broadcast"),
  DECLINED_COMBAT("declined_combat"),
  DECLINED_FALLING("declined_falling"),
  DECLINED_BURNING("declined_burning"),
  DECLINED_LIQUID("declined_liquid"),
  DECLINED_TARGETED("declined_targeted"),
  MANUAL_DISABLED("manual_disabled"),
  LIMIT_WARNING("limit_warning"),
  LIMIT_KICK("limit_kick"),
  MOVED_TO_SAFETY("moved_to_safety"),
  VOID_RESCUE("void_rescue"),
  SECONDS("seconds"),
  MINUTES("minutes"),
  HOURS("hours"),
  LIST("list"),
  LIST_ENTRY("list_entry"),
  LIST_EMPTY("list_empty"),
  RELOADED("reloaded"),
  RELOAD_FAILED("reload_failed"),
  USAGE("usage"),
  LOCALE_INFO("locale_info"),
  PLAYER_NOT_FOUND("player_not_found"),
  PLAYERS_ONLY("players_only"),
}

/**
 * The messages of every language, by code (`en`, `ru`, `pt_br`). Parsing happens once per load;
 * rendering only fills the placeholders.
 */
class Languages(private val texts: Map<String, Map<String, String>>, val default: String) {
  private val mini = MiniMessage.miniMessage()

  val codes: Set<String> get() = texts.keys

  fun has(code: String) = code in texts

  /** The raw text, or null when no language sets it, which means the message is off. */
  fun text(code: String, message: Message): String? =
    texts[code]?.get(message.key) ?: texts[default]?.get(message.key) ?: texts[FALLBACK]?.get(message.key)

  /** An empty text turns a message off, so a server can drop, say, quit messages for one language. */
  fun render(code: String, message: Message, vararg resolvers: TagResolver): Component? = render(code, message, null, *resolvers)

  /** [target] is the player the message is about: per-player tags from other plugins read it. */
  fun render(code: String, message: Message, target: Pointered?, vararg resolvers: TagResolver): Component? =
    text(code, message)?.takeIf { it.isNotBlank() }?.let { if (target == null) mini.deserialize(it, *resolvers) else mini.deserialize(it, target, *resolvers) }

  companion object {
    const val FALLBACK = "en"
    val BUNDLED = listOf("en", "ru", "es", "pt", "de")
  }
}

/** Picks a player's language from their country and their game's language setting. */
class LocaleResolver(private val settings: Settings.Locales, private val available: Set<String>) {
  private val byCountry: Map<String, String> = buildMap {
    settings.countries.forEach { (code, countries) -> countries.forEach { putIfAbsent(it.uppercase(), normalize(code)) } }
  }

  val default: String = normalize(settings.default).takeIf { it in available } ?: Languages.FALLBACK

  fun resolve(country: String?, client: Locale?): String {
    for (source in settings.sources) {
      val code = when (source) {
        Settings.Source.IP -> country?.let { byCountry[it.uppercase()] }
        Settings.Source.CLIENT -> client?.let(::fromClient)
      }
      if (code != null && code in available) return code
    }
    return default
  }

  /** `pt_BR` matches a `pt_br` file first, then `pt`. */
  private fun fromClient(locale: Locale): String? {
    val full = normalize(locale.toString())
    val language = normalize(locale.language)
    return when {
      full in available -> full
      language in available -> language
      else -> null
    }
  }

  companion object {
    fun normalize(code: String) = code.trim().lowercase().replace('-', '_')
  }
}

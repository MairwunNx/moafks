package ru.mairwunnx.moafks

import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.PlaceholderAPIPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * PlaceholderAPI both ways, when it's there. `<papi:name>` in a message turns into `%name%` for the
 * player the message is about: a tag, not raw `%…%` text, so nothing in a message changes unless it
 * asks for a placeholder. And `%moafks_…%` tells TAB, scoreboards, and other plugins who is AFK.
 */
object Placeholders {
  private val legacy = LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build()

  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

  fun resolver(player: Player): TagResolver = TagResolver.resolver("papi") { args, _ ->
    val name = args.popOr("<papi> needs a placeholder name, like <papi:player_health>").value()
    // Only touch PlaceholderAPI's classes when it's there; they aren't on the classpath otherwise.
    Tag.selfClosingInserting(if (available) render(player, name) else Component.empty())
  }

  private fun render(player: Player, name: String): Component =
    legacy.deserialize(PlaceholderAPI.setPlaceholders(player, "%$name%"))

  /** Registers `%moafks_…%`. Call only when [available]. Returns a handle that unregisters it. */
  fun register(plugin: MoAfksPlugin): AutoCloseable {
    val expansion = Expansion(plugin)
    expansion.register()
    return AutoCloseable { expansion.unregister() }
  }

  /**
   * `%moafks_afk%` (PlaceholderAPI's true or false), `%moafks_afk_time%` (seconds in this AFK spell),
   * `%moafks_afk_total%` (seconds AFK across all sessions, so play time can leave it out),
   * `%moafks_reason%`, and `%moafks_tag%` (the mark from `effects.nickname.mark` while AFK, else empty).
   * TAB may ask from its own thread, so everything here reads thread-safe state.
   */
  private class Expansion(private val plugin: MoAfksPlugin) : PlaceholderExpansion() {
    private val mini = MiniMessage.miniMessage()

    override fun getIdentifier() = "moafks"
    override fun getAuthor() = "MairwunNx"
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist() = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
      val id = player?.uniqueId ?: return ""
      val state = plugin.afk.state(id)
      return when (params.lowercase()) {
        "afk" -> if (state != null) PlaceholderAPIPlugin.booleanTrue() else PlaceholderAPIPlugin.booleanFalse()
        "afk_time" -> "${state?.seconds() ?: 0}"
        "afk_total" -> "${plugin.afk.totalSeconds(id)}"
        "reason" -> state?.reason.orEmpty()
        "tag" -> if (state != null) legacy.serialize(mini.deserialize(plugin.settings.effects.nickname.mark)) else ""
        else -> null
      }
    }
  }
}

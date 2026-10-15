package ru.mairwunnx.moafks

import io.github.miniplaceholders.api.Expansion
import io.github.miniplaceholders.api.MiniPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * MiniPlaceholders both ways, when it's there: the Adventure-native cousin of PlaceholderAPI. Other
 * plugins' tags, like `<player_name>`, work in Mo’Afks' messages for the player a message is about;
 * and `<moafks_afk>`, `<moafks_afk_time>`, `<moafks_afk_total>`, `<moafks_reason>`, and `<moafks_tag>`
 * work wherever MiniPlaceholders does. Its classes load only when it's there.
 */
object AdventurePlaceholders {
  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")

  /**
   * Other plugins' global and per-player tags. The per-player ones read the message's target, so a
   * message rendered with its subject as the target gets that subject's values.
   */
  fun resolver(): TagResolver = if (available) MiniPlaceholders.audienceGlobalPlaceholders() else TagResolver.empty()

  /** Registers `<moafks_…>`. Call only when [available]. Returns a handle that unregisters it. */
  fun register(plugin: MoAfksPlugin): AutoCloseable {
    val mini = MiniMessage.miniMessage()
    // TAB and other readers may ask from their own threads: only thread-safe state, and a vanished
    // player reads as not AFK, as with PlaceholderAPI. The reason is a player's own text: plain, never parsed.
    fun text(value: String) = Tag.selfClosingInserting(Component.text(value))
    val expansion = Expansion.builder("moafks")
      .author("MairwunNx")
      .version(plugin.pluginMeta.version)
      .audiencePlaceholder(Player::class.java, "afk") { player, _, _ -> text("${plugin.afk.visibleState(player.uniqueId) != null}") }
      .audiencePlaceholder(Player::class.java, "afk_time") { player, _, _ -> text("${plugin.afk.visibleState(player.uniqueId)?.seconds() ?: 0}") }
      .audiencePlaceholder(Player::class.java, "afk_total") { player, _, _ -> text("${plugin.afk.totalSeconds(player.uniqueId)}") }
      .audiencePlaceholder(Player::class.java, "reason") { player, _, _ -> text(plugin.afk.visibleState(player.uniqueId)?.reason.orEmpty()) }
      .audiencePlaceholder(Player::class.java, "tag") { player, _, _ ->
        val shown = plugin.afk.visibleState(player.uniqueId) != null
        Tag.selfClosingInserting(if (shown) mini.deserialize(plugin.settings.effects.nickname.mark) else Component.empty())
      }
      .build()
    expansion.register()
    return AutoCloseable { expansion.unregister() }
  }
}

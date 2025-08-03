package ru.mairwunnx.moafks.platform

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.entity.Player
import ru.mairwunnx.moafks.platform.PaperAudience.mm

@Suppress("NOTHING_TO_INLINE") inline fun c(s: String): Component = mm.deserialize(s)

fun Component.render(vararg placeholders: Pair<String, Any?>) = render(placeholders.toMap())

fun Component.render(placeholders: Map<String, Any?>): Component {
  var out = this
  for ((key, value) in placeholders) {
    val replacement = when (value) {
      null -> Component.empty()
      is Component -> value
      is Player -> value.name()
      else -> Component.text(value.toString())
    }
    out = out.replaceText(TextReplacementConfig.builder().matchLiteral("<$key>").replacement(replacement).build())
  }
  return out
}
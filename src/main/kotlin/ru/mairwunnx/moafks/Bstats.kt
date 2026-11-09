package ru.mairwunnx.moafks

import org.bstats.bukkit.Metrics
import org.bstats.charts.AdvancedPie
import org.bstats.charts.SingleLineChart
import java.util.concurrent.atomic.AtomicBoolean

/**
 * bStats, when `metrics` is on and [BSTATS_ID] is set. It sends every 30 minutes; each chart drains what
 * [Usage] counted since. Nothing here may break the plugin: a failure logs one warning and the chart sends nothing.
 */
class Bstats(private val plugin: MoAfksPlugin) {
  private var metrics: Metrics? = null
  private val warned = AtomicBoolean()

  /** Starts or stops bStats to match `metrics`; safe to call on every reload. */
  fun apply(enabled: Boolean) {
    if (!enabled || BSTATS_ID == 0) return stop()
    if (metrics != null) return
    metrics = guard("start") {
      Metrics(plugin, BSTATS_ID).apply {
        val usage = plugin.usage
        addCustomChart(AdvancedPie("player_languages") { guard("player_languages") { usage.drainLanguages() } ?: emptyMap() })
        addCustomChart(AdvancedPie("language_source") { guard("language_source") { usage.drainSources() } ?: emptyMap() })
        addCustomChart(SingleLineChart("afk_entries") { guard("afk_entries") { usage.drainEntries() } ?: 0 })
        addCustomChart(AdvancedPie("afk_entry_kind") { guard("afk_entry_kind") { usage.drainKinds() } ?: emptyMap() })
        addCustomChart(SingleLineChart("afk_minutes") {
          guard("afk_minutes") {
            plugin.afk.settle()
            usage.drainMinutes()
          } ?: 0
        })
      }
    }
  }

  fun stop() {
    metrics?.let { guard("stop") { it.shutdown() } }
    metrics = null
  }

  private fun <T> guard(what: String, block: () -> T): T? = try {
    block()
  } catch (error: Throwable) {
    if (warned.compareAndSet(false, true)) plugin.log.warn("bStats failed at $what, its charts may stay empty: $error")
    null
  }

  companion object {
    /** The plugin's id on bstats.org; 0 keeps bStats off. */
    const val BSTATS_ID = 0
  }
}

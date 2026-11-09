package ru.mairwunnx.moafks

import com.mairwunnx.worldapi.expansion.ExpansionApi
import com.mairwunnx.worldapi.expansion.Resource
import com.mairwunnx.worldapi.expansion.WorldApiExpansion
import com.mairwunnx.worldapi.expansion.WorldApiExpansions
import org.bukkit.Bukkit
import java.time.Instant
import java.util.UUID

/**
 * Mo’Afks' data in WorldAPI, when it runs: the `moafks` namespace. [AfksExpansion] speaks WorldAPI's
 * expansion API; this object holds no type of it, so the plugin loads without WorldAPI.
 */
object WorldApi {
  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("WorldAPI")

  /** Registers the expansion. Call only when [available]. */
  fun register(plugin: MoAfksPlugin): AutoCloseable {
    try {
      AfksExpansion.register(plugin)
    } catch (error: LinkageError) {
      // WorldAPI before 3.4.0 has no expansion API.
      throw IllegalStateException("this WorldAPI is too old, it needs 3.4.0 or newer", error)
    }
    return AutoCloseable { runCatching { AfksExpansion.unregister() } }
  }
}

/**
 * The `moafks` namespace: the server's totals, who is AFK now, each player's numbers, and boards in the
 * shape of WorldAPI's own leaderboards. Handlers run on WorldAPI's threads and read only [Afk]'s
 * concurrent state and [AfkStats]; vanished players stay out of everything about now.
 */
class AfksExpansion(private val plugin: MoAfksPlugin) : WorldApiExpansion {
  override fun id() = ID

  override fun requiresApi(): String = ExpansionApi.VERSION

  override fun name() = "Mo’Afks"

  override fun description() = "Mo’Afks: who is AFK, how often players go AFK, and for how long"

  override fun resources(): List<Resource> = listOf(
    Resource.of("stats", "The server's totals: players AFK now, times players went AFK (by /afk and by idling since 2.3.0), and time AFK") {
      val records = plugin.afk.records().values
      mapOf(
        "afkNow" to plugin.afk.visible().size,
        "players" to records.size,
        "entries" to records.sumOf { it.spells },
        "manual" to records.sumOf { it.manual },
        "auto" to records.sumOf { it.auto },
        "seconds" to records.sumOf { it.seconds },
        "minutes" to records.sumOf { it.seconds } / 60,
      )
    }.publicly(),
    Resource.of("afk", "Players AFK now: since when, for how long, how they went AFK, and why") {
      val now = System.currentTimeMillis()
      plugin.afk.visibleStates().sortedBy { it.second.since }.map { (id, state) ->
        mapOf(
          "uuid" to id.toString(), "name" to state.name, "since" to Instant.ofEpochMilli(state.since).toString(),
          "seconds" to state.seconds(now), "kind" to kind(state.manual), "reason" to state.reason,
        )
      }
    }.publicly(),
    Resource.of("players/{player}", "A player's numbers, by UUID or name: AFK now, times AFK (by /afk and by idling since 2.3.0), time AFK, and rank by time") { request ->
      val id = player(request.path("player"))
      if (!plugin.afk.known(id) && plugin.afk.visibleState(id) == null) throw NoSuchElementException("Mo’Afks knows nothing about this player yet")
      val records = plugin.afk.records()
      val record = records[id] ?: AfkRecord()
      val state = plugin.afk.visibleState(id)
      mapOf(
        "uuid" to id.toString(), "name" to (record.name.ifEmpty { null } ?: Bukkit.getOfflinePlayer(id).name),
        "afk" to (state != null), "kind" to state?.let { kind(it.manual) }, "reason" to state?.reason,
        "entries" to record.spells, "manual" to record.manual, "auto" to record.auto,
        "seconds" to record.seconds, "minutes" to record.seconds / 60,
        "rank" to records.values.count { it.seconds > record.seconds } + 1,
      )
    }.publicly(),
    Resource.of("leaderboards/{board}", "Players ranked by time AFK (seconds) or times AFK, shaped as WorldAPI's own leaderboards; ?limit=1..500") { request ->
      val board = request.path("board")
      val (title, unit, score) = when (board) {
        "time" -> Triple("Time AFK", "seconds") { r: AfkRecord -> r.seconds }
        "entries" -> Triple("Times AFK", "times") { r: AfkRecord -> r.spells }
        else -> throw NoSuchElementException("Boards: time, entries")
      }
      val ranked = plugin.afk.records().entries.filter { score(it.value) > 0 }.sortedByDescending { score(it.value) }
      mapOf(
        "id" to board, "title" to title, "unit" to unit, "total" to ranked.size,
        "entries" to ranked.take(request.query("limit", 10, 1, 500)).mapIndexed { i, (id, record) ->
          mapOf("place" to i + 1, "player" to mapOf("uuid" to id.toString(), "name" to record.name), "value" to score(record))
        },
        "at" to Instant.now().toString(),
      )
    }.publicly(),
  )

  private fun kind(manual: Boolean) = if (manual) "manual" else "auto"

  /** A UUID, or the name of a player the server knows. */
  private fun player(raw: String): UUID = runCatching { UUID.fromString(raw) }.getOrNull()
    ?: plugin.stats.all().entries.firstOrNull { it.value.name.equals(raw, ignoreCase = true) }?.key
    ?: Bukkit.getOfflinePlayerIfCached(raw)?.uniqueId
    ?: throw NoSuchElementException("No player named $raw has played here")

  companion object {
    const val ID = "moafks"

    fun register(plugin: MoAfksPlugin) = WorldApiExpansions.register(AfksExpansion(plugin))

    fun unregister() = WorldApiExpansions.unregister(ID)
  }
}

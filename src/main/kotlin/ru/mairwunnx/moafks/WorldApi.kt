package ru.mairwunnx.moafks

import com.mairwunnx.worldapi.expansion.v2.Expansion
import com.mairwunnx.worldapi.expansion.v2.PlayerRef
import org.bukkit.Bukkit
import java.time.Instant
import java.util.UUID

/**
 * Mo’Afks' data in WorldAPI, when it runs: the `moafks` namespace. [AfksExpansion] speaks WorldAPI's expansion API 2,
 * which WorldAPI 4.5.0 brings. This object holds no type of it, so the plugin loads without WorldAPI or with an older one.
 */
object WorldApi {
  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("WorldAPI")

  /** Registers the expansion. Call only when [available]. */
  fun register(plugin: MoAfksPlugin): AutoCloseable {
    try {
      AfksExpansion.plugin = plugin
      AfksExpansion.register()
    } catch (error: LinkageError) {
      // WorldAPI before 4.5.0 has no expansion API 2.
      throw IllegalStateException("this WorldAPI is too old, Mo’Afks needs WorldAPI 4.5.0 or newer", error)
    }
    return AutoCloseable { runCatching { AfksExpansion.unregister() } }
  }
}

/** How a player went AFK: with `/afk`, or by standing idle. */
enum class AfkKind { MANUAL, AUTO }

/** A leaderboard: by time AFK in seconds, or by times AFK. */
enum class AfkBoard(val title: String, val unit: String, val score: (AfkRecord) -> Long) {
  TIME("Time AFK", "seconds", { it.seconds }),
  ENTRIES("Times AFK", "times", { it.spells }),
}

@JvmRecord data class AfkTotals(val afkNow: Int, val players: Int, val entries: Long, val manual: Long, val auto: Long, val seconds: Long, val minutes: Long)

@JvmRecord data class AfkNow(val uuid: UUID, val name: String, val since: Instant, val seconds: Long, val kind: AfkKind, val reason: String?)

@JvmRecord data class AfkPlayer(val uuid: UUID, val name: String?, val afk: Boolean, val kind: AfkKind?, val reason: String?, val entries: Long, val manual: Long, val auto: Long, val seconds: Long, val minutes: Long, val rank: Int)

@JvmRecord data class AfkPlace(val place: Int, val player: PlayerRef, val value: Long)

@JvmRecord data class AfkLeaderboard(val id: AfkBoard, val title: String, val unit: String, val total: Int, val entries: List<AfkPlace>, val at: Instant)

/**
 * The `moafks` namespace: the server's totals, who is AFK now, each player's numbers, and boards in the shape of
 * WorldAPI's own leaderboards. Handlers run on WorldAPI's threads and read only [Afk]'s concurrent state and
 * [AfkStats]. Vanished players stay out of everything about now.
 */
object AfksExpansion : Expansion("moafks", "Mo’Afks", "Mo’Afks: who is AFK, how often players go AFK, and for how long") {
  lateinit var plugin: MoAfksPlugin

  val player = player("player", "A player, by UUID or name")
  val board = choice("board", "What players are ranked by: time (seconds AFK) or entries (times AFK)", AfkBoard::class.java, AfkBoard.TIME)
  val limit = integer("limit", "How many players, from 1 to 500", 1, 500, 10)

  val stats = resource("stats", "The server's totals: players AFK now, times players went AFK (by /afk and by idling since 2.3.0), and time AFK") { totals() }.returns(AfkTotals::class.java).publicly()
  val afk = resource("afk", "Players AFK now: since when, for how long, how they went AFK, and why") { now() }.returnsListOf(AfkNow::class.java).publicly()
  val players = resource("players/{player}", "A player's numbers: AFK now, times AFK (by /afk and by idling since 2.3.0), time AFK, and rank by time") { numbers(it[player]) }.returns(AfkPlayer::class.java).publicly()
  val leaderboards = resource("leaderboards/{board}", "Players ranked by time AFK (seconds) or times AFK, shaped as WorldAPI's own leaderboards") { top(it[board], it[limit]) }.query(limit).returns(AfkLeaderboard::class.java).publicly()

  private fun totals(): AfkTotals {
    val records = plugin.afk.records().values
    val seconds = records.sumOf { it.seconds }
    return AfkTotals(plugin.afk.visible().size, records.size, records.sumOf { it.spells }, records.sumOf { it.manual }, records.sumOf { it.auto }, seconds, seconds / 60)
  }

  private fun now(): List<AfkNow> {
    val now = System.currentTimeMillis()
    return plugin.afk.visibleStates().sortedBy { it.second.since }.map { (id, state) -> AfkNow(id, state.name, Instant.ofEpochMilli(state.since), state.seconds(now), kind(state.manual), state.reason) }
  }

  private fun numbers(who: PlayerRef): AfkPlayer {
    val state = plugin.afk.visibleState(who.uuid)
    if (!plugin.afk.known(who.uuid) && state == null) throw NoSuchElementException("Mo’Afks knows nothing about this player yet")
    val records = plugin.afk.records()
    val record = records[who.uuid] ?: AfkRecord()
    val rank = records.values.count { it.seconds > record.seconds } + 1
    return AfkPlayer(who.uuid, record.name.ifEmpty { null } ?: who.name, state != null, state?.let { kind(it.manual) }, state?.reason, record.spells, record.manual, record.auto, record.seconds, record.seconds / 60, rank)
  }

  private fun top(board: AfkBoard, limit: Int): AfkLeaderboard {
    val ranked = plugin.afk.records().entries.filter { board.score(it.value) > 0 }.sortedByDescending { board.score(it.value) }
    val entries = ranked.take(limit).mapIndexed { i, (id, record) -> AfkPlace(i + 1, PlayerRef(id, record.name), board.score(record)) }
    return AfkLeaderboard(board, board.title, board.unit, ranked.size, entries, Instant.now())
  }

  private fun kind(manual: Boolean) = if (manual) AfkKind.MANUAL else AfkKind.AUTO
}

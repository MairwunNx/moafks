package ru.mairwunnx.moafks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Who is vanished, as vanish plugins (SuperVanish, PremiumVanish, Essentials…) agree to tell: a
 * `vanished` metadata value that reads true. Others must not learn a vanished player is AFK, so they
 * get no broadcast, no mark, no placeholder, no Plan entry; the player's own protection and lines stay.
 * Metadata is read on the server thread; [hidden] serves TAB's and Plan's threads from a set kept here.
 */
class Vanish {
  private val vanished = ConcurrentHashMap.newKeySet<UUID>()

  /** Reads the player's metadata now and remembers the answer. Server thread only. */
  fun check(player: Player): Boolean {
    val now = player.getMetadata(KEY).any { it.asBoolean() }
    if (now) vanished += player.uniqueId else vanished -= player.uniqueId
    return now
  }

  /** The last answer [check] gave; safe from any thread. */
  fun hidden(id: UUID) = id in vanished

  /** Checks everyone online and returns the players whose answer changed. Server thread only. */
  fun refresh(): List<Player> = Bukkit.getOnlinePlayers().filter { player ->
    val before = hidden(player.uniqueId)
    check(player) != before
  }

  fun forget(id: UUID) {
    vanished -= id
  }

  private companion object {
    const val KEY = "vanished"
  }
}

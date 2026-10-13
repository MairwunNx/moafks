package ru.mairwunnx.moafks

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A player's session as Mo’Afks sees it: the country lookup at login (off the server thread), a fresh
 * idle timer at join, and a clean exit from AFK on quit, kick, death, or a trip to another world.
 */
class SessionListener(private val plugin: MoAfksPlugin) : Listener {
  private val countries = ConcurrentHashMap<UUID, String>()

  fun country(player: Player): String? = countries[player.uniqueId]

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
    plugin.geoIp.country(event.address)?.let { countries[event.uniqueId] = it }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onJoin(event: PlayerJoinEvent) = plugin.afk.joined(event.player)

  /** A kick ends in a quit too. */
  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) {
    plugin.afk.left(event.player)
    countries.remove(event.player.uniqueId)
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onDeath(event: PlayerDeathEvent) = plugin.afk.exit(event.player, quiet = true)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onRespawn(event: PlayerRespawnEvent) = plugin.afk.exit(event.player, quiet = true)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onWorldChange(event: PlayerChangedWorldEvent) = plugin.afk.exit(event.player, quiet = true)

  /** A teleport inside the world isn't activity, but the void rescue should bring the player back here. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onTeleport(event: PlayerTeleportEvent) {
    val state = plugin.afk.state(event.player.uniqueId) ?: return
    if (event.to.world == event.from.world) state.anchor = event.to
  }
}

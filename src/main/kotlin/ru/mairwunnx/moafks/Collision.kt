package ru.mairwunnx.moafks

import me.neznamy.tab.api.TabAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

/**
 * Tells clients that an AFK player can't be pushed. `Player#setCollidable` holds only on the server:
 * the AFK player's own client still pushes them away from anyone who walks into them, and the server
 * takes that move. Clients learn the rule only from the player's team.
 */
interface ClientCollision {
  /** Turns pushing off; false when this player's team isn't ours to change. */
  fun off(player: Player): Boolean

  /** Puts back what [off] changed. */
  fun restore(player: Player)
}

object Collision {
  /** TAB's teams when TAB runs them, since TAB overrides every other team; otherwise our own team. */
  fun pick(): ClientCollision = TabCollision.takeIf { it.available } ?: TeamCollision
}

/**
 * A team on the main scoreboard with collision off, there only while someone is AFK: the world save
 * keeps teams, so the last one out removes it, and a start after a crash removes a leftover.
 */
object TeamCollision : ClientCollision {
  const val NAME = "moafks_afk"

  private val scoreboard get() = Bukkit.getScoreboardManager().mainScoreboard

  override fun off(player: Player): Boolean {
    // A player in another team keeps it: moving them would undo another plugin's or a map's setup.
    if (scoreboard.getEntryTeam(player.name) != null) return false
    val team = scoreboard.getTeam(NAME) ?: scoreboard.registerNewTeam(NAME)
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    team.addEntry(player.name)
    return true
  }

  override fun restore(player: Player) {
    val team = scoreboard.getTeam(NAME) ?: return
    team.removeEntry(player.name)
    if (team.entries.isEmpty()) team.unregister()
  }

  fun clear() {
    scoreboard.getTeam(NAME)?.unregister()
  }
}

/** TAB's collision rule for the player. TAB's classes load only when TAB is on. */
object TabCollision : ClientCollision {
  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("TAB") && runCatching { TabAPI.getInstance().nameTagManager != null }.getOrDefault(false)

  override fun off(player: Player): Boolean = runCatching {
    val tab = TabAPI.getInstance()
    val tabPlayer = tab.getPlayer(player.uniqueId) ?: return false
    val nameTags = tab.nameTagManager ?: return false
    // Another plugin already set a rule through TAB; leave it.
    if (nameTags.getCollisionRule(tabPlayer) != null) return false
    nameTags.setCollisionRule(tabPlayer, false)
    true
  }.getOrDefault(false)

  override fun restore(player: Player) {
    runCatching {
      val tab = TabAPI.getInstance()
      val tabPlayer = tab.getPlayer(player.uniqueId) ?: return
      val nameTags = tab.nameTagManager ?: return
      if (nameTags.getCollisionRule(tabPlayer) == false) nameTags.setCollisionRule(tabPlayer, null)
    }
  }
}

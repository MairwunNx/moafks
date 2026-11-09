package ru.mairwunnx.moafks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * One AFK spell. What AFK changed on the player is kept here, so [Afk] puts back exactly that: null
 * means AFK left the value alone.
 */
class AfkState(val name: String, val reason: String?, val manual: Boolean, val since: Long, @Volatile var anchor: Location) {
  var collidable: Boolean? = null
  var clientCollision: ClientCollision? = null
  var sleepingIgnored: Boolean? = null
  var listName: Component? = null
  var marked: Component? = null
  /** The limit warnings already shown. */
  val warned = mutableSetOf<Int>()
  /** Up to when bStats has counted this spell's time, in milliseconds. */
  val counted = AtomicLong(since)

  fun seconds(now: Long = System.currentTimeMillis()) = (now - since) / 1000
}

/**
 * Who is AFK, and everything that goes with it: entering and leaving, the idle timer, the time
 * limit, and putting the player back as they were. State is keyed by UUID; Bukkit calls happen on
 * the server thread. [states] is a concurrent map, and the totals live in [AfkStats], because
 * PlaceholderAPI, Plan, WorldAPI, and bStats read them from their own threads.
 */
class Afk(private val plugin: MoAfksPlugin) {
  private val states = ConcurrentHashMap<UUID, AfkState>()
  private val activity = HashMap<UUID, Activity>()
  private val combat = HashMap<UUID, Long>()
  private val totalKey = NamespacedKey(plugin, "afk_seconds")
  private val spellsKey = NamespacedKey(plugin, "afk_spells")

  private val settings get() = plugin.settings
  private val effects get() = plugin.effects
  private val messenger get() = plugin.messenger
  private val stats get() = plugin.stats

  private class Activity(var at: Long, var yaw: Float, var pitch: Float)

  fun isAfk(player: Player) = states.containsKey(player.uniqueId)

  fun state(id: UUID): AfkState? = states[id]

  /** The player's AFK state as others may know it: none while they're vanished. Safe from any thread. */
  fun visibleState(id: UUID): AfkState? = states[id]?.takeUnless { plugin.vanish.hidden(id) }

  fun afk(): List<Pair<Player, AfkState>> = states.mapNotNull { (id, state) -> Bukkit.getPlayer(id)?.let { it to state } }

  /** Seconds AFK across all sessions, this spell included. */
  fun totalSeconds(id: UUID): Long = (stats[id]?.seconds ?: 0L) + (states[id]?.seconds() ?: 0L)

  /** How many times the player went AFK, this spell included. */
  fun spells(id: UUID): Long = (stats[id]?.spells ?: 0L) + (if (states.containsKey(id)) 1 else 0)

  /** How many times the player went AFK with /afk since 2.3.0, this spell included. */
  fun manualSpells(id: UUID): Long = (stats[id]?.manual ?: 0L) + (if (states[id]?.manual == true) 1 else 0)

  /** How many times the player went AFK by idling since 2.3.0, this spell included. */
  fun autoSpells(id: UUID): Long = (stats[id]?.auto ?: 0L) + (if (states[id]?.manual == false) 1 else 0)

  /** Whether Mo’Afks knows this player's totals: they played since 2.3.0, or since the server started. */
  fun known(id: UUID) = stats[id] != null

  /** Every player's totals with the spells going on now, for boards and the server's sums. Safe from any thread. */
  fun records(): Map<UUID, AfkRecord> {
    val now = System.currentTimeMillis()
    val all = stats.all().toMutableMap()
    states.forEach { (id, state) ->
      // A vanished player's spell counts once it ends, so nobody learns it's going on.
      if (plugin.vanish.hidden(id)) return@forEach
      val record = all[id] ?: AfkRecord(name = state.name)
      all[id] = record.copy(
        seconds = record.seconds + state.seconds(now),
        spells = record.spells + 1,
        manual = record.manual + if (state.manual) 1 else 0,
        auto = record.auto + if (state.manual) 0 else 1,
      )
    }
    return all
  }

  /** Hands bStats the time of the spells still going on. Safe from any thread. */
  fun settle() {
    val now = System.currentTimeMillis()
    states.values.forEach { plugin.usage.afk(now - it.counted.getAndSet(now)) }
  }

  /** Everyone AFK now whom others may see, with their UUIDs; safe from any thread. */
  fun visibleStates(): List<Pair<UUID, AfkState>> = states.filterKeys { !plugin.vanish.hidden(it) }.toList()

  /** Everyone AFK now whom others may see; safe from any thread. */
  fun visible(): List<AfkState> = states.filterKeys { !plugin.vanish.hidden(it) }.values.toList()

  fun joined(player: Player) {
    plugin.vanish.check(player)
    touch(player)
    val data = player.persistentDataContainer
    stats.seed(player.uniqueId, player.name, data.getOrDefault(totalKey, PersistentDataType.LONG, 0L), data.getOrDefault(spellsKey, PersistentDataType.LONG, 0L))
  }

  /**
   * The player quit or got kicked: put everything back and forget them. Their totals stay in
   * [AfkStats], because Plan and WorldAPI read them after the quit.
   */
  fun left(player: Player) {
    exit(player, quiet = true)
    activity.remove(player.uniqueId)
    combat.remove(player.uniqueId)
    plugin.vanish.forget(player.uniqueId)
  }

  /** Something the player did. When its switch is on, it restarts the idle timer and ends AFK. */
  fun active(player: Player, trigger: Trigger) {
    if (!settings.enabled || !trigger.isOn(settings.exitTriggers)) return
    touch(player)
    if (isAfk(player)) {
      plugin.log.debug { "${player.name} is back: ${trigger.name.lowercase()}" }
      exit(player, quiet = false)
    }
  }

  /** The camera turned. It counts once it has turned past `look_degrees` since the last activity. */
  fun looked(player: Player, yaw: Float, pitch: Float) {
    val last = activity[player.uniqueId] ?: return touch(player)
    if (Rules.turned(last.yaw, last.pitch, yaw, pitch, settings.exitTriggers.lookDegrees)) active(player, Trigger.LOOK)
  }

  fun fought(player: Player) {
    combat[player.uniqueId] = System.currentTimeMillis()
  }

  /** Puts the player into AFK. Returns false when they're already AFK or can't be right now; a manual try hears why. */
  fun enter(player: Player, manual: Boolean, reason: String? = null): Boolean {
    if (!settings.enabled || isAfk(player)) return false
    val danger = Rules.danger(situation(player), settings.antiAbuse)
    if (danger != null) {
      if (manual) {
        messenger.tell(player, danger.message)
        effects.play(player, effects.declined)
      }
      plugin.log.debug { "${player.name} can't go AFK: ${danger.name.lowercase()}" }
      return false
    }

    val state = AfkState(player.name, reason, manual, System.currentTimeMillis(), player.location)
    apply(player, state)
    states[player.uniqueId] = state
    // The camera counts from where it points now, not from the last activity before AFK.
    touch(player)
    plugin.usage.entered(manual)
    plugin.analytics?.changed(player)
    plugin.log.debug { "${player.name} is AFK: ${if (manual) "manual" else "idle"}${reason?.let { ", $it" }.orEmpty()}" }

    val withReason: (String) -> Array<TagResolver> = { arrayOf(Placeholder.unparsed("reason", reason.orEmpty())) }
    when {
      !manual -> {
        messenger.tell(player, Message.AUTO_ENTER)
        messenger.announce(player, Message.AUTO_ENTER_BROADCAST)
      }
      reason == null -> {
        messenger.tell(player, Message.MANUAL_ENTER)
        messenger.announce(player, Message.MANUAL_ENTER_BROADCAST)
      }
      else -> {
        messenger.tell(player, Message.MANUAL_ENTER_REASON, withReason)
        messenger.announce(player, Message.MANUAL_ENTER_BROADCAST_REASON, withReason)
      }
    }
    effects.play(player, effects.enter)
    return true
  }

  /**
   * Ends AFK and puts back what it changed. A quiet exit (quit, death, another world, reload) only
   * cleans up; a real one also moves the player out of harm's way and tells everyone they're back.
   */
  fun exit(player: Player, quiet: Boolean) {
    val state = states.remove(player.uniqueId) ?: return
    restore(player, state)
    val now = System.currentTimeMillis()
    val seconds = state.seconds(now)
    plugin.usage.afk(now - state.counted.getAndSet(now))
    addTotal(player, seconds, state.manual)
    plugin.analytics?.changed(player)
    touch(player)
    if (quiet) return

    if (settings.protection.damage) {
      // Whatever piled up while AFK was harmless then and shouldn't hurt now.
      player.fallDistance = 0f
      player.freezeTicks = 0
      moveToSafety(player)
    }
    val time: (String) -> Array<TagResolver> = { arrayOf(messenger.time(it, seconds)) }
    messenger.tell(player, Message.EXIT, time)
    messenger.announce(player, Message.EXIT_BROADCAST, time)
    effects.play(player, effects.exit)
  }

  /** Every second: idle players go AFK, AFK players near the limit get warned, and those past it get kicked. */
  fun tick() {
    if (!settings.enabled) return
    // Vanishing or reappearing while AFK hides or shows the mark, and Plan hears of it.
    plugin.vanish.refresh().forEach { player ->
      states[player.uniqueId]?.let { state ->
        restore(player, state)
        apply(player, state)
        plugin.analytics?.changed(player)
      }
    }
    val now = System.currentTimeMillis()
    val kicks = mutableListOf<Player>()
    for (player in Bukkit.getOnlinePlayers()) {
      val state = states[player.uniqueId]
      if (state == null) {
        val last = activity[player.uniqueId]?.at ?: now.also { touch(player) }
        // A player on the death screen isn't idle in any way that matters.
        if (settings.auto.enabled && !player.isDead && (now - last) / 1000 >= settings.auto.seconds) enter(player, manual = false)
        continue
      }
      if (!settings.limits.enabled) continue
      val remaining = settings.limits.seconds - state.seconds(now)
      if (remaining <= 0) {
        kicks += player
        continue
      }
      if (!settings.limits.warning.enabled) continue
      Rules.warning(remaining, settings.limits.warning.seconds, state.warned)?.let { seconds ->
        val code = messenger.languageOf(player)
        messenger.render(code, Message.LIMIT_WARNING, player, messenger.time(code, seconds.toLong()))?.let(player::sendActionBar)
        effects.play(player, effects.warning)
      }
    }
    // Kicking fires the quit event, which changes the online list; so kick after the loop.
    kicks.forEach { player ->
      val reason = messenger.render(messenger.languageOf(player), Message.LIMIT_KICK, player) ?: Component.empty()
      plugin.log.info("${player.name} stayed AFK past the limit; kicking")
      player.kick(reason)
    }
  }

  fun particles() {
    if (settings.enabled) afk().forEach { (player, _) -> effects.ring(player, onlyFor = plugin.vanish.hidden(player.uniqueId)) }
  }

  /** The player fell into the void while AFK: back to where they went AFK, or to their spawn point. */
  fun rescue(player: Player) {
    val state = states[player.uniqueId] ?: return
    val target = state.anchor.takeIf { it.world == player.world && it.y >= player.world.minHeight }
      ?: player.respawnLocation
      ?: player.world.spawnLocation
    player.fallDistance = 0f
    player.teleportAsync(target).thenAccept { moved ->
      if (moved) {
        state.anchor = target
        messenger.tell(player, Message.VOID_RESCUE)
      }
    }
  }

  /** After a reload: lift what the old settings did and apply the new ones, keeping everyone AFK. */
  fun reapply() {
    if (!settings.enabled) return shutdown()
    states.forEach { (id, state) ->
      val player = Bukkit.getPlayer(id) ?: return@forEach run { states.remove(id) }
      restore(player, state)
      apply(player, state)
    }
  }

  /** Plugin disable: everyone leaves AFK quietly, with nothing left behind. */
  fun shutdown() {
    states.keys.toList().forEach { id -> Bukkit.getPlayer(id)?.let { exit(it, quiet = true) } ?: states.remove(id) }
  }

  /** Every player online starts with a fresh idle timer, as after a join. */
  fun start() {
    Bukkit.getOnlinePlayers().forEach(::joined)
  }

  private fun touch(player: Player) {
    val location = player.location
    val entry = activity.getOrPut(player.uniqueId) { Activity(0, location.yaw, location.pitch) }
    entry.at = System.currentTimeMillis()
    entry.yaw = location.yaw
    entry.pitch = location.pitch
  }

  private fun apply(player: Player, state: AfkState) {
    if (settings.protection.collision) {
      state.collidable = player.isCollidable
      player.isCollidable = false
      // The server alone isn't enough: the player's own client pushes them unless their team says no.
      val collision = Collision.pick()
      if (collision.off(player)) state.clientCollision = collision
      else plugin.log.debug { "${player.name} is in a team Mo’Afks can't change; other players can still push them" }
    }
    if (settings.sleepIgnored) {
      state.sleepingIgnored = player.isSleepingIgnored
      player.isSleepingIgnored = true
    }
    // A vanished player's name shows no mark: others would learn they're there.
    effects.mark?.takeUnless { plugin.vanish.hidden(player.uniqueId) }?.let { mark ->
      val original = player.playerListName()
      val marked = mark.append(original)
      state.listName = original
      state.marked = marked
      player.playerListName(marked)
    }
    if (settings.protection.mobs) {
      // Mobs that were already after the player let go; new ones can't pick them.
      player.getNearbyEntities(TARGET_RANGE, TARGET_RANGE / 2, TARGET_RANGE)
        .forEach { if (it is Mob && it.target?.uniqueId == player.uniqueId) it.target = null }
    }
  }

  private fun restore(player: Player, state: AfkState) {
    state.collidable?.let { player.isCollidable = it }
    state.clientCollision?.restore(player)
    // Only undo what's still ours: another plugin may have changed it since.
    state.sleepingIgnored?.let { if (player.isSleepingIgnored) player.isSleepingIgnored = it }
    state.marked?.let { marked ->
      if (plain(player.playerListName()) == plain(marked)) {
        // Paper reports an unset list name as the plain name; set it back to unset, not to that.
        player.playerListName(state.listName.takeUnless { it == Component.text(player.name) })
      }
    }
    state.collidable = null
    state.clientCollision = null
    state.sleepingIgnored = null
    state.listName = null
    state.marked = null
  }

  // Paper turns the name into a vanilla component and back, which may not compare equal; the text does.
  private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

  /** Adds a finished spell to the player's totals, in `stats.yml` and in their saved data. */
  private fun addTotal(player: Player, seconds: Long, manual: Boolean) {
    val data = player.persistentDataContainer
    if (stats[player.uniqueId] == null) {
      stats.seed(player.uniqueId, player.name, data.getOrDefault(totalKey, PersistentDataType.LONG, 0L), data.getOrDefault(spellsKey, PersistentDataType.LONG, 0L))
    }
    val record = stats.add(player.uniqueId, player.name, seconds, manual)
    data.set(totalKey, PersistentDataType.LONG, record.seconds)
    data.set(spellsKey, PersistentDataType.LONG, record.spells)
  }

  private fun moveToSafety(player: Player) {
    if (SafeSpot.isSafe(player.location)) return
    val spot = SafeSpot.near(player.location)
    if (spot == null) {
      plugin.log.debug { "${player.name} came back somewhere unsafe, and there's no safe spot nearby" }
      return
    }
    player.teleportAsync(spot).thenAccept { moved -> if (moved) messenger.tell(player, Message.MOVED_TO_SAFETY) }
  }

  private fun situation(player: Player): Situation {
    val rules = settings.antiAbuse
    return Situation(
      secondsSinceCombat = combat[player.uniqueId]?.let { (System.currentTimeMillis() - it) / 1000 },
      fallDistance = player.fallDistance,
      onGround = @Suppress("DEPRECATION") player.isOnGround,
      gliding = player.isGliding,
      flying = player.isFlying || player.isInsideVehicle,
      fireTicks = player.fireTicks,
      freezeTicks = player.freezeTicks,
      inWater = player.isInWater,
      inLava = player.isInLava,
      targeted = rules.targeted && player.getNearbyEntities(TARGET_RANGE, TARGET_RANGE / 2, TARGET_RANGE)
        .any { it is Mob && it.target?.uniqueId == player.uniqueId },
    )
  }

  private companion object {
    const val TARGET_RANGE = 32.0
  }
}

package ru.mairwunnx.moafks.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AwayFromKeyboardManager(private val plugin: PluginUnit) : Closeable {
  data class AfkState(val reason: String?, val since: Long, val manual: Boolean)

  private val scheduler = plugin.scheduler
  private val states = ConcurrentHashMap<UUID, AfkState>()
  private val lastActivity = ConcurrentHashMap<UUID, Long>()
  private val warningTasks = ConcurrentHashMap<UUID, MutableSet<Int>>()
  private val lastCombatAt = ConcurrentHashMap<UUID, Long>()
  private val lastAutoAfkAttempt = ConcurrentHashMap<UUID, Long>()
  private val combatCooldownMs = 15_000L // todo: вынести в конфиг.
  private val mm = MiniMessage.miniMessage()

  init {
    startAutoAfkChecker()
    startWarningChecker()
  }

  fun isAfk(player: Player) = states.containsKey(player.uniqueId)

  fun updateActivity(player: Player) {
    lastActivity[player.uniqueId] = System.currentTimeMillis()
  }

  fun enterManual(player: Player, reason: String? = null): Boolean {
    val config = plugin.configuration[GeneralConfigurationModel::class.java]

    if (!config.manual.enabled) return false
    if (isAfk(player)) return false

    // Проверка анти абьюза (combat, падение, урон)
    if (isInCombatOrDanger(player)) {
      player.sendMessage(config.manual.declinedMessage)
      player.playSound(player.location, config.manual.declinedSound, 1.0f, 1.0f)
      return false
    }

    plugin.logger.info { "🔄 Player ${player.name} entering manual AFK${if (reason != null) " with reason: $reason" else ""}" }

    val afkState = AfkState(reason, System.currentTimeMillis(), true)
    states[player.uniqueId] = afkState

    applyAfkProtections(player)

    val personalMessage = if (reason != null && reason.isNotBlank()) {
      processMessagePlaceholders(config.manual.enterMessageReasoned, player, reason, 0)
    } else {
      processMessagePlaceholders(config.manual.enterMessage, player, null, 0)
    }
    
    val broadcastMessage = if (reason != null && reason.isNotBlank()) {
      processMessagePlaceholders(config.manual.enterBroadcastMessageReasoned, player, reason, 0)
    } else {
      processMessagePlaceholders(config.manual.enterBroadcastMessage, player, null, 0)
    }

    player.sendMessage(personalMessage)
    player.playSound(player.location, config.manual.enterSound, 1.0f, 1.0f)

    Bukkit.getOnlinePlayers().forEach { p ->
      if (p != player) p.sendMessage(broadcastMessage)
    }

    plugin.logger.info { "✅ Player ${player.name} entered manual AFK" }
    return true
  }

  fun enterAuto(player: Player) {
    val config = plugin.configuration[GeneralConfigurationModel::class.java]

    if (!config.auto.enabled) return
    if (isAfk(player)) {
      plugin.logger.debug { "Player ${player.name} is already AFK, skipping auto AFK" }
      return
    }

    plugin.logger.info { "🔄 Player ${player.name} entering auto AFK" }

    val afkState = AfkState(null, System.currentTimeMillis(), false)
    states[player.uniqueId] = afkState

    applyAfkProtections(player)

    val personalMessage = processMessagePlaceholders(config.auto.enterMessage, player, null, 0)
    val broadcastMessage = processMessagePlaceholders(config.auto.enterBroadcastMessage, player, null, 0)

    player.sendMessage(personalMessage)
    player.playSound(player.location, config.auto.enterSound, 1.0f, 1.0f)

    Bukkit.getOnlinePlayers().forEach { p ->
      if (p != player) p.sendMessage(broadcastMessage)
    }

    plugin.logger.info { "✅ Player ${player.name} entered auto AFK" }
  }

  fun exit(player: Player, safe: Boolean = true, silent: Boolean = false) {
    val afkState = states.remove(player.uniqueId) ?: return
    val config = plugin.configuration[GeneralConfigurationModel::class.java]

    plugin.logger.info { "🔄 Player ${player.name} exiting AFK" }

    if (safe) {
      val lm = plugin.location
      val locNow = player.location
      if (!lm.isSafeLocation(locNow)) {
        lm.findSafeLocation(player)?.let { found ->
          val safeLoc = lm.clampToWorldBorder(found) // на всякий случай проверим то что в бордер вписываемся
          player.teleportAsync(safeLoc).thenAccept { ok ->
            if (ok && !silent) player.sendMessage(config.system.messages.unsafe)
          }
        }
      }
    }

    removeAfkProtections(player)
    warningTasks.remove(player.uniqueId)

    if (!silent) {
      val exitMessage = if (afkState.manual) {
        processMessagePlaceholders(config.manual.exitMessage, player, afkState.reason, 0)
      } else {
        processMessagePlaceholders(config.auto.exitMessage, player, afkState.reason, 0)
      }
      val exitSound = if (afkState.manual) config.manual.exitSound else config.auto.exitSound

      player.sendMessage(exitMessage)
      player.playSound(player.location, exitSound, 1.0f, 1.0f)
    }

    plugin.logger.info { "✅ Player ${player.name} exited AFK" }
  }

  fun noteCombat(player: Player) {
    lastCombatAt[player.uniqueId] = System.currentTimeMillis()
  }

  fun noteCombatFromDamageCause(cause: DamageCause, player: Player) {
    when (cause) {
      DamageCause.ENTITY_ATTACK,
      DamageCause.ENTITY_SWEEP_ATTACK,
      DamageCause.THORNS,
      DamageCause.PROJECTILE,
      DamageCause.ENTITY_EXPLOSION -> noteCombat(player)

      else -> {} // падение, огонь и прочее - не считаем боем
    }
  }

  private fun isInCombatOrDanger(player: Player): Boolean {
    val now = System.currentTimeMillis()
    val last = lastCombatAt[player.uniqueId] ?: 0L
    val recentlyInCombat = (now - last) <= combatCooldownMs
    val falling = player.fallDistance > 0f
    return recentlyInCombat || falling
  }

  private fun applyAfkProtections(player: Player) {
    val config = plugin.configuration[GeneralConfigurationModel::class.java]

    plugin.effects.applyAfkVisuals(player)

    if (config.prevents.contains(GeneralConfigurationModel.PreventType.COLLISION)) {
      plugin.effects.setCollisionDisabled(player, true)
    }

    player.isSleepingIgnored = true
  }

  private fun removeAfkProtections(player: Player) {
    plugin.effects.removeAfkVisuals(player)
    plugin.effects.setCollisionDisabled(player, false)

    player.isSleepingIgnored = false
  }

  private fun processMessagePlaceholders(message: Component, player: Player, reason: String?, seconds: Int): Component {
    val miniMessageText = mm.serialize(message)

    val processed = miniMessageText
      .replace("<player>", player.name)
      .replace("<seconds>", seconds.toString())
      .replace("<reason>", reason ?: "")

    return mm.deserialize(processed)
  }

  private fun startAutoAfkChecker() {
    scheduler.runRepeating(name = TASK_AUTO_AFK, delayTicks = 20L, periodTicks = 20L * 10) {
      val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
      if (!cfg.auto.enabled) return@runRepeating

      val now = System.currentTimeMillis()
      val thresholdMs = cfg.auto.seconds * 1000L

      Bukkit.getOnlinePlayers().forEach { p ->
        if (!isAfk(p)) {
          val last = lastActivity[p.uniqueId] ?: now
          if (now - last >= thresholdMs) {
            if (!isAfk(p)) {
              plugin.logger.debug { "Player ${p.name} inactive for ${(now - last) / 1000}s, entering auto AFK" }
              enterAuto(p)
            }
          }
        }
      }
    }
  }

  private fun startWarningChecker() {
    scheduler.runRepeating(name = TASK_WARNINGS, delayTicks = 20L, periodTicks = 20L) {
      val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
      if (!cfg.limits.enabled) return@runRepeating

      val now = System.currentTimeMillis()

      states.forEach { (uuid, state) ->
        val p = Bukkit.getPlayer(uuid) ?: return@forEach
        val afkSec = ((now - state.since) / 1000).toInt()

        if (afkSec >= cfg.limits.seconds) {
          p.kick(cfg.limits.kickMessage)
          states.remove(uuid)
          warningTasks.remove(uuid)
          return@forEach
        }

        if (cfg.limits.warning.enabled) {
          val remaining = cfg.limits.seconds - afkSec
          val sent = warningTasks.getOrPut(uuid) { mutableSetOf() }
          cfg.limits.warning.seconds.forEach { s ->
            if (remaining <= s && sent.add(s)) {
              val msg = processMessagePlaceholders(cfg.limits.warning.text, p, state.reason, s)
              p.sendActionBar(msg)
              p.playSound(p.location, cfg.limits.warning.sound, 1f, 1f)
            }
          }
        }
      }
    }
  }

  override fun close() {
    scheduler.cancel(TASK_AUTO_AFK)
    scheduler.cancel(TASK_WARNINGS)

    states.clear()
    lastActivity.clear()
    warningTasks.clear()
    lastCombatAt.clear()
  }

  private companion object {
    const val TASK_AUTO_AFK = "auto-afk"
    const val TASK_WARNINGS = "afk-warnings"
  }
}

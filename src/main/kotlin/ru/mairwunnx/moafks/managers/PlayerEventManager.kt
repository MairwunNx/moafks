@file:Suppress("unused")

package ru.mairwunnx.moafks.managers

import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.HIGHEST
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.ExitTrigger
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType
import java.io.Closeable

class PlayerEventManager(private val plugin: PluginUnit) : Closeable, Listener {
  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onPlayerMove(e: PlayerMoveEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]

    if (e.hasChangedPosition()) {
      if (ExitTrigger.MOVE in cfg.exitTriggers) {
        handleExitTrigger(e.player, ExitTrigger.MOVE)
      }
    }
    plugin.afk.updateActivity(e.player)
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onPlayerChat(e: AsyncChatEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (ExitTrigger.CHAT !in cfg.exitTriggers) {
      Bukkit.getScheduler().runTask(plugin) { -> plugin.afk.updateActivity(e.player) }
      return
    }
    Bukkit.getScheduler().runTask(plugin) { ->
      handleExitTrigger(e.player, ExitTrigger.CHAT)
      plugin.afk.updateActivity(e.player)
    }
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onPlayerCommand(e: PlayerCommandPreprocessEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    val root = e.message.removePrefix("/").trim().split(Regex("\\s+")).firstOrNull()?.lowercase() ?: return
    if (root == "afk" || root == "moafks") { // свои команды не снимают AFK
      plugin.afk.updateActivity(e.player) // всё равно засчитаем активность
      return
    }
    if (ExitTrigger.COMMAND in cfg.exitTriggers) {
      handleExitTrigger(e.player, ExitTrigger.COMMAND)
    }
    plugin.afk.updateActivity(e.player)
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (ExitTrigger.COMBAT !in cfg.exitTriggers) return

    val attacker: Player? = when (val d = e.damager) {
      is Player -> d
      is Projectile -> (d.shooter as? Player)
      else -> null
    }

    attacker?.let {
      if (plugin.afk.isAfk(it)) {
        handleExitTrigger(it, ExitTrigger.COMBAT)
      }
      plugin.afk.updateActivity(it)
    }
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onEntityDamage(e: EntityDamageEvent) {
    val p = e.entity as? Player ?: return
    if (!plugin.afk.isAfk(p)) return

    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    val cause = e.cause

    val melee = cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK || cause == DamageCause.THORNS
    val projectile = cause == DamageCause.PROJECTILE
    val explosion = cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.BLOCK_EXPLOSION
    val fire = cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA || cause == DamageCause.HOT_FLOOR
    val fall = cause == DamageCause.FALL || cause == DamageCause.VOID

    when {
      melee && PreventType.ENTITY_DAMAGE in cfg.prevents -> e.isCancelled = true
      projectile && PreventType.PROJECTILE_DAMAGE in cfg.prevents -> e.isCancelled = true
      explosion && PreventType.EXPLOSIONS in cfg.prevents -> e.isCancelled = true
      fire && PreventType.FIRE in cfg.prevents -> e.isCancelled = true
      fall && PreventType.FALL in cfg.prevents -> e.isCancelled = true
      else -> {} // другие причины (яд, утопление и т.п.) не трогаем
    }
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onEntityCombust(e: EntityCombustEvent) {
    val p = e.entity as? Player ?: return
    if (!plugin.afk.isAfk(p)) return
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.FIRE in cfg.prevents) e.isCancelled = true
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onFoodLevelChange(e: FoodLevelChangeEvent) {
    val p = e.entity as? Player ?: return
    if (!plugin.afk.isAfk(p)) return
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.HUNGER !in cfg.prevents) return

    e.isCancelled = true
    if (p.exhaustion > 0f) p.exhaustion = 0f
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onEntityTargetLiving(e: EntityTargetLivingEntityEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.MOB_TARGET !in cfg.prevents) return

    val p = e.target as? Player ?: return
    if (!plugin.afk.isAfk(p)) return

    e.target = null
    e.isCancelled = true
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onPlayerInteract(event: PlayerInteractEvent) {
    plugin.afk.updateActivity(event.player)
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onInventoryClick(event: InventoryClickEvent) {
    val player = event.whoClicked as? Player ?: return
    plugin.afk.updateActivity(player)
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = true)
  fun onPlayerJoin(event: PlayerJoinEvent) {
    plugin.afk.updateActivity(event.player)
  }

  @EventHandler(priority = MONITOR)
  fun onPlayerQuit(e: PlayerQuitEvent) {
    val p = e.player
    if (plugin.afk.isAfk(p)) {
      plugin.afk.exit(p, safe = false, silent = true)
    }
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onKnockback(e: EntityKnockbackEvent) {
    val p = e.entity as? Player ?: return
    if (plugin.afk.isAfk(p)) e.isCancelled = true
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onProjectileCollide(e: ProjectileHitEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.PROJECTILE_DAMAGE !in cfg.prevents) return

    val victim = e.hitEntity as? Player ?: return
    if (!plugin.afk.isAfk(victim)) return

    e.isCancelled = true
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onPotionSplash(e: PotionSplashEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.PROJECTILE_DAMAGE !in cfg.prevents) return
    e.affectedEntities.forEach { ent ->
      val p = ent as? Player ?: return@forEach
      if (plugin.afk.isAfk(p)) e.setIntensity(p, 0.0)
    }
  }

  @EventHandler(priority = HIGHEST, ignoreCancelled = true)
  fun onCloudApply(e: AreaEffectCloudApplyEvent) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    if (PreventType.PROJECTILE_DAMAGE !in cfg.prevents) return
    e.affectedEntities.removeIf { it is Player && plugin.afk.isAfk(it) }
  }

  @EventHandler(priority = MONITOR, ignoreCancelled = false)
  fun onEntityDamageByEntityEvent(e: EntityDamageByEntityEvent) {
    val victim = e.entity as? Player
    val attacker = when (val d = e.damager) {
      is Player -> d
      is Projectile -> d.shooter as? Player ?: (d.shooter as? LivingEntity) // моб-стрелок
      is LivingEntity -> d
      else -> null
    }

    // Трекаем "бой" для жертвы и/или атакующего (если игроки/мобы)
    victim?.let { plugin.afk.noteCombatFromDamageCause(e.cause, it) }
    if (attacker is Player) plugin.afk.noteCombatFromDamageCause(e.cause, attacker)
  }

  private fun handleExitTrigger(player: Player, trigger: ExitTrigger) {
    val config = plugin.configuration[GeneralConfigurationModel::class.java]

    if (config.exitTriggers.contains(trigger) && plugin.afk.isAfk(player)) {
      plugin.afk.exit(player)
      plugin.logger.debug { "Player ${player.name} exited AFK due to trigger: $trigger" }
    }
  }

  override fun close() {
  }
}
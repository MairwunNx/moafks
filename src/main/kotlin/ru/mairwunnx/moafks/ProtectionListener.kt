package ru.mairwunnx.moafks

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent
import io.papermc.paper.event.entity.WardenAngerChangeEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PigZombieAngerEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.event.raid.RaidTriggerEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.potion.PotionEffectType

/**
 * Keeps AFK players out of harm's way, and keeps the protection from paying off. CLAUDE.md lists
 * every event Mo’Afks looked at and what it does with each; a switch in `protection` or `gains`
 * stands behind every handler.
 */
class ProtectionListener(private val plugin: MoAfksPlugin) : Listener {
  private val protection get() = plugin.settings.protection

  private fun afk(entity: Entity?): Boolean = entity is Player && plugin.afk.isAfk(entity)

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onDamage(event: EntityDamageEvent) {
    val player = event.entity as? Player ?: return
    if (!plugin.afk.isAfk(player)) return
    when (Rules.damage(event.cause, protection)) {
      DamageVerdict.ALLOW -> Unit
      DamageVerdict.CANCEL -> event.isCancelled = true
      DamageVerdict.RESCUE -> {
        event.isCancelled = true
        plugin.afk.rescue(player)
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onCombust(event: EntityCombustEvent) {
    if (protection.damage && afk(event.entity)) event.isCancelled = true
  }

  /** Air doesn't run out under water, so coming back doesn't start with drowning. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onAir(event: EntityAirChangeEvent) {
    val player = event.entity as? Player ?: return
    if (protection.damage && plugin.afk.isAfk(player) && event.amount < player.remainingAir) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onKnockback(event: EntityKnockbackEvent) {
    if (protection.knockback && afk(event.entity)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPushed(event: EntityPushedByEntityAttackEvent) {
    if (protection.knockback && afk(event.entity)) event.isCancelled = true
  }

  /** The projectile flies on through: arrows, snowballs, ender pearls, wind charges, and fishing hooks alike. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onProjectileHit(event: ProjectileHitEvent) {
    if (protection.projectiles && afk(event.hitEntity)) event.isCancelled = true
  }

  /** A rod that hooked the player before they went AFK can't reel them in. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onFish(event: PlayerFishEvent) {
    if (protection.projectiles && event.state == PlayerFishEvent.State.CAUGHT_ENTITY && afk(event.caught)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onEffect(event: EntityPotionEffectEvent) {
    if (!protection.effects || !afk(event.entity)) return
    val harmful = event.newEffect?.type?.effectCategory == PotionEffectType.Category.HARMFUL
    if (Rules.blocksEffect(event.action, event.cause, harmful)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onFood(event: FoodLevelChangeEvent) {
    if (protection.hunger && afk(event.entity) && event.foodLevel < event.entity.foodLevel) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onExhaustion(event: EntityExhaustionEvent) {
    if (protection.hunger && afk(event.entity)) event.isCancelled = true
  }

  /** With hunger frozen, healing from a full bar would be free; so it waits too. Potions and beacons still heal. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onRegain(event: EntityRegainHealthEvent) {
    if (protection.hunger && afk(event.entity) && event.regainReason == EntityRegainHealthEvent.RegainReason.SATIATED) event.isCancelled = true
  }

  /** Every mob that picks a target goes through here, wardens and piglins included. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onTarget(event: EntityTargetEvent) {
    if (protection.mobs && afk(event.target)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onWardenAnger(event: WardenAngerChangeEvent) {
    if (protection.mobs && afk(event.target) && event.newAnger > event.oldAnger) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onEnderman(event: EndermanAttackPlayerEvent) {
    if (protection.mobs && afk(event.player)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onZombifiedPiglin(event: PigZombieAngerEvent) {
    if (protection.mobs && afk(event.target)) event.isCancelled = true
  }

  /** Insomnia keeps counting while AFK, as vanilla's statistic should; phantoms just don't come for it. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPhantom(event: PhantomPreSpawnEvent) {
    if (protection.mobs && afk(event.spawningEntity)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onVehicleEnter(event: VehicleEnterEvent) {
    if (protection.vehicles && afk(event.entered)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onVehicleCollision(event: VehicleEntityCollisionEvent) {
    if (protection.vehicles && afk(event.entity)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onRaid(event: RaidTriggerEvent) {
    if (protection.raids && afk(event.player)) event.isCancelled = true
  }

  /** Steps, sounds, and anything else an AFK player gives off go unheard by sculk and wardens. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onVibration(event: GenericGameEvent) {
    if (protection.vibrations && afk(event.entity)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPickup(event: PlayerAttemptPickupItemEvent) {
    if (plugin.settings.gains.items || !afk(event.player)) return
    event.flyAtPlayer = false
    event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPickupArrow(event: PlayerPickupArrowEvent) {
    if (!plugin.settings.gains.items && afk(event.player)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPickupExperience(event: PlayerPickupExperienceEvent) {
    if (!plugin.settings.gains.experience && afk(event.player)) event.isCancelled = true
  }
}

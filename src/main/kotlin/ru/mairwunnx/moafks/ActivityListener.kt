package ru.mairwunnx.moafks

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/**
 * What the player does with their own hands. Each event maps to a [Trigger]; the switches in
 * `exit_triggers` decide whether it counts. Movement comes from the keys the client presses, not from
 * position: water, pistons, and minecarts move a player without them doing anything. Events another
 * plugin cancelled still count, since the player still acted.
 */
class ActivityListener(private val plugin: MoAfksPlugin) : Listener {
  private val afk get() = plugin.afk

  @EventHandler(priority = EventPriority.MONITOR)
  fun onInput(event: PlayerInputEvent) {
    val input = event.input
    Rules.inputTriggers(input.isForward, input.isBackward, input.isLeft, input.isRight, input.isJump, input.isSneak, input.isSprint)
      .forEach { afk.active(event.player, it) }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onMove(event: PlayerMoveEvent) {
    if (event.hasChangedOrientation()) afk.looked(event.player, event.to.yaw, event.to.pitch)
  }

  /** Chat arrives off the server thread; the rest happens on it. */
  @EventHandler(priority = EventPriority.MONITOR)
  fun onChat(event: AsyncChatEvent) {
    val player = event.player
    plugin.server.scheduler.runTask(plugin, Runnable { if (player.isOnline) afk.active(player, Trigger.CHAT) })
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onCommand(event: PlayerCommandPreprocessEvent) {
    if (!Rules.isOwnCommand(event.message)) afk.active(event.player, Trigger.COMMAND)
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onAttack(event: PrePlayerAttackEntityEvent) = afk.active(event.player, Trigger.ATTACK)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onInteract(event: PlayerInteractEvent) {
    // Pressure plates and tripwire fire on their own when water carries a player onto them.
    if (event.action != Action.PHYSICAL) afk.active(event.player, Trigger.INTERACT)
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onInteractEntity(event: PlayerInteractEntityEvent) = afk.active(event.player, Trigger.INTERACT)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onBreak(event: BlockBreakEvent) = afk.active(event.player, Trigger.BLOCKS)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onPlace(event: BlockPlaceEvent) = afk.active(event.player, Trigger.BLOCKS)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onOpen(event: InventoryOpenEvent) {
    (event.player as? Player)?.let { afk.active(it, Trigger.INVENTORY) }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onClick(event: InventoryClickEvent) {
    (event.whoClicked as? Player)?.let { afk.active(it, Trigger.INVENTORY) }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onDrop(event: PlayerDropItemEvent) = afk.active(event.player, Trigger.DROP)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onSwap(event: PlayerSwapHandItemsEvent) = afk.active(event.player, Trigger.SWAP_HANDS)

  @EventHandler(priority = EventPriority.MONITOR)
  fun onHeld(event: PlayerItemHeldEvent) = afk.active(event.player, Trigger.HELD_SLOT)

  /** Damage taken or dealt marks a fight, which keeps a player from going AFK for `combat_seconds`. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onDamage(event: EntityDamageEvent) {
    (event.entity as? Player)?.let(afk::fought)
    if (event !is EntityDamageByEntityEvent || event.entity !is LivingEntity) return
    val attacker = when (val damager = event.damager) {
      is Player -> damager
      is Projectile -> damager.shooter as? Player
      else -> null
    }
    attacker?.let(afk::fought)
  }
}

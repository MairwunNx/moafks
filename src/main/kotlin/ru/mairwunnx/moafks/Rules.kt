package ru.mairwunnx.moafks

import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityPotionEffectEvent
import kotlin.math.abs

/** What ends AFK and restarts the idle timer, one per switch in `exit_triggers`. */
enum class Trigger {
  MOVE, LOOK, JUMP, SNEAK, SPRINT, CHAT, COMMAND, ATTACK, INTERACT, BLOCKS, INVENTORY, DROP, SWAP_HANDS, HELD_SLOT;

  fun isOn(triggers: Settings.ExitTriggers): Boolean = when (this) {
    MOVE -> triggers.move
    LOOK -> triggers.look
    JUMP -> triggers.jump
    SNEAK -> triggers.sneak
    SPRINT -> triggers.sprint
    CHAT -> triggers.chat
    COMMAND -> triggers.command
    ATTACK -> triggers.attack
    INTERACT -> triggers.interact
    BLOCKS -> triggers.blocks
    INVENTORY -> triggers.inventory
    DROP -> triggers.drop
    SWAP_HANDS -> triggers.swapHands
    HELD_SLOT -> triggers.heldSlot
  }
}

/** Why a player can't go AFK right now. */
enum class Danger(val message: Message) {
  COMBAT(Message.DECLINED_COMBAT),
  FALLING(Message.DECLINED_FALLING),
  BURNING(Message.DECLINED_BURNING),
  LIQUID(Message.DECLINED_LIQUID),
  TARGETED(Message.DECLINED_TARGETED),
}

/** What the damage listener does with a hit on an AFK player. */
enum class DamageVerdict { ALLOW, CANCEL, RESCUE }

/** The state the anti-abuse checks look at, read from the player in one go. */
data class Situation(
  val secondsSinceCombat: Long?,
  val fallDistance: Float,
  val onGround: Boolean,
  val gliding: Boolean,
  val flying: Boolean,
  val fireTicks: Int,
  val freezeTicks: Int,
  val inWater: Boolean,
  val inLava: Boolean,
  val targeted: Boolean,
)

/**
 * The decisions Mo’Afks makes, kept apart from Bukkit so tests can run them: what counts as activity,
 * what an AFK player is shielded from, and when a player may go AFK at all.
 */
object Rules {
  /** The keys held in a player input packet. Releasing a key isn't activity; pressing one is. */
  fun inputTriggers(forward: Boolean, backward: Boolean, left: Boolean, right: Boolean, jump: Boolean, sneak: Boolean, sprint: Boolean): Set<Trigger> =
    buildSet {
      if (forward || backward || left || right) add(Trigger.MOVE)
      if (jump) add(Trigger.JUMP)
      if (sneak) add(Trigger.SNEAK)
      if (sprint) add(Trigger.SPRINT)
    }

  /** Whether the camera turned by more than [degrees] on either axis; yaw wraps around at 360. */
  fun turned(fromYaw: Float, fromPitch: Float, toYaw: Float, toPitch: Float, degrees: Float): Boolean {
    val yaw = abs(((toYaw - fromYaw) % 360f + 540f) % 360f - 180f)
    return yaw > degrees || abs(toPitch - fromPitch) > degrees
  }

  /** `/afk`, `/moafks reload`, and `/moafks:afk` are the plugin's own and don't end AFK. */
  fun isOwnCommand(message: String): Boolean {
    val label = message.removePrefix("/").trimStart().substringBefore(' ').lowercase()
    return label.substringAfter(':') in OWN_COMMANDS && (':' !in label || label.startsWith("moafks:"))
  }

  fun damage(cause: DamageCause, protection: Settings.Protection): DamageVerdict = when {
    !protection.damage -> DamageVerdict.ALLOW
    // /kill and plugins that kill on purpose still work.
    cause == DamageCause.KILL || cause == DamageCause.SUICIDE -> DamageVerdict.ALLOW
    // Cancelling void damage would leave the player falling forever.
    cause == DamageCause.VOID -> if (protection.void) DamageVerdict.RESCUE else DamageVerdict.ALLOW
    else -> DamageVerdict.CANCEL
  }

  /** Harmful effects don't land on AFK players, unless an admin gives them by command. Removing an effect is always fine. */
  fun blocksEffect(action: EntityPotionEffectEvent.Action, cause: EntityPotionEffectEvent.Cause, harmful: Boolean): Boolean =
    harmful && cause != EntityPotionEffectEvent.Cause.COMMAND &&
      (action == EntityPotionEffectEvent.Action.ADDED || action == EntityPotionEffectEvent.Action.CHANGED)

  /** The first reason the player can't go AFK, or null when they can. */
  fun danger(situation: Situation, rules: Settings.AntiAbuse): Danger? = when {
    situation.secondsSinceCombat != null && situation.secondsSinceCombat < rules.combatSeconds -> Danger.COMBAT
    rules.falling && (situation.gliding || (!situation.onGround && !situation.flying && situation.fallDistance > 0f)) -> Danger.FALLING
    rules.burning && (situation.fireTicks > 0 || situation.freezeTicks > 0) -> Danger.BURNING
    rules.liquids && (situation.inWater || situation.inLava) -> Danger.LIQUID
    rules.targeted && situation.targeted -> Danger.TARGETED
    else -> null
  }

  /**
   * The warning to show with [remaining] seconds left, or null. Every mark passed so far goes into
   * [shown], so a skipped second or a reload doesn't fire several warnings at once.
   */
  fun warning(remaining: Long, marks: List<Int>, shown: MutableSet<Int>): Int? {
    val due = marks.filter { remaining <= it && it !in shown }
    shown += due
    return due.minOrNull()
  }

  /** A duration in the largest unit that fits: seconds under a minute, minutes under an hour, then hours and minutes. */
  fun duration(seconds: Long): Pair<Message, Map<String, String>> {
    val s = seconds.coerceAtLeast(0)
    return when {
      s < 60 -> Message.SECONDS to mapOf("seconds" to "$s")
      s < 3600 -> Message.MINUTES to mapOf("minutes" to "${s / 60}")
      else -> Message.HOURS to mapOf("hours" to "${s / 3600}", "minutes" to "${s % 3600 / 60}")
    }
  }

  private val OWN_COMMANDS = setOf("afk", "moafks")
}

package ru.mairwunnx.moafks

import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityPotionEffectEvent.Action
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RulesTest {
  private val calm = Situation(
    secondsSinceCombat = null, fallDistance = 0f, onGround = true, gliding = false, flying = false,
    fireTicks = -20, freezeTicks = 0, inWater = false, inLava = false, targeted = false,
  )
  private val rules = Settings.AntiAbuse()

  @Test fun `pressing a key is activity, releasing one isn't`() {
    assertEquals(setOf(Trigger.MOVE), Rules.inputTriggers(true, false, false, false, false, false, false))
    assertEquals(setOf(Trigger.MOVE, Trigger.JUMP, Trigger.SPRINT), Rules.inputTriggers(false, false, true, false, true, false, true))
    assertEquals(setOf(Trigger.SNEAK), Rules.inputTriggers(false, false, false, false, false, true, false))
    assertEquals(emptySet(), Rules.inputTriggers(false, false, false, false, false, false, false))
  }

  @Test fun `each trigger follows its own switch`() {
    val triggers = Settings.ExitTriggers(chat = false, heldSlot = false)
    assertFalse(Trigger.CHAT.isOn(triggers))
    assertFalse(Trigger.HELD_SLOT.isOn(triggers))
    assertTrue(Trigger.entries.filter { it != Trigger.CHAT && it != Trigger.HELD_SLOT }.all { it.isOn(triggers) })
  }

  @Test fun `the camera counts past the threshold, across the yaw wrap`() {
    assertFalse(Rules.turned(10f, 0f, 30f, 10f, 30f))
    assertTrue(Rules.turned(10f, 0f, 50f, 0f, 30f))
    assertTrue(Rules.turned(0f, -40f, 0f, 0f, 30f))
    assertFalse(Rules.turned(170f, 0f, -170f, 0f, 30f))
    assertTrue(Rules.turned(-179f, 0f, 140f, 0f, 30f))
  }

  @Test fun `the plugin's own commands don't end AFK`() {
    assertTrue(Rules.isOwnCommand("/afk"))
    assertTrue(Rules.isOwnCommand("/afk brb, tea"))
    assertTrue(Rules.isOwnCommand("/AFK"))
    assertTrue(Rules.isOwnCommand("/moafks reload"))
    assertTrue(Rules.isOwnCommand("/moafks:afk"))
    assertFalse(Rules.isOwnCommand("/essentials:afk"))
    assertFalse(Rules.isOwnCommand("/afkcheck"))
    assertFalse(Rules.isOwnCommand("/home"))
  }

  @Test fun `damage is cancelled, except kills, and the void rescues`() {
    val on = Settings.Protection()
    listOf(
      DamageCause.ENTITY_ATTACK, DamageCause.SUFFOCATION, DamageCause.DROWNING, DamageCause.FREEZE, DamageCause.LAVA,
      DamageCause.LIGHTNING, DamageCause.MAGIC, DamageCause.THORNS, DamageCause.SONIC_BOOM, DamageCause.FALL,
      DamageCause.WITHER, DamageCause.POISON, DamageCause.CRAMMING, DamageCause.FALLING_BLOCK, DamageCause.CUSTOM,
    ).forEach { assertEquals(DamageVerdict.CANCEL, Rules.damage(it, on), "$it") }
    assertEquals(DamageVerdict.ALLOW, Rules.damage(DamageCause.KILL, on))
    assertEquals(DamageVerdict.RESCUE, Rules.damage(DamageCause.VOID, on))
    assertEquals(DamageVerdict.ALLOW, Rules.damage(DamageCause.VOID, on.copy(void = false)))
    assertEquals(DamageVerdict.ALLOW, Rules.damage(DamageCause.LAVA, on.copy(damage = false)))
  }

  @Test fun `harmful effects don't land, unless from a command`() {
    assertTrue(Rules.blocksEffect(Action.ADDED, Cause.ATTACK, harmful = true))
    assertTrue(Rules.blocksEffect(Action.CHANGED, Cause.POTION_SPLASH, harmful = true))
    assertTrue(Rules.blocksEffect(Action.ADDED, Cause.PLUGIN, harmful = true))
    assertFalse(Rules.blocksEffect(Action.ADDED, Cause.COMMAND, harmful = true))
    assertFalse(Rules.blocksEffect(Action.ADDED, Cause.BEACON, harmful = false))
    assertFalse(Rules.blocksEffect(Action.REMOVED, Cause.MILK, harmful = true))
  }

  @Test fun `a calm player may go AFK`() {
    assertNull(Rules.danger(calm, rules))
  }

  @Test fun `anti-abuse refuses fights, falls, fire, liquids, and pursuit`() {
    assertEquals(Danger.COMBAT, Rules.danger(calm.copy(secondsSinceCombat = 3), rules))
    assertNull(Rules.danger(calm.copy(secondsSinceCombat = 15), rules))
    assertEquals(Danger.FALLING, Rules.danger(calm.copy(onGround = false, fallDistance = 4f), rules))
    assertEquals(Danger.FALLING, Rules.danger(calm.copy(gliding = true), rules))
    assertNull(Rules.danger(calm.copy(onGround = false, flying = true, fallDistance = 4f), rules))
    assertEquals(Danger.BURNING, Rules.danger(calm.copy(fireTicks = 40), rules))
    assertEquals(Danger.BURNING, Rules.danger(calm.copy(freezeTicks = 10), rules))
    assertEquals(Danger.LIQUID, Rules.danger(calm.copy(inWater = true), rules))
    assertEquals(Danger.LIQUID, Rules.danger(calm.copy(inLava = true), rules))
    assertEquals(Danger.TARGETED, Rules.danger(calm.copy(targeted = true), rules))
  }

  @Test fun `each anti-abuse check has its switch`() {
    val off = Settings.AntiAbuse(combatSeconds = 0, falling = false, burning = false, liquids = false, targeted = false)
    val risky = calm.copy(secondsSinceCombat = 0, gliding = true, fireTicks = 40, inLava = true, targeted = true)
    assertNull(Rules.danger(risky, off))
  }

  @Test fun `a warning fires once per mark, and only the nearest of several passed at once`() {
    val shown = mutableSetOf<Int>()
    val marks = listOf(60, 30, 10, 5)
    assertNull(Rules.warning(90, marks, shown))
    assertEquals(60, Rules.warning(60, marks, shown))
    assertNull(Rules.warning(59, marks, shown))
    assertEquals(10, Rules.warning(8, marks, shown))
    assertEquals(setOf(60, 30, 10), shown)
    assertEquals(5, Rules.warning(5, marks, shown))
  }

  @Test fun `durations pick the largest unit that fits`() {
    assertEquals(Message.SECONDS to mapOf("seconds" to "42"), Rules.duration(42))
    assertEquals(Message.MINUTES to mapOf("minutes" to "5"), Rules.duration(330))
    assertEquals(Message.HOURS to mapOf("hours" to "2", "minutes" to "3"), Rules.duration(7380))
    assertEquals(Message.SECONDS to mapOf("seconds" to "0"), Rules.duration(-5))
  }
}

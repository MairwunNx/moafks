package ru.mairwunnx.moafks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AllayAnswersTest {
  @Test fun `the prompt fits Allay's limit`() {
    assertTrue(AllayAnswers.PROMPT.length <= 2000, "${AllayAnswers.PROMPT.length} characters")
  }

  @Test fun `the defaults read as the shipped config says`() {
    val rules = AllayAnswers.rules(Settings())
    assertEquals(300, rules.autoAfkAfterSeconds)
    assertEquals(1200, rules.kickAfterSeconds)
    assertEquals(listOf(60, 30, 10, 5, 4, 3, 2, 1), rules.kickWarningsAtSecondsLeft)
    assertEquals(11, rules.protections.size)
    assertEquals(5, rules.refusedWhen.size)
    assertEquals(Trigger.entries.map { it.name.lowercase() }, rules.endsOn)
    assertEquals(30f, rules.lookDegrees)
  }

  @Test fun `a part that's off reads as null or leaves the list`() {
    val settings = Settings(
      auto = Settings.Auto(enabled = false),
      limits = Settings.Limits(enabled = false),
      protection = Settings.Protection(damage = false, mobs = false),
      antiAbuse = Settings.AntiAbuse(combatSeconds = 0, targeted = false),
      exitTriggers = Settings.ExitTriggers(look = false, chat = false),
    )
    val rules = AllayAnswers.rules(settings)
    assertNull(rules.autoAfkAfterSeconds)
    assertNull(rules.kickAfterSeconds)
    assertEquals(emptyList(), rules.kickWarningsAtSecondsLeft)
    assertEquals(9, rules.protections.size)
    assertEquals(listOf("falling or gliding", "on fire or freezing", "in water or lava"), rules.refusedWhen)
    assertNull(rules.lookDegrees)
    assertTrue("chat" !in rules.endsOn && "look" !in rules.endsOn && "command" in rules.endsOn)
  }
}

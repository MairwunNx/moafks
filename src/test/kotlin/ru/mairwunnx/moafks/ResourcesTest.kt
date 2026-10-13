package ru.mairwunnx.moafks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourcesTest {
  private fun resource(path: String) = javaClass.classLoader.getResource(path)!!.readText()

  @Test fun `the bundled config matches the defaults`() {
    assertEquals(Settings(), Settings.parse(resource("config.yml")))
  }

  @Test fun `a partial or empty config still loads`() {
    val settings = Settings.parse("debug: true\nprotection: { hunger: false }")
    assertTrue(settings.debug)
    assertFalse(settings.protection.hunger)
    assertTrue(settings.protection.damage)
    assertEquals("en", settings.locales.default)
    assertEquals(Settings(), Settings.parse(""))
  }

  @Test fun `a 1_x config loads, its lists turn into switches, and its messages are ignored`() {
    val settings = Settings.parse(resource("config-1.x.yml"))
    assertEquals(60, settings.auto.seconds)
    assertEquals(1200, settings.limits.seconds)
    assertEquals(listOf(1, 2, 3, 4, 5, 10, 30, 60, 90), settings.limits.warning.seconds)
    assertEquals("minecraft:happy_villager", settings.effects.particles.type)
    assertTrue(settings.effects.nickname.enabled)
    with(settings.protection) {
      assertTrue(damage && projectiles && hunger && mobs && collision)
    }
    with(settings.exitTriggers) {
      assertTrue(move && look && jump && chat && command && attack)
    }
  }

  @Test fun `1_x lists leave out what they don't name`() {
    val settings = Settings.parse("prevents: [hunger, collision]\nexit_triggers: [chat]")
    assertFalse(settings.protection.damage)
    assertFalse(settings.protection.mobs)
    assertTrue(settings.protection.hunger)
    assertFalse(settings.exitTriggers.move)
    assertFalse(settings.exitTriggers.attack)
    assertTrue(settings.exitTriggers.chat)
    // Triggers 1.x didn't have keep their defaults.
    assertTrue(settings.exitTriggers.inventory)
  }

  @Test fun `every bundled language sets every message`() {
    Languages.BUNDLED.forEach { code ->
      val messages = Settings.parseMessages(resource("lang/$code.yml"))
      val missing = Message.entries.map { it.key }.filter { it !in messages }
      assertEquals(emptyList(), missing, "lang/$code.yml misses keys")
      val unknown = messages.keys - Message.entries.map { it.key }.toSet()
      assertEquals(emptySet(), unknown, "lang/$code.yml has keys the plugin doesn't know")
    }
  }
}

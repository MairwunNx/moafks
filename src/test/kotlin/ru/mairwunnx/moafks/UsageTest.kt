package ru.mairwunnx.moafks

import kotlin.test.Test
import kotlin.test.assertEquals

class UsageTest {
  @Test fun `languages and their sources drain to zero`() {
    val usage = Usage()
    usage.language("ru", Usage.Source.IP)
    usage.language("ru", Usage.Source.CHOICE)
    usage.language("en", Usage.Source.DEFAULT)
    assertEquals(mapOf("ru" to 2, "en" to 1), usage.drainLanguages())
    assertEquals(mapOf("ip" to 1, "choice" to 1, "default" to 1), usage.drainSources())
    assertEquals(emptyMap(), usage.drainLanguages())
    assertEquals(emptyMap(), usage.drainSources())
  }

  @Test fun `entries count by kind`() {
    val usage = Usage()
    usage.entered(manual = true)
    usage.entered(manual = false)
    usage.entered(manual = false)
    assertEquals(3, usage.drainEntries())
    assertEquals(mapOf("manual" to 1, "auto" to 2), usage.drainKinds())
    assertEquals(0, usage.drainEntries())
    assertEquals(emptyMap(), usage.drainKinds())
  }

  @Test fun `minutes carry the rest of a minute over`() {
    val usage = Usage()
    usage.afk(90_000)
    assertEquals(1, usage.drainMinutes())
    usage.afk(45_000)
    assertEquals(1, usage.drainMinutes())
    assertEquals(0, usage.drainMinutes())
    usage.afk(-5)
    usage.afk(44_999)
    assertEquals(0, usage.drainMinutes())
    usage.afk(1)
    assertEquals(1, usage.drainMinutes())
  }
}

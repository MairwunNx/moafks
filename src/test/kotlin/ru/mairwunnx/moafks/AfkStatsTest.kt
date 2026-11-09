package ru.mairwunnx.moafks

import java.nio.file.Files
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class AfkStatsTest {
  private val log = Log(Logger.getAnonymousLogger()) { false }
  private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

  @Test fun `a seed keeps the larger totals and spells count by kind`() {
    val stats = AfkStats(Files.createTempDirectory("moafks").resolve("stats.yml").toFile(), log)
    stats.seed(id, "Steve", seconds = 100, spells = 3)
    stats.add(id, "Steve", seconds = 20, manual = true)
    stats.add(id, "Steve", seconds = 5, manual = false)
    stats.seed(id, "Steve2", seconds = 50, spells = 1)
    assertEquals(AfkRecord(name = "Steve2", seconds = 125, spells = 5, manual = 1, auto = 1), stats[id])
  }

  @Test fun `the file round-trips`() {
    val file = Files.createTempDirectory("moafks").resolve("stats.yml").toFile()
    val stats = AfkStats(file, log)
    stats.add(id, "Alex", seconds = 61, manual = true)
    stats.save()
    val again = AfkStats(file, log).apply { load() }
    assertEquals(stats.all(), again.all())
  }

  @Test fun `a broken key is skipped`() {
    assertEquals(mapOf(id to AfkRecord(name = "Alex", seconds = 1)), AfkStats.parse("nope: { name: X }\n$id: { name: Alex, seconds: 1 }"))
  }
}

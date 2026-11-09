package ru.mairwunnx.moafks

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * What bStats sends, counted between two of its sends. Any thread adds; [Bstats]' callbacks drain. Only
 * counts and fixed keys live here: language codes, how a language was picked, and how a player went AFK.
 */
class Usage {
  private val languages = ConcurrentHashMap<String, AtomicInteger>()
  private val sources = ConcurrentHashMap<String, AtomicInteger>()
  private val kinds = ConcurrentHashMap<String, AtomicInteger>()
  private val entries = AtomicInteger()
  private val afkMillis = AtomicLong()

  /** How a player's language was picked, as the `language_source` chart names it; `choice` is `/moafks language`. */
  enum class Source(val key: String) { IP("ip"), CLIENT("client"), DEFAULT("default"), CHOICE("choice") }

  fun language(code: String, source: Source) {
    languages.computeIfAbsent(code) { AtomicInteger() }.incrementAndGet()
    sources.computeIfAbsent(source.key) { AtomicInteger() }.incrementAndGet()
  }

  fun entered(manual: Boolean) {
    entries.incrementAndGet()
    kinds.computeIfAbsent(if (manual) "manual" else "auto") { AtomicInteger() }.incrementAndGet()
  }

  fun afk(millis: Long) {
    if (millis > 0) afkMillis.addAndGet(millis)
  }

  fun drainLanguages() = drain(languages)

  fun drainSources() = drain(sources)

  fun drainKinds() = drain(kinds)

  fun drainEntries() = entries.getAndSet(0)

  /** Whole minutes AFK since the last drain; the rest of a minute waits for the next one. */
  fun drainMinutes(): Int {
    val minutes = afkMillis.get() / MINUTE
    afkMillis.addAndGet(-minutes * MINUTE)
    return minutes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
  }

  private fun drain(counters: ConcurrentHashMap<String, AtomicInteger>): Map<String, Int> =
    counters.entries.associate { (key, count) -> key to count.getAndSet(0) }.filterValues { it > 0 }

  private companion object {
    const val MINUTE = 60_000L
  }
}

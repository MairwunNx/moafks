package ru.mairwunnx.moafks

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * One player's AFK history: finished spells only, the one going on stays in [Afk]. [spells] counts every
 * spell since 2.1.0, [manual] and [auto] only those since 2.3.0, so their sum may be smaller.
 */
@Serializable
data class AfkRecord(
  val name: String = "",
  val seconds: Long = 0,
  val spells: Long = 0,
  val manual: Long = 0,
  val auto: Long = 0,
)

/**
 * Every player's [AfkRecord], kept in `stats.yml` so Plan and WorldAPI know players who are offline,
 * and the server's totals add up across restarts. The map is concurrent: Plan and WorldAPI read it
 * from their own threads, the server thread writes, and saving runs off it.
 */
class AfkStats(private val file: File, private val log: Log) {
  private val records = ConcurrentHashMap<UUID, AfkRecord>()
  @Volatile private var dirty = false
  private val lock = Any()

  operator fun get(id: UUID): AfkRecord? = records[id]

  fun all(): Map<UUID, AfkRecord> = records.toMap()

  /** A joining player: their name, and the totals in their own data when this file knows less, as before 2.3.0. */
  fun seed(id: UUID, name: String, seconds: Long, spells: Long) {
    records.compute(id) { _, old ->
      val record = old ?: AfkRecord()
      record.copy(name = name, seconds = maxOf(record.seconds, seconds), spells = maxOf(record.spells, spells))
    }
    dirty = true
  }

  /** A finished spell; returns the player's record with it. */
  fun add(id: UUID, name: String, seconds: Long, manual: Boolean): AfkRecord {
    val record = records.compute(id) { _, old ->
      val record = old ?: AfkRecord()
      record.copy(
        name = name,
        seconds = record.seconds + seconds,
        spells = record.spells + 1,
        manual = record.manual + if (manual) 1 else 0,
        auto = record.auto + if (manual) 0 else 1,
      )
    }!!
    dirty = true
    return record
  }

  fun load() {
    if (!file.isFile) return
    runCatching { parse(file.readText()) }
      .onSuccess { records.clear(); records.putAll(it) }
      .onFailure { log.error("${file.name} has an error; AFK totals start over from players' own data: ${it.message}") }
  }

  /** Writes the file when something changed: to a temporary file first, so a crash can't leave half of it. */
  fun save() {
    if (!dirty) return
    synchronized(lock) {
      dirty = false
      runCatching {
        file.parentFile.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(format(records.toMap()))
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      }.onFailure {
        dirty = true
        log.warn("Can't save ${file.name}: ${it.message}")
      }
    }
  }

  companion object {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private val serializer = MapSerializer(String.serializer(), AfkRecord.serializer())

    fun parse(text: String): Map<UUID, AfkRecord> =
      if (text.isBlank()) emptyMap()
      else yaml.decodeFromString(serializer, text).mapNotNull { (key, record) -> runCatching { UUID.fromString(key) }.getOrNull()?.let { it to record } }.toMap()

    fun format(records: Map<UUID, AfkRecord>): String =
      "# Mo’Afks keeps every player's AFK totals here. Don't edit it while the server runs.\n" +
        yaml.encodeToString(serializer, records.mapKeys { it.key.toString() }.toSortedMap())
  }
}

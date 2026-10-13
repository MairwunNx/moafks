package ru.mairwunnx.moafks

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * `config.yml`. Every field has a default, so a config from 1.x, or one that sets only a few keys,
 * still loads. Game ids (sounds, particles) stay strings here and resolve against the server's
 * registries in [AfkEffects], where a bad id turns into a warning.
 */
@Serializable
data class Settings(
  val enabled: Boolean = true,
  val debug: Boolean = false,
  val locales: Locales = Locales(),
  val geoip: GeoIpSettings = GeoIpSettings(),
  val auto: Auto = Auto(),
  val manual: Manual = Manual(),
  val limits: Limits = Limits(),
  /** AFK players don't count when the server checks whether everyone sleeps. */
  @SerialName("sleep_ignored") val sleepIgnored: Boolean = true,
  val protection: Protection = Protection(),
  @SerialName("anti_abuse") val antiAbuse: AntiAbuse = AntiAbuse(),
  val gains: Gains = Gains(),
  @SerialName("exit_triggers") val exitTriggers: ExitTriggers = ExitTriggers(),
  val sounds: Sounds = Sounds(),
  val effects: Effects = Effects(),
) {
  @Serializable
  data class Locales(
    /** The language for players nothing else matches, and for the console. */
    val default: String = "en",
    /** Where a player's language comes from, tried in order: `ip` (country by address), `client` (game language). */
    val sources: List<Source> = listOf(Source.IP, Source.CLIENT),
    /** Language → the ISO 3166 country codes that speak it. Countries not listed fall through to the next source. */
    val countries: Map<String, List<String>> = DEFAULT_COUNTRIES,
  )

  @Serializable
  enum class Source {
    @SerialName("ip") IP,
    @SerialName("client") CLIENT,
  }

  @Serializable
  data class GeoIpSettings(
    val enabled: Boolean = true,
    /** A MaxMind-format country database, plain or gzipped. `{year}` and `{month}` fill in the current month. */
    val url: String = "https://download.db-ip.com/free/dbip-country-lite-{year}-{month}.mmdb.gz",
    @SerialName("refresh_days") val refreshDays: Int = 30,
  )

  @Serializable
  data class Auto(
    val enabled: Boolean = true,
    /** Seconds without activity before a player goes AFK. */
    val seconds: Int = 300,
  )

  @Serializable
  data class Manual(
    val enabled: Boolean = true,
    /** Longer reasons get cut. */
    @SerialName("reason_length") val reasonLength: Int = 64,
  )

  @Serializable
  data class Limits(
    val enabled: Boolean = true,
    /** Seconds a player may stay AFK before the kick. */
    val seconds: Int = 1200,
    val warning: Warning = Warning(),
  )

  @Serializable
  data class Warning(
    val enabled: Boolean = true,
    /** Seconds before the kick at which the action bar warns. */
    val seconds: List<Int> = listOf(60, 30, 10, 5, 4, 3, 2, 1),
  )

  @Serializable
  data class Protection(
    /** Every kind of damage but /kill, plus drowning: air doesn't run out. */
    val damage: Boolean = true,
    /** Falling into the void takes the player back to where they went AFK. */
    val void: Boolean = true,
    /** Hits, explosions, and wind charges don't push them. */
    val knockback: Boolean = true,
    /** Mobs and other players walk through them. */
    val collision: Boolean = true,
    /** Arrows, tridents, snowballs, eggs, ender pearls, and fishing hooks fly through them; rods can't pull them. */
    val projectiles: Boolean = true,
    /** Harmful potion effects don't stick, except from commands. */
    val effects: Boolean = true,
    /** Hunger stays where it was, and so does health: no drain, no natural healing. */
    val hunger: Boolean = true,
    /** Mobs don't target them, wardens and endermen don't get angry at them, phantoms don't spawn for them. */
    val mobs: Boolean = true,
    /** Boats and minecarts can't pick them up or push them. */
    val vehicles: Boolean = true,
    /** Bad Omen doesn't start a raid while they stand in a village. */
    val raids: Boolean = true,
    /** Sculk sensors, shriekers, and wardens don't hear them. */
    val vibrations: Boolean = true,
  )

  @Serializable
  data class AntiAbuse(
    /** No AFK within this many seconds of taking damage or hitting a mob or player. */
    @SerialName("combat_seconds") val combatSeconds: Int = 15,
    /** No AFK while falling or gliding. */
    val falling: Boolean = true,
    /** No AFK while on fire or freezing. */
    val burning: Boolean = true,
    /** No AFK in water or lava. */
    val liquids: Boolean = true,
    /** No AFK while a mob nearby is after the player. */
    val targeted: Boolean = true,
  )

  @Serializable
  data class Gains(
    /** Whether AFK players pick up items and arrows. */
    val items: Boolean = false,
    /** Whether AFK players pick up experience orbs. */
    val experience: Boolean = false,
  )

  /** What counts as activity: each one ends AFK and restarts the idle timer. */
  @Serializable
  data class ExitTriggers(
    /** Walking: the movement keys. Being pushed by water, pistons, or a minecart doesn't count. */
    val move: Boolean = true,
    /** Turning the camera by more than [lookDegrees]. */
    val look: Boolean = true,
    @SerialName("look_degrees") val lookDegrees: Float = 30f,
    val jump: Boolean = true,
    val sneak: Boolean = true,
    val sprint: Boolean = true,
    val chat: Boolean = true,
    /** Any command but /afk and /moafks. */
    val command: Boolean = true,
    /** Hitting a mob or a player. */
    val attack: Boolean = true,
    /** Clicking a block, an entity, or the air. */
    val interact: Boolean = true,
    /** Breaking or placing a block. */
    val blocks: Boolean = true,
    /** Opening a container or clicking in an inventory. */
    val inventory: Boolean = true,
    val drop: Boolean = true,
    @SerialName("swap_hands") val swapHands: Boolean = true,
    @SerialName("held_slot") val heldSlot: Boolean = true,
  )

  @Serializable
  data class Sounds(
    val enter: SoundSpec = SoundSpec(key = "minecraft:block.amethyst_block.chime"),
    val exit: SoundSpec = SoundSpec(key = "minecraft:entity.experience_orb.pickup"),
    val declined: SoundSpec = SoundSpec(key = "minecraft:entity.villager.no"),
    val warning: SoundSpec = SoundSpec(key = "minecraft:block.note_block.pling"),
  )

  @Serializable
  data class SoundSpec(
    val enabled: Boolean = true,
    val key: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
  )

  @Serializable
  data class Effects(
    val particles: Particles = Particles(),
    val nickname: Nickname = Nickname(),
  )

  @Serializable
  data class Particles(
    val enabled: Boolean = true,
    val type: String = "minecraft:happy_villager",
    val radius: Double = 1.5,
    val points: Int = 8,
  )

  @Serializable
  data class Nickname(
    /** Puts [mark] in front of the name in the player list. */
    val enabled: Boolean = true,
    /** MiniMessage. PlaceholderAPI's `%moafks_tag%` shows it too. */
    val mark: String = "<gray><italic>[AFK]</italic></gray> ",
  )

  companion object {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    fun parse(text: String): Settings {
      if (text.isBlank()) return Settings()
      val root = yaml.parseToYamlNode(text) as? YamlMap ?: return yaml.decodeFromString(serializer(), text)
      // 1.x kept lists here: `exit_triggers: [combat, move]` and `prevents: [fire, hunger]`.
      val oldTriggers = root.get<YamlNode>("exit_triggers")?.let { it as? YamlList }?.let(::strings)
      val oldPrevents = root.get<YamlNode>("prevents")?.let { it as? YamlList }?.let(::strings)
      val rest = if (oldTriggers == null) root else YamlMap(root.entries.filterKeys { it.content != "exit_triggers" }, root.path)
      return yaml.decodeFromYamlNode(serializer(), rest).migrate(oldPrevents, oldTriggers)
    }

    private fun strings(list: YamlList) = list.items.mapNotNull { (it as? YamlScalar)?.content?.trim()?.lowercase() }

    /** A language file: message key → MiniMessage text. */
    fun parseMessages(text: String): Map<String, String> =
      if (text.isBlank()) emptyMap() else yaml.decodeFromString(MapSerializer(String.serializer(), String.serializer()), text)

    val DEFAULT_COUNTRIES = mapOf(
      "ru" to listOf("RU", "BY", "KZ", "KG", "TJ", "UZ", "AM", "AZ", "MD", "GE", "TM"),
      "es" to listOf(
        "ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN",
        "PY", "SV", "NI", "CR", "PA", "UY", "PR", "GQ",
      ),
      "pt" to listOf("BR", "PT", "AO", "MZ", "CV", "GW", "ST", "TL"),
      "de" to listOf("DE", "AT", "CH", "LI", "LU"),
    )
  }
}

/** Turns 1.x's `prevents` and `exit_triggers` lists into the switches that replaced them. */
internal fun Settings.migrate(prevents: List<String>?, triggers: List<String>?): Settings {
  var settings = this
  if (prevents != null) {
    val damage = listOf("explosions", "fire", "entity_damage", "projectile_damage", "fall").any { it in prevents }
    settings = settings.copy(
      protection = settings.protection.copy(
        damage = damage,
        projectiles = "projectile_damage" in prevents,
        hunger = "hunger" in prevents,
        mobs = "mob_target" in prevents,
        collision = "collision" in prevents,
      ),
    )
  }
  if (triggers != null) {
    // 1.x's `move` meant any change of position; now it splits into the keys a player presses.
    val moving = "move" in triggers
    settings = settings.copy(
      exitTriggers = settings.exitTriggers.copy(
        move = moving, look = moving, jump = moving, sneak = moving, sprint = moving,
        chat = "chat" in triggers,
        command = "command" in triggers,
        attack = "combat" in triggers,
      ),
    )
  }
  return settings
}

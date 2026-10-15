package ru.mairwunnx.moafks

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File

class MoAfksPlugin : JavaPlugin() {
  @Volatile var settings = Settings(); private set
  @Volatile lateinit var languages: Languages; private set
  @Volatile lateinit var resolver: LocaleResolver; private set
  @Volatile lateinit var effects: AfkEffects; private set

  val log = Log(logger) { settings.debug }
  val geoIp by lazy { GeoIp(dataFolder.resolve("geoip"), log) }
  val messenger = Messenger(this)
  val vanish = Vanish()
  lateinit var afk: Afk; private set
  lateinit var sessions: SessionListener; private set

  private var geoIpTask: BukkitTask? = null
  private val tasks = mutableListOf<BukkitTask>()
  private var expansion: AutoCloseable? = null
  private var miniExpansion: AutoCloseable? = null
  var analytics: Analytics? = null; private set

  override fun onEnable() {
    if (!reload()) log.warn("Started with the default settings; fix config.yml and run /moafks reload")
    afk = Afk(this)
    sessions = SessionListener(this)
    removeOldTeam()

    server.pluginManager.registerEvents(sessions, this)
    server.pluginManager.registerEvents(ActivityListener(this), this)
    server.pluginManager.registerEvents(ProtectionListener(this), this)
    registerCommand("afk", "Go AFK, or come back", AfkCommand(this))
    registerCommand("moafks", "Reload Mo’Afks, list AFK players, or check a player’s language", MoAfksCommand(this))

    afk.start()
    tasks += server.scheduler.runTaskTimer(this, Runnable { afk.tick() }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { afk.particles() }, 40L, 40L)

    if (Placeholders.available) {
      expansion = runCatching { Placeholders.register(this) }.onFailure { log.warn("Can't register %moafks_…% with PlaceholderAPI: ${it.message}") }.getOrNull()
      if (expansion != null) log.info("PlaceholderAPI found: <papi:…> tags work in messages, %moafks_…% works elsewhere")
    }
    if (AdventurePlaceholders.available) {
      miniExpansion = runCatching { AdventurePlaceholders.register(this) }.onFailure { log.warn("Can't register <moafks_…> with MiniPlaceholders: ${it.message}") }.getOrNull()
      if (miniExpansion != null) log.info("MiniPlaceholders found: its tags work in messages, <moafks_…> works elsewhere")
    }
    if (PlanAnalytics.available) {
      analytics = runCatching { PlanAnalytics.register(this) }.onFailure { log.warn("Can't show AFK time in Plan: ${it.message}") }.getOrNull()
      if (analytics != null) log.info("Plan found: AFK time shows on its pages")
    }
    log.info("Mo’Afks ${pluginMeta.version} is on: ${languages.codes.sorted().joinToString()}, default ${resolver.default}")
  }

  override fun onDisable() {
    tasks.forEach { it.cancel() }
    tasks.clear()
    geoIpTask?.cancel()
    // Before the quiet exits below: Plan may already be stopping.
    analytics?.close()
    analytics = null
    if (::afk.isInitialized) afk.shutdown()
    expansion?.close()
    miniExpansion?.close()
    geoIp.close()
  }

  /**
   * Reads config.yml and the language files again; players stay AFK under the new settings. On a
   * broken file, keeps what it can and returns false. Commands and listeners stay registered.
   */
  fun reload(): Boolean {
    var ok = true
    settings = runCatching { Settings.parse(file("config.yml").readText()) }
      .onFailure { ok = false; log.error("config.yml has an error, using the defaults: ${it.message}") }
      .getOrDefault(Settings())

    val texts = loadLanguages { ok = false }
    languages = Languages(texts, LocaleResolver.normalize(settings.locales.default))
    resolver = LocaleResolver(settings.locales, texts.keys)
    effects = AfkEffects(settings, log)
    if (LocaleResolver.normalize(settings.locales.default) !in texts) log.warn("locales.default: no lang/${settings.locales.default}.yml, using ${resolver.default}")

    if (::afk.isInitialized) afk.reapply()
    setUpGeoIp()
    return ok
  }

  /** The bundled languages, overlaid key by key with the files in `lang/`, plus any extra languages found there. */
  private fun loadLanguages(onError: () -> Unit): Map<String, Map<String, String>> {
    val folder = dataFolder.resolve("lang")
    Languages.BUNDLED.forEach { file("lang/$it.yml") }

    val bundled = Languages.BUNDLED.associateWith { code ->
      getResource("lang/$code.yml")!!.use { Settings.parseMessages(it.readBytes().toString(Charsets.UTF_8)) }
    }
    val custom = folder.listFiles { f -> f.isFile && f.extension == "yml" }.orEmpty().associate { f ->
      LocaleResolver.normalize(f.nameWithoutExtension) to runCatching { Settings.parseMessages(f.readText()) }
        .onFailure { onError(); log.error("lang/${f.name} has an error, skipping it: ${it.message}") }
        .getOrDefault(emptyMap())
    }
    return (bundled.keys + custom.keys).associateWith { bundled[it].orEmpty() + custom[it].orEmpty() }
  }

  private fun setUpGeoIp() {
    geoIpTask?.cancel()
    val config = settings.geoip
    if (!config.enabled || Settings.Source.IP !in settings.locales.sources) {
      geoIp.close()
      return
    }
    geoIp.open()
    // Checks twice a day; downloads only when the file on disk has aged past refresh_days.
    geoIpTask = server.scheduler.runTaskTimerAsynchronously(this, Runnable { if (geoIp.isStale(config)) geoIp.refresh(config) }, 0L, 20L * 60 * 60 * 12)
  }

  /** 1.x put AFK players into a scoreboard team, which outlived a crash in the world save. */
  private fun removeOldTeam() {
    server.scoreboardManager.mainScoreboard.getTeam("moafks_noclip")?.let {
      it.unregister()
      log.info("Removed the moafks_noclip team left by Mo’Afks 1.x")
    }
  }

  /** A file in the data folder, copied from the jar first when it isn't there yet. */
  private fun file(path: String): File {
    val file = dataFolder.resolve(path)
    if (!file.exists()) saveResource(path, false)
    return file
  }
}

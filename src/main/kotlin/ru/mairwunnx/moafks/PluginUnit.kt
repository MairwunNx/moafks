package ru.mairwunnx.moafks

import kotlinx.coroutines.runBlocking
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import ru.mairwunnx.moafks.managers.AwayFromKeyboardManager
import ru.mairwunnx.moafks.managers.CommandManager
import ru.mairwunnx.moafks.managers.ConfigurationManager
import ru.mairwunnx.moafks.managers.EffectsManager
import ru.mairwunnx.moafks.managers.LocationManager
import ru.mairwunnx.moafks.managers.PlayerEventManager
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import ru.mairwunnx.moafks.platform.PaperLogger
import ru.mairwunnx.moafks.platform.PaperScheduler

class PluginUnit : JavaPlugin() {
  lateinit var logger: PaperLogger private set

  lateinit var scheduler: PaperScheduler private set

  lateinit var configuration: ConfigurationManager private set
  val isConfigurationDefined get() = ::configuration.isInitialized

  lateinit var commands: CommandManager private set
  lateinit var afk: AwayFromKeyboardManager private set
  lateinit var effects: EffectsManager private set
  lateinit var players: PlayerEventManager private set
  lateinit var location: LocationManager private set

  override fun onEnable() {
    logger = PaperLogger(this)
    scheduler = PaperScheduler(this)

    logger.info { "🔄 Loading Mo'Afks plugin" }

    configuration = ConfigurationManager(this)
    runBlocking { configuration.initialize() }

    if (!configuration[GeneralConfigurationModel::class.java].enabled) {
      logger.info { "⛔️ Mo'Afks is disabled in the config. Plugin will not be enabled." }
      onDisable()
      return
    }

    commands = CommandManager(this)
    afk = AwayFromKeyboardManager(this)
    effects = EffectsManager(this)
    players = PlayerEventManager(this)
    location = LocationManager(this)

    server.pluginManager.registerEvents(players, this)

    logger.info { "✅ Plugin Mo'Afks loaded" }
  }

  override fun onDisable() {
    HandlerList.unregisterAll(this)

    if (::scheduler.isInitialized) scheduler.close()
    if (::configuration.isInitialized) configuration.close()
    if (::commands.isInitialized) commands.close()
    if (::afk.isInitialized) afk.close()
    if (::effects.isInitialized) effects.close()
    if (::players.isInitialized) players.close()
    if (::location.isInitialized) location.close()

    logger.info { "✅ Plugin Mo'Afks unloaded" }
  }

  fun reload() {
    logger.info { "🔄 Reloading Mo'Afks plugin" }

    if (::scheduler.isInitialized) scheduler.close()
    if (::configuration.isInitialized) configuration.close()
    if (::afk.isInitialized) afk.close()
    if (::effects.isInitialized) effects.close()
    if (::players.isInitialized) players.close()
    if (::location.isInitialized) location.close()

    scheduler = PaperScheduler(this)

    configuration = ConfigurationManager(this)
    runBlocking { configuration.initialize() }

    if (!configuration[GeneralConfigurationModel::class.java].enabled) {
      logger.info { "⛔️ Mo'Afks is disabled in the config. Plugin will not be enabled." }
      onDisable()
      return
    }

    afk = AwayFromKeyboardManager(this)
    effects = EffectsManager(this)
    players = PlayerEventManager(this)
    location = LocationManager(this)

    logger.info { "✅ Plugin Mo'Afks reloaded" }
  }
}

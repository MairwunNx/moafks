package ru.mairwunnx.moafks.managers

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.Sound
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import ru.mairwunnx.moafks.serializers.ComponentSerializer
import ru.mairwunnx.moafks.serializers.ParticleSerializer
import ru.mairwunnx.moafks.serializers.SoundSerializer
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.reflect.KClass

class ConfigurationManager(val plugin: PluginUnit) : Closeable {
  private val serializersModule = SerializersModule {
    contextual(Particle::class, ParticleSerializer)
    contextual(Sound::class, SoundSerializer)
    contextual(Component::class, ComponentSerializer)
  }

  private val yaml = Yaml(
    serializersModule = serializersModule,
    configuration = YamlConfiguration(encodeDefaults = true, allowAnchorsAndAliases = true, strictMode = false)
  )

  private val files = listOf<ConfigurationFile<*>>(
    ConfigurationFile(
      "config.yml",
      GeneralConfigurationModel.serializer(),
      GeneralConfigurationModel.default(),
      GeneralConfigurationModel::class
    ),
  )

  private val configurations = ConcurrentHashMap<KClass<*>, Any>()

  private inner class ConfigurationFile<T : Any>(val path: String, val type: KSerializer<T>, val default: T, val kClass: KClass<T>) {
    suspend fun resolve(): T = withContext(IO) {
      plugin.logger.info { "🔄 Resolving configuration file: $path" }

      val result = runCatching {
        val resolvedFile = plugin.dataFolder.resolve(path)
        if (!resolvedFile.exists()) {
          plugin.logger.warn { "$path configuration file not found, creating new one" }
          plugin.saveResource(path, false)
        }
        val text = resolvedFile.readText(Charsets.UTF_8)
        yaml.decodeFromString(type, text)
      }.onFailure {
        plugin.logger.error({ "Can't load or read $path configuration file! Using default one." }, it)
      }.getOrDefault(default)

      plugin.logger.info { "✅ $path configuration file loaded" }
      result
    }
  }

  suspend fun initialize() = supervisorScope {
    plugin.logger.info { "🔄 Loading configuration files" }
    val loaded = files.map { async(IO) { it.kClass to it.resolve() } }.awaitAll()
    loaded.forEach { (k, v) -> configurations[k] = v }
    plugin.logger.info { "✅ Configuration files loaded successfully" }
  }

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(type: Class<T>): T =
    configurations[type.kotlin] as? T
      ?: run {
        plugin.logger.error(
          { "No configuration ${type.simpleName} resolved at this moment!" },
          kotlin.NullPointerException()
        )
        throw kotlin.IllegalStateException("Configuration ${type.simpleName} not loaded")
      }

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(type: KClass<T>): T =
    configurations[type] as? T
      ?: run {
        plugin.logger.error(
          { "No configuration ${type.simpleName} resolved at this moment!" },
          kotlin.NullPointerException()
        )
        throw kotlin.IllegalStateException("Configuration ${type.simpleName} not loaded")
      }

  override fun close() {
    configurations.clear()
  }
}

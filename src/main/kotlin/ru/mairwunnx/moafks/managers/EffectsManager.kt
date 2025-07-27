package ru.mairwunnx.moafks.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import ru.mairwunnx.moafks.PluginUnit
import ru.mairwunnx.moafks.models.GeneralConfigurationModel
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

class EffectsManager(private val plugin: PluginUnit) : Closeable {
  private val originalDisplayNames = ConcurrentHashMap<UUID, Component>()
  private val originalListNames = ConcurrentHashMap<UUID, Component>()

  private val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
  private val noClipTeam: Team = scoreboard.getTeam("moafks_noclip")
    ?: scoreboard.registerNewTeam("moafks_noclip").apply {
      setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
      setCanSeeFriendlyInvisibles(false)
    }

  private var particlesTaskStarted = false

  fun applyAfkVisuals(player: Player) {
    val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
    plugin.logger.debug { "Applying AFK visuals for ${player.name}" }

    if (cfg.effects.nickname.enabled) applyAfkNickname(player)
    if (cfg.effects.particles.enabled) ensureParticlesTaskRunning()
    plugin.logger.debug { "Applied AFK visuals for ${player.name}" }
  }

  fun removeAfkVisuals(player: Player) {
    plugin.logger.debug { "Removing AFK visuals for ${player.name}" }
    restoreNickname(player)
    plugin.logger.debug { "Removed AFK visuals for ${player.name}" }
  }

  fun setCollisionDisabled(player: Player, disabled: Boolean) {
    plugin.logger.debug { "Setting collision for ${player.name}: disabled=$disabled" }

    // Пробуем прямое API (если доступно)
    val directSetOk = runCatching {
      player.isCollidable = !disabled
      true
    }.getOrElse { false }

    if (!directSetOk) {
      // Fallback на командную таблицу
      val entry = player.name // team entries — строковые ники
      if (disabled) {
        if (!noClipTeam.hasEntry(entry)) noClipTeam.addEntry(entry)
      } else {
        if (noClipTeam.hasEntry(entry)) noClipTeam.removeEntry(entry)
      }
    }
  }

  private fun applyAfkNickname(player: Player) {
    val uuid = player.uniqueId
    // Сохраняем исходные значения ОДИН раз
    originalDisplayNames.putIfAbsent(uuid, player.displayName())
    originalListNames.putIfAbsent(uuid, player.playerListName())

    val prefix = Component.text("[AFK] ", NamedTextColor.GRAY)
      .decoration(TextDecoration.ITALIC, true)
    val newDisplay = prefix.append(player.displayName())

    player.displayName(newDisplay)
    player.playerListName(newDisplay)
  }

  private fun restoreNickname(player: Player) {
    val uuid = player.uniqueId
    originalDisplayNames.remove(uuid)?.let { player.displayName(it) }
    originalListNames.remove(uuid)?.let { player.playerListName(it) }
  }

  /** Запустить общий таск для частиц при первой необходимости */
  private fun ensureParticlesTaskRunning() {
    if (particlesTaskStarted) return
    particlesTaskStarted = true

    plugin.scheduler.runRepeating(
      name = "effects-particles",
      delayTicks = 40L,
      periodTicks = 40L // каждые 2 секунды
    ) {
      val cfg = plugin.configuration[GeneralConfigurationModel::class.java]
      if (!cfg.effects.particles.enabled) return@runRepeating

      val angles = ANGLES

      Bukkit.getOnlinePlayers().forEach { p ->
        if (!plugin.afk.isAfk(p)) return@forEach

        val base: Location = p.location.clone().add(0.0, 1.0, 0.0) // над головой
        val w = base.world ?: return@forEach

        // 8 точек кольца
        for (i in 0 until angles.size) {
          val a = angles[i]
          val x = base.x + cos(a) * 1.5
          val z = base.z + sin(a) * 1.5
          val spot = base.clone().apply { this.x = x; this.z = z }

          w.spawnParticle(
            cfg.effects.particles.type,
            spot,
            1, // count
            0.05, 0.05, 0.05, // offset
            0.0 // speed
          )
        }
      }
    }
  }

  override fun close() {
    plugin.logger.debug { "Closing EffectsManager" }

    // Снять визульки и коллизии у всех онлайн
    Bukkit.getOnlinePlayers().forEach { p ->
      restoreNickname(p)
      setCollisionDisabled(p, false)
    }

    originalDisplayNames.clear()
    originalListNames.clear()

    // Команду можно не удалять, но раз это наш ресурс — подчистим
    runCatching { noClipTeam.unregister() }
      .onFailure { plugin.logger.debug { "Unable to unregister team: ${it.message}" } }
  }

  private companion object {
    // Предрасчитанные 8 углов (45° шаг)
    val ANGLES = DoubleArray(8) { i -> Math.toRadians((i * 45).toDouble()) }
  }
}
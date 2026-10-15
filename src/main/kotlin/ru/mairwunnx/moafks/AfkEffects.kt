package ru.mairwunnx.moafks

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sounds, the particle ring, and the name mark. Built from [Settings] on every load: ids resolve
 * against the server's registries once, and a bad one logs a warning and drops out instead of
 * failing every time someone goes AFK.
 */
class AfkEffects(settings: Settings, private val log: Log) {
  val enter = settings.sounds.enter.toSound("sounds.enter")
  val exit = settings.sounds.exit.toSound("sounds.exit")
  val declined = settings.sounds.declined.toSound("sounds.declined")
  val warning = settings.sounds.warning.toSound("sounds.warning")

  private val particle: Particle? = settings.effects.particles.takeIf { it.enabled }?.let { spec ->
    val key = NamespacedKey.fromString(spec.type.trim().lowercase())
    val particle = key?.let { RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE).get(it) }
      ?: return@let null.also { log.warn("effects.particles.type: unknown id \"${spec.type}\"") }
    // Particles like dust need extra data this ring doesn't have.
    particle.takeIf { it.dataType == Void::class.java }
      ?: null.also { log.warn("effects.particles.type: ${spec.type} needs extra data; pick another particle") }
  }
  private val radius = settings.effects.particles.radius
  private val points = settings.effects.particles.points.coerceIn(3, 64)

  /** The mark put in front of an AFK player's name in the player list, or null when that's off. */
  val mark: Component? = settings.effects.nickname.takeIf { it.enabled }?.let {
    runCatching { MiniMessage.miniMessage().deserialize(it.mark) }
      .onFailure { e -> log.warn("effects.nickname.mark: ${e.message}") }
      .getOrNull()
  }

  fun play(player: Player, sound: Sound?) {
    sound?.let { player.playSound(it, Sound.Emitter.self()) }
  }

  /** The particle ring around an AFK player; [onlyFor] shows it to them alone, as for a vanished player. */
  fun ring(player: Player, onlyFor: Boolean = false) {
    val particle = particle ?: return
    val center = player.location.add(0.0, 1.0, 0.0)
    for (i in 0 until points) {
      val angle = 2 * PI * i / points
      val at = center.clone().add(cos(angle) * radius, 0.0, sin(angle) * radius)
      if (onlyFor) player.spawnParticle(particle, at, 1, 0.05, 0.05, 0.05, 0.0)
      else player.world.spawnParticle(particle, at, 1, 0.05, 0.05, 0.05, 0.0)
    }
  }

  private fun Settings.SoundSpec.toSound(path: String): Sound? {
    if (!enabled) return null
    val key = runCatching { Key.key(key.trim().lowercase()) }.getOrNull()
      ?: return null.also { log.warn("$path.key: \"$key\" isn't a valid sound id") }
    return Sound.sound(key, Sound.Source.MASTER, volume, pitch)
  }
}

package ru.mairwunnx.moafks.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry

object ParticleSerializer : KSerializer<Particle> {
  override val descriptor = PrimitiveSerialDescriptor("Particle", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Particle) {
    val key: NamespacedKey? = runCatching { Registry.PARTICLE_TYPE.getKey(value) }.getOrNull()
    val out = key?.asString() ?: runCatching { value.toString() }.getOrDefault("HAPPY_VILLAGER")
    encoder.encodeString(out)
  }

  override fun deserialize(decoder: Decoder): Particle {
    val raw = decoder.decodeString()
    val key = NamespacedKey.fromString(raw) ?: NamespacedKey.minecraft(raw.lowercase())
    return Registry.PARTICLE_TYPE.get(key) ?: Particle.FLAME
  }
}
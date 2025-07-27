package ru.mairwunnx.moafks.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound

object SoundSerializer : KSerializer<Sound> {
  override val descriptor = PrimitiveSerialDescriptor("Sound", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Sound) {
    val key: NamespacedKey? = runCatching { Registry.SOUNDS.getKey(value) }.getOrNull()
    val out = key?.asString() ?: runCatching { value.toString() }.getOrDefault("ENTITY_GENERIC_HURT")
    encoder.encodeString(out)
  }

  override fun deserialize(decoder: Decoder): Sound {
    val raw = decoder.decodeString()
    val key = NamespacedKey.fromString(raw)
    return if (key != null) {
      Registry.SOUNDS.get(key) ?: Sound.ENTITY_GENERIC_HURT
    } else {
      val fallback = NamespacedKey.minecraft(raw.lowercase())
      Registry.SOUNDS.get(fallback) ?: Sound.ENTITY_GENERIC_HURT
    }
  }
}
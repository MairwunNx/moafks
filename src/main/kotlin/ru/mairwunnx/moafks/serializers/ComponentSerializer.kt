package ru.mairwunnx.moafks.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.Component
import ru.mairwunnx.moafks.platform.PaperAudience.mm

object ComponentSerializer : KSerializer<Component> {
  override val descriptor = PrimitiveSerialDescriptor("Component", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Component) = encoder.encodeString(mm.serialize(value))

  override fun deserialize(decoder: Decoder): Component {
    val raw = decoder.decodeString()
    return runCatching { mm.deserialize(raw) }.getOrElse { Component.empty() }
  }
}

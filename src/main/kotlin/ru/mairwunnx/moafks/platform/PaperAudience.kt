package ru.mairwunnx.moafks.platform

import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object PaperAudience {
  val mm = MiniMessage.builder().tags(TagResolver.standard()).build()
}
package ru.mairwunnx.moafks

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleResolverTest {
  private val available = setOf("en", "ru", "es", "pt_br")
  private val resolver = LocaleResolver(Settings.Locales(), available)

  @Test fun `country wins over the game language`() {
    assertEquals("ru", resolver.resolve("RU", Locale.forLanguageTag("es-ES")))
    assertEquals("es", resolver.resolve("mx", Locale.US))
  }

  @Test fun `an unlisted country falls through to the game language`() {
    assertEquals("es", resolver.resolve("DE", Locale.forLanguageTag("es-AR")))
    assertEquals("pt_br", resolver.resolve(null, Locale.forLanguageTag("pt-BR")))
  }

  @Test fun `nothing matching gives the default`() {
    assertEquals("en", resolver.resolve("DE", Locale.GERMANY))
    assertEquals("en", resolver.resolve(null, null))
  }

  @Test fun `sources follow the configured order`() {
    val clientFirst = LocaleResolver(Settings.Locales(sources = listOf(Settings.Source.CLIENT, Settings.Source.IP)), available)
    assertEquals("es", clientFirst.resolve("RU", Locale.forLanguageTag("es")))
    assertEquals("ru", clientFirst.resolve("RU", Locale.GERMANY))
  }

  @Test fun `a missing default language falls back to English`() {
    assertEquals("en", LocaleResolver(Settings.Locales(default = "de"), available).default)
  }

  @Test fun `the player's choice beats every rule`() {
    assertEquals("es", resolver.resolve("RU", Locale.forLanguageTag("ru"), chosen = "es"))
    assertEquals("pt_br", resolver.resolve(null, null, chosen = "PT-BR"))
  }

  @Test fun `a choice without its file falls back to the rules`() {
    assertEquals("ru", resolver.resolve("RU", null, chosen = "de"))
  }

  @Test fun `a typed code matches the file or its language`() {
    assertEquals("ru", resolver.match("RU"))
    assertEquals("pt_br", resolver.match("pt-BR"))
    assertEquals("es", resolver.match("es_MX"))
    assertEquals(null, resolver.match("de"))
  }

  @Test fun `bundled languages come first, by their own names`() {
    val languages = Languages(mapOf("de" to emptyMap(), "uk" to emptyMap(), "en" to emptyMap(), "ru" to emptyMap()), "en")
    assertEquals(listOf("en", "ru", "de", "uk"), languages.ordered())
    assertEquals("Русский", Languages.name("ru"))
    assertEquals("uk", Languages.name("uk"))
  }
}

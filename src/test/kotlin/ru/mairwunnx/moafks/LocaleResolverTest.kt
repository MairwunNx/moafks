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
}

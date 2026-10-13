package ru.mairwunnx.moafks

import java.util.logging.Level
import java.util.logging.Logger

/** The plugin's logger; debug lines show only with `debug: true`. */
class Log(private val logger: Logger, private val debugEnabled: () -> Boolean) {
  fun info(message: String) = logger.info(message)
  fun warn(message: String) = logger.warning(message)
  fun error(message: String, cause: Throwable? = null) = logger.log(Level.SEVERE, message, cause)

  inline fun debug(message: () -> String) {
    if (isDebug()) info("[debug] ${message()}")
  }

  fun isDebug() = debugEnabled()
}

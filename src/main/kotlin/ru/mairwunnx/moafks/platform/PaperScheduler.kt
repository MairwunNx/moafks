package ru.mairwunnx.moafks.platform

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

class PaperScheduler(private val plugin: Plugin) : Closeable {
  private val tasks = ConcurrentHashMap<String, Int>()

  /** Запускает повторяющуюся задачу (main-поток). Возвращает id. */
  fun runRepeating(name: String, delayTicks: Long, periodTicks: Long, task: () -> Unit): Int {
    cancel(name)
    val id = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { task() }, delayTicks, periodTicks).taskId
    tasks[name] = id
    return id
  }

  /** Одноразовая задача (main-поток). */
  fun runLater(name: String, delayTicks: Long, task: () -> Unit): Int {
    cancel(name)
    val id = Bukkit.getScheduler().runTaskLater(plugin, Runnable { task() }, delayTicks).taskId
    tasks[name] = id
    return id
  }

  /** Асинхронная задача. */
  fun runAsync(name: String, task: () -> Unit): Int {
    cancel(name)
    val id = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { task() }).taskId
    tasks[name] = id
    return id
  }

  /** Отменить задачу по имени. */
  fun cancel(name: String) {
    tasks.remove(name)?.let { Bukkit.getScheduler().cancelTask(it) }
  }

  /** Отменить всё. */
  override fun close() {
    tasks.values.forEach { Bukkit.getScheduler().cancelTask(it) }
    tasks.clear()
  }
}
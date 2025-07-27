@file:Suppress("unused")

package ru.mairwunnx.moafks.managers

import org.bukkit.Location
import org.bukkit.Material.CACTUS
import org.bukkit.Material.CAMPFIRE
import org.bukkit.Material.CORNFLOWER
import org.bukkit.Material.DANDELION
import org.bukkit.Material.DEAD_BUSH
import org.bukkit.Material.FERN
import org.bukkit.Material.FIRE
import org.bukkit.Material.KELP
import org.bukkit.Material.KELP_PLANT
import org.bukkit.Material.LADDER
import org.bukkit.Material.LARGE_FERN
import org.bukkit.Material.LAVA
import org.bukkit.Material.LILAC
import org.bukkit.Material.LILY_OF_THE_VALLEY
import org.bukkit.Material.MAGMA_BLOCK
import org.bukkit.Material.PEONY
import org.bukkit.Material.POPPY
import org.bukkit.Material.POWDER_SNOW
import org.bukkit.Material.ROSE_BUSH
import org.bukkit.Material.SEAGRASS
import org.bukkit.Material.SHORT_GRASS
import org.bukkit.Material.SOUL_CAMPFIRE
import org.bukkit.Material.SOUL_FIRE
import org.bukkit.Material.SUNFLOWER
import org.bukkit.Material.SWEET_BERRY_BUSH
import org.bukkit.Material.TALL_GRASS
import org.bukkit.Material.VINE
import org.bukkit.Material.WATER
import org.bukkit.Material.WITHER_ROSE
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import ru.mairwunnx.moafks.PluginUnit
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min

class LocationManager(private val plugin: PluginUnit) : Closeable {
  private val unsafeBelow = setOf(LAVA, MAGMA_BLOCK, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE, CACTUS, SWEET_BERRY_BUSH, POWDER_SNOW)

  fun isSafeLocation(location: Location): Boolean {
    val world = location.world ?: return false
    val border = world.worldBorder
    if (!border.isInside(location)) return false

    val feet = world.getBlockAt(location.blockX, location.blockY, location.blockZ)
    val head = world.getBlockAt(location.blockX, location.blockY + 1, location.blockZ)
    val below = world.getBlockAt(location.blockX, location.blockY - 1, location.blockZ)

    // Под ногами — твердый и не опасный блок
    if (!below.type.isSolid) return false
    if (below.type in unsafeBelow) return false

    // На уровне ног и головы — проходимо (воздух и пр.), но не вода/лава
    if (!isPassableBlock(feet)) return false
    if (!isPassableBlock(head)) return false

    return true
  }

  fun findSafeLocation(player: Player, searchRadius: Int = 5): Location? {
    val start = player.location
    val world = start.world ?: return null

    if (isSafeLocation(start)) return start

    for (radius in 1..searchRadius) {
      searchInRadius(start, radius)?.let { return it }
    }

    return searchVertically(start, searchRadius) ?: findSpawnLocation(world)
  }

  private fun searchInRadius(center: Location, radius: Int): Location? {
    val world = center.world ?: return null
    val border = world.worldBorder
    val y = center.blockY

    // Перебираем «кольцо», но можно и весь квадрат — не критично.
    for (x in (center.blockX - radius)..(center.blockX + radius)) {
      for (z in (center.blockZ - radius)..(center.blockZ + radius)) {
        val candidate = Location(world, x + 0.5, y.toDouble(), z + 0.5, center.yaw, center.pitch)
        // сначала зажмём к границе, чтобы не предлагать точки за пределами
        val inside = if (!border.isInside(candidate)) clampToWorldBorder(candidate) else candidate
        if (isSafeLocation(inside)) return inside
      }
    }

    return null
  }

  private fun searchVertically(center: Location, radius: Int): Location? {
    val world = center.world ?: return null
    val maxY = min(world.maxHeight - 2, center.blockY + 10)
    val minY = max(world.minHeight, center.blockY - 10)

    for (dy in 1..10) {
      val upY = center.blockY + dy
      if (upY <= maxY) {
        val up = center.clone().apply { y = upY.toDouble() }
        searchInRadius(up, radius)?.let { return it }
      }
      val downY = center.blockY - dy
      if (downY >= minY) {
        val down = center.clone().apply { y = downY.toDouble() }
        searchInRadius(down, radius)?.let { return it }
      }
    }
    return null
  }

  private fun findSpawnLocation(world: World): Location {
    val spawn = world.spawnLocation
    if (isSafeLocation(spawn)) return spawn
    for (r in 1..10) {
      searchInRadius(spawn, r)?.let { return it }
    }
    return spawn
  }

  private fun isPassableBlock(block: Block): Boolean {
    val type = block.type

    if (type.isAir) return true

    // Вода/лава — считаем НЕпроходимыми для safe спота AFK
    if (type == WATER || type == LAVA) return false

    // «Тонкие» и декоративные — проходимые
    if (type == SHORT_GRASS || type == TALL_GRASS ||
      type == FERN || type == LARGE_FERN ||
      type == DEAD_BUSH || type == SEAGRASS ||
      type == KELP || type == KELP_PLANT ||
      type.name.endsWith("_TORCH") || type.name.endsWith("_WALL_TORCH") ||
      type.name.endsWith("_SIGN") || type.name.endsWith("_WALL_SIGN") ||
      type.name.endsWith("_BANNER") || type == LADDER ||
      type == VINE || type.name.endsWith("PRESSURE_PLATE") ||
      type.name.contains("TULIP") || type.name.contains("ORCHID") ||
      type.name.contains("ALLIUM") || type.name.contains("DAISY") ||
      type == DANDELION || type == POPPY ||
      type == CORNFLOWER || type == LILY_OF_THE_VALLEY ||
      type == WITHER_ROSE || type == SUNFLOWER ||
      type == LILAC || type == ROSE_BUSH || type == PEONY
    ) return true

    // Опасные — НЕпроходимые
    if (type in unsafeBelow) return false

    // По умолчанию — непроходимо
    return false
  }

  fun clampToWorldBorder(loc: Location, margin: Double = 1.0): Location {
    val world = loc.world ?: return loc
    val border = world.worldBorder

    if (border.isInside(loc)) return loc

    val center = border.center
    val half = border.size / 2.0 - margin // size — диаметр в блоках
    // Квадратная граница по X/Z:
    val minX = center.x - half
    val maxX = center.x + half
    val minZ = center.z - half
    val maxZ = center.z + half

    val clampedX = loc.x.coerceIn(minX, maxX)
    val clampedZ = loc.z.coerceIn(minZ, maxZ)

    // Убедимся, что Y в допустимом диапазоне мира
    val minY = world.minHeight.toDouble()
    val maxY = (world.maxHeight - 2).toDouble()
    val clampedY = loc.y.coerceIn(minY, maxY)

    return Location(world, clampedX, clampedY, clampedZ, loc.yaw, loc.pitch)
  }

  override fun close() {}
}
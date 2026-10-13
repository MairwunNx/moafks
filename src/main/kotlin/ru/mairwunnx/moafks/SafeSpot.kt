package ru.mairwunnx.moafks

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Finds out whether a player who comes back from AFK stands somewhere that hurts: in lava, fire, or
 * powder snow, on magma, or with their head in a solid block. While AFK nothing hurts them, but a
 * piston or a water stream may have moved them there. Water alone is fine.
 */
object SafeSpot {
  private val HAZARDS = setOf(
    Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.POWDER_SNOW,
    Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
  )
  private val HOT_FLOORS = setOf(Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.LAVA)

  fun isSafe(location: Location): Boolean {
    val world = location.world ?: return true
    if (location.y < world.minHeight) return false
    val feet = location.block
    val head = feet.getRelative(0, 1, 0)
    return feet.type !in HAZARDS && head.type !in HAZARDS && !head.type.isOccluding && feet.getRelative(0, -1, 0).type !in HOT_FLOORS
  }

  /** The nearest spot to stand on within a few blocks, or null when there's none: then the player stays put. */
  fun near(location: Location, radius: Int = 5, height: Int = 8): Location? {
    val world = location.world ?: return null
    val border = world.worldBorder
    val candidates = buildList {
      for (dx in -radius..radius) for (dz in -radius..radius) for (dy in -height..height) add(Triple(dx, dy, dz))
    }.sortedBy { (dx, dy, dz) -> dx * dx + dz * dz + dy * dy * 2 }

    for ((dx, dy, dz) in candidates) {
      val y = location.blockY + dy
      if (y <= world.minHeight || y >= world.maxHeight - 1) continue
      val feet = world.getBlockAt(location.blockX + dx, y, location.blockZ + dz)
      if (!standable(feet)) continue
      val spot = feet.location.add(0.5, 0.0, 0.5).apply { yaw = location.yaw; pitch = location.pitch }
      if (border.isInside(spot)) return spot
    }
    return null
  }

  private fun standable(feet: Block): Boolean {
    val head = feet.getRelative(0, 1, 0)
    val floor = feet.getRelative(0, -1, 0)
    return clear(feet) && clear(head) && floor.type.isSolid && floor.type !in HOT_FLOORS && floor.type !in HAZARDS
  }

  private fun clear(block: Block) = block.isPassable && !block.isLiquid && block.type !in HAZARDS
}

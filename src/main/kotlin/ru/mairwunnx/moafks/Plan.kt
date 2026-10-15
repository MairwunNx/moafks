package ru.mairwunnx.moafks

import com.djrapitops.plan.capability.CapabilityService
import com.djrapitops.plan.extension.CallEvents
import com.djrapitops.plan.extension.Caller
import com.djrapitops.plan.extension.DataExtension
import com.djrapitops.plan.extension.ExtensionService
import com.djrapitops.plan.extension.FormatType
import com.djrapitops.plan.extension.NotReadyException
import com.djrapitops.plan.extension.annotation.BooleanProvider
import com.djrapitops.plan.extension.annotation.Conditional
import com.djrapitops.plan.extension.annotation.NumberProvider
import com.djrapitops.plan.extension.annotation.PluginInfo
import com.djrapitops.plan.extension.annotation.StringProvider
import com.djrapitops.plan.extension.annotation.TableProvider
import com.djrapitops.plan.extension.icon.Color
import com.djrapitops.plan.extension.icon.Family
import com.djrapitops.plan.extension.icon.Icon
import com.djrapitops.plan.extension.table.Table
import com.djrapitops.plan.extension.table.TableColumnFormat
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/** What the rest of the plugin sees of Plan: a nudge when someone goes AFK or comes back. */
interface Analytics : AutoCloseable {
  fun changed(player: Player)
}

/**
 * Plan (Player Analytics), when it's there. Its “Plugins Overview” shows who is AFK now; a player's
 * “Plugins” tab and the players table show their AFK time. Plan's classes load only when Plan is on.
 */
object PlanAnalytics {
  private val needed = listOf("DATA_EXTENSION_VALUES", "DATA_EXTENSION_TABLES", "DATA_EXTENSION_SHOW_IN_PLAYER_TABLE")

  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("Plan")

  /** Registers the extension, now and after every `/plan reload`. Call only when [available]. */
  fun register(plugin: MoAfksPlugin): Analytics {
    val capabilities = CapabilityService.getInstance()
    val missing = needed.filterNot(capabilities::hasCapability)
    require(missing.isEmpty()) { "this Plan is too old, it lacks ${missing.joinToString()}" }
    val extension = PlanExtension(plugin)
    val analytics = object : Analytics {
      @Volatile var caller: Caller? = null
      @Volatile var closed = false

      fun connect() {
        caller = ExtensionService.getInstance().register(extension).orElse(null)
      }

      override fun changed(player: Player) {
        caller?.run {
          updatePlayerData(player.uniqueId, player.name)
          updateServerData()
        }
      }

      override fun close() {
        closed = true
        caller = null
        runCatching { ExtensionService.getInstance().unregister(extension) }
      }
    }
    analytics.connect()
    // Plan forgets extensions on reload and calls this once it's back; a listener can't be removed, so it checks.
    capabilities.registerEnableListener { enabled -> if (enabled && !analytics.closed && plugin.isEnabled) analytics.connect() }
    return analytics
  }
}

/**
 * The values Plan shows. Plan calls these from its own threads, so they read only [Afk]'s concurrent
 * maps, never Bukkit. A player Mo’Afks hasn't seen since the server started is “not ready”, and Plan
 * keeps what it stored before.
 */
@PluginInfo(name = "Mo’Afks", iconName = "bed", iconFamily = Family.SOLID, color = Color.TEAL)
class PlanExtension(private val plugin: MoAfksPlugin) : DataExtension {
  override fun callExtensionMethodsOn() = arrayOf(
    CallEvents.PLAYER_JOIN, CallEvents.PLAYER_LEAVE, CallEvents.PLAYER_PERIODICAL,
    CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.SERVER_PERIODICAL,
  )

  @BooleanProvider(
    text = "AFK now", conditionName = "afk", priority = 100,
    iconName = "bed", iconFamily = Family.SOLID, iconColor = Color.TEAL, showInPlayerTable = true,
  )
  fun afk(player: UUID): Boolean = plugin.afk.state(player) != null

  @Conditional("afk")
  @StringProvider(text = "Why", description = "The reason given with /afk, or how the player went AFK", priority = 90, iconName = "comment", iconColor = Color.TEAL)
  fun reason(player: UUID): String {
    val state = plugin.afk.state(player) ?: throw NotReadyException()
    return state.reason ?: if (state.manual) "/afk without a reason" else "Idle"
  }

  @NumberProvider(
    text = "Time AFK", description = "Across all sessions; take it from play time to get time really played", priority = 80,
    format = FormatType.TIME_MILLISECONDS, iconName = "clock", iconFamily = Family.REGULAR, iconColor = Color.TEAL, showInPlayerTable = true,
  )
  fun time(player: UUID): Long = if (plugin.afk.known(player)) plugin.afk.totalSeconds(player) * 1000 else throw NotReadyException()

  @NumberProvider(text = "Times AFK", description = "How many times the player went AFK", priority = 70, iconName = "repeat", iconColor = Color.TEAL)
  fun spells(player: UUID): Long = if (plugin.afk.known(player)) plugin.afk.spells(player) else throw NotReadyException()

  @NumberProvider(text = "AFK now", description = "Players in AFK mode right now", priority = 100, iconName = "bed", iconColor = Color.TEAL)
  fun afkNow(): Long = plugin.afk.count.toLong()

  @TableProvider(tableColor = Color.TEAL)
  fun afkPlayers(): Table {
    val now = System.currentTimeMillis()
    val table = Table.builder()
      .columnOne("AFK now", Icon.called("user").build()).columnOneFormat(TableColumnFormat.PLAYER_NAME)
      .columnTwo("For", Icon.called("clock").of(Family.REGULAR).build()).columnTwoFormat(TableColumnFormat.TIME_MILLISECONDS)
      .columnThree("Why", Icon.called("comment").build())
    plugin.afk.current().sortedBy { it.since }.forEach { state ->
      table.addRow(state.name, now - state.since, state.reason ?: if (state.manual) "/afk" else "Idle")
    }
    return table.build()
  }
}

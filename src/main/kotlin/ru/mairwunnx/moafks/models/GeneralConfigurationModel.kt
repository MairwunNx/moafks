@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, ComponentSerializer::class)

package ru.mairwunnx.moafks.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Particle
import org.bukkit.Sound
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.ExitTrigger.CHAT
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.ExitTrigger.COMBAT
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.ExitTrigger.COMMAND
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.ExitTrigger.MOVE
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.COLLISION
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.ENTITY_DAMAGE
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.EXPLOSIONS
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.FALL
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.FIRE
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.HUNGER
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.MOB_TARGET
import ru.mairwunnx.moafks.models.GeneralConfigurationModel.PreventType.PROJECTILE_DAMAGE
import ru.mairwunnx.moafks.serializers.ComponentSerializer
import ru.mairwunnx.moafks.serializers.ParticleSerializer
import ru.mairwunnx.moafks.serializers.SoundSerializer

private val mm = MiniMessage.miniMessage()
private fun c(s: String): Component = mm.deserialize(s)

@Serializable class GeneralConfigurationModel(
  @SerialName("enabled") val enabled: Boolean,
  @SerialName("debug") val debug: Boolean,
  @SerialName("auto") val auto: AutoSection,
  @SerialName("manual") val manual: ManualSection,
  @SerialName("limits") val limits: LimitsSection,
  @SerialName("prevents") val prevents: List<PreventType>,
  @SerialName("effects") val effects: EffectsSection,
  @SerialName("exit_triggers") val exitTriggers: List<ExitTrigger>,
  @SerialName("system") val system: SystemSection
) {
  @Serializable class AutoSection(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("seconds") val seconds: Int,
    @SerialName("enter_message") val enterMessage: Component,
    @SerialName("enter_broadcast_message") val enterBroadcastMessage: Component,
    @SerialName("enter_sound") val enterSound: Sound,
    @SerialName("exit_message") val exitMessage: Component,
    @SerialName("exit_sound") val exitSound: Sound
  )

  @Serializable class ManualSection(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("enter_message") val enterMessage: Component,
    @SerialName("enter_message_reasoned") val enterMessageReasoned: Component,
    @SerialName("enter_broadcast_message") val enterBroadcastMessage: Component,
    @SerialName("enter_broadcast_message_reasoned") val enterBroadcastMessageReasoned: Component,
    @SerialName("enter_sound") val enterSound: Sound,
    @SerialName("exit_message") val exitMessage: Component,
    @SerialName("exit_sound") val exitSound: Sound,
    @SerialName("declined_message") val declinedMessage: Component,
    @SerialName("declined_sound") val declinedSound: Sound
  )

  @Serializable class LimitsSection(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("seconds") val seconds: Int,
    @SerialName("kick_message") val kickMessage: Component,
    @SerialName("warning") val warning: WarningSection
  )

  @Serializable class WarningSection(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("seconds") val seconds: List<Int>,
    @SerialName("sound") val sound: Sound,
    @SerialName("text") val text: Component
  )

  @Serializable class EffectsSection(
    @SerialName("noclip") val noclip: NoclipSection,
    @SerialName("particles") val particles: ParticlesSection,
    @SerialName("nickname") val nickname: NicknameSection
  )

  @Serializable class NoclipSection(@SerialName("enabled") val enabled: Boolean)

  @Serializable class ParticlesSection(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("type") val type: Particle
  )

  @Serializable class NicknameSection(@SerialName("enabled") val enabled: Boolean)

  @Serializable class SystemSection(@SerialName("messages") val messages: SystemMessages)

  @Serializable class SystemMessages(
    @SerialName("reloaded") val reloaded: Component,
    @SerialName("incorrect") val incorrect: Component,
    @SerialName("restricted") val restricted: Component,
    @SerialName("unsafe") val unsafe: Component
  )

  @Serializable enum class PreventType {
    @SerialName("explosions") EXPLOSIONS,
    @SerialName("fire") FIRE,
    @SerialName("entity_damage") ENTITY_DAMAGE,
    @SerialName("projectile_damage") PROJECTILE_DAMAGE,
    @SerialName("hunger") HUNGER,
    @SerialName("fall") FALL,
    @SerialName("mob_target") MOB_TARGET,
    @SerialName("collision") COLLISION
  }

  @Serializable enum class ExitTrigger {
    @SerialName("combat") COMBAT,
    @SerialName("move") MOVE,
    @SerialName("chat") CHAT,
    @SerialName("command") COMMAND
  }

  companion object {
    fun default() = GeneralConfigurationModel(
      enabled = true,
      debug = false,
      auto = AutoSection(
        enabled = true,
        seconds = 60,
        enterMessage = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <yellow>Вы автоматически вошли в режим AFK.</yellow> <gray>Двигайтесь или введите </gray><white>/afk</white><gray>, чтобы вернуться.</gray>"),
        enterBroadcastMessage = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <white><player></white> теперь <yellow>AFK</yellow> <gray>(неактивность)</gray>."),
        enterSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
        exitMessage = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <green>Вы автоматически вышли из AFK.</green>"),
        exitSound = Sound.ENTITY_PLAYER_LEVELUP
      ),
      manual = ManualSection(
        enabled = true,
        enterMessage = c("<light_purple>»</light_purple> <aqua>Ты решил отойти от компьютера. Хорошего отдыха!</aqua>"),
        enterMessageReasoned = c("<light_purple>»</light_purple> <aqua>Ты решил отойти от компьютера</aqua> <dark_gray>по причине:</dark_gray> <yellow><reason></yellow><aqua>. Хорошего отдыха!</aqua>"),
        enterBroadcastMessage = c("<light_purple>»</light_purple> <white><player></white> <aqua>решил отойти от компьютера</aqua>"),
        enterBroadcastMessageReasoned = c("<light_purple>»</light_purple> <white><player></white> <aqua>решил отойти от компьютера</aqua> <dark_gray>по причине:</dark_gray> <yellow><reason></yellow>"),
        enterSound = Sound.UI_BUTTON_CLICK,
        exitMessage = c("<green>«</green> <aqua>С возвращением в игру!</aqua> <green>Надеюсь, ты хорошо отдохнул и готов продолжить приключения</green>"),
        exitSound = Sound.ENTITY_PLAYER_LEVELUP,
        declinedMessage = c("<red>×</red> <yellow>Сейчас нельзя отойти от компьютера!</yellow> <red>Слишком опасная ситуация</red> <dark_gray>(активный бой или падение)</dark_gray>"),
        declinedSound = Sound.ENTITY_VILLAGER_NO
      ),
      limits = LimitsSection(
        enabled = true,
        seconds = 1200,
        kickMessage = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <red>Превышено время AFK.</red> <gray>Вы были отключены.</gray>"),
        warning = WarningSection(
          enabled = true,
          seconds = listOf(10, 30, 60, 90),
          sound = Sound.BLOCK_NOTE_BLOCK_PLING,
          text = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <yellow>AFK завершится через</yellow> <white><seconds></white> <yellow>сек.</yellow>")
        )
      ),
      prevents = listOf(EXPLOSIONS, FIRE, ENTITY_DAMAGE, PROJECTILE_DAMAGE, HUNGER, FALL, MOB_TARGET, COLLISION),
      effects = EffectsSection(
        noclip = NoclipSection(enabled = true),
        particles = ParticlesSection(enabled = true, type = Particle.HAPPY_VILLAGER),
        nickname = NicknameSection(enabled = true)
      ),
      exitTriggers = listOf(COMBAT, MOVE, CHAT, COMMAND),
      system = SystemSection(
        messages = SystemMessages(
          reloaded = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <green>Конфигурация перезагружена.</green>"),
          incorrect = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <red>Неверная команда.</red> <gray>Используйте </gray><white>/moafks reload</white><gray>.</gray>"),
          restricted = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <red>Недостаточно прав.</red>"),
          unsafe = c("<gray>[</gray><gold>AFK</gold><gray>]</gray> <yellow>Выход был небезопасным — выполнен телепорт на ближайший безопасный блок.</yellow>")
        )
      )
    )
  }
}


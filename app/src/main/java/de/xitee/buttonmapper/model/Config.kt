package de.xitee.buttonmapper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Gesture {
    @SerialName("single") SINGLE,
    @SerialName("double") DOUBLE,
    @SerialName("hold") HOLD,
}

/** Which physical key is meant. [deviceName] = null matches the key on any input device. */
@Serializable
data class KeyTrigger(
    val keyName: String,
    val deviceName: String? = null,
) {
    fun matches(keyName: String, deviceName: String): Boolean =
        this.keyName == keyName && (this.deviceName == null || this.deviceName == deviceName)
}

/**
 * An action a gesture can run, including its own settings.
 *
 * To add an action: add a subclass here, list a default instance in [ActionConfig.defaults], handle it in
 * [de.xitee.buttonmapper.action.ActionExecutor], give it a label and summary in `ui/Labels.kt` and, if it
 * has settings, a settings section in `ui/action/ActionSettings.kt`.
 */
@Serializable
sealed interface ActionConfig {

    @Serializable
    @SerialName("flashlight_toggle")
    data class FlashlightToggle(
        /** Turn the flashlight off automatically after [autoOffMinutes] when this action switched it on. */
        val autoOffEnabled: Boolean = false,
        val autoOffMinutes: Int = 15,
    ) : ActionConfig {
        val autoOffMillis: Long? get() = if (autoOffEnabled) autoOffMinutes * 60_000L else null
    }

    @Serializable
    @SerialName("vibrate")
    data class Vibrate(
        val pattern: VibrationPattern = VibrationPattern.CLICK,
        /** Only used by [VibrationPattern.CUSTOM]. */
        val durationMs: Int = 200,
        /** 10..100 %, only used by [VibrationPattern.CUSTOM] on devices with amplitude control. */
        val strengthPercent: Int = 100,
    ) : ActionConfig

    companion object {
        /** One default instance per action type, in the order offered in the UI. */
        val defaults: List<ActionConfig> = listOf(FlashlightToggle(), Vibrate())
    }
}

@Serializable
enum class VibrationPattern {
    @SerialName("tick") TICK,
    @SerialName("click") CLICK,
    @SerialName("double_click") DOUBLE_CLICK,
    @SerialName("heavy_click") HEAVY_CLICK,
    @SerialName("custom") CUSTOM,
}

/** An action as configured on a gesture; [id] identifies it in navigation and edits. */
@Serializable
data class ConfiguredAction(
    val id: String = UUID.randomUUID().toString(),
    val config: ActionConfig,
)

/** A configured hardware button with a list of actions per gesture, run in order. */
@Serializable
data class ButtonConfig(
    val id: String = UUID.randomUUID().toString(),
    val trigger: KeyTrigger,
    val enabled: Boolean = true,
    /**
     * Android should no longer react to the button. Implemented by grabbing the whole input device
     * of [KeyTrigger.deviceName], so it only works for device-specific triggers.
     */
    val blockSystemFunction: Boolean = false,
    val gestures: Map<Gesture, List<ConfiguredAction>> = emptyMap(),
) {
    fun actions(gesture: Gesture): List<ConfiguredAction> = gestures[gesture].orEmpty()

    /** Gestures that have at least one action. */
    val mappedGestures: Set<Gesture> get() = gestures.filterValues { it.isNotEmpty() }.keys
}

@Serializable
data class Timing(
    /** Max. pause between two presses of a double press. */
    val doublePressMs: Long = 300,
    /** Min. press duration for a hold. */
    val holdMs: Long = 500,
)

@Serializable
data class AppConfig(
    val serviceEnabled: Boolean = false,
    val buttons: List<ButtonConfig> = emptyList(),
    val timing: Timing = Timing(),
) {
    /** Devices to grab exclusively: those of all active buttons that block the system function. */
    val devicesToGrab: Set<String>
        get() = buttons
            .filter { it.enabled && it.blockSystemFunction }
            .mapNotNullTo(HashSet()) { it.trigger.deviceName }

    fun button(id: String): ButtonConfig? = buttons.firstOrNull { it.id == id }

    /** Active buttons matching a key event. */
    fun activeButtonsFor(keyName: String, deviceName: String): List<ButtonConfig> =
        buttons.filter { it.enabled && it.trigger.matches(keyName, deviceName) }
}

package de.xitee.buttonmapper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Gesture(val label: String) {
    @SerialName("single") SINGLE("Einfach drücken"),
    @SerialName("double") DOUBLE("Doppelt drücken"),
    @SerialName("hold") HOLD("Gedrückt halten"),
}

/** Which physical key triggers a mapping. [deviceName] = null matches the key on any device. */
@Serializable
data class KeyTrigger(
    val keyName: String,
    val deviceName: String? = null,
) {
    fun matches(keyName: String, deviceName: String): Boolean =
        this.keyName == keyName && (this.deviceName == null || this.deviceName == deviceName)
}

/**
 * An action a mapping can run. New actions: add a subclass here, list it in [ActionConfig.all]
 * and handle it in [de.xitee.buttonmapper.action.ActionExecutor].
 */
@Serializable
sealed interface ActionConfig {
    val label: String

    @Serializable
    @SerialName("flashlight_toggle")
    data object FlashlightToggle : ActionConfig {
        override val label get() = "Taschenlampe umschalten"
    }

    companion object {
        /** Actions offered in the UI. */
        val all: List<ActionConfig> = listOf(FlashlightToggle)
    }
}

@Serializable
data class Mapping(
    val id: String = UUID.randomUUID().toString(),
    val trigger: KeyTrigger,
    val gesture: Gesture,
    val action: ActionConfig,
    val enabled: Boolean = true,
    /**
     * Android should no longer react to the key. Implemented by grabbing the whole input device
     * of [KeyTrigger.deviceName], so it only works for device-specific triggers.
     */
    val blockSystemFunction: Boolean = false,
)

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
    val mappings: List<Mapping> = emptyList(),
    val timing: Timing = Timing(),
) {
    /** Devices to grab exclusively: those of all active mappings that block the system function. */
    val devicesToGrab: Set<String>
        get() = mappings
            .filter { it.enabled && it.blockSystemFunction }
            .mapNotNullTo(HashSet()) { it.trigger.deviceName }
}

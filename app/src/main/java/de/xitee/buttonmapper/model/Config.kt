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
    /** Devices that are grabbed exclusively, i.e. Android no longer receives their keys. */
    val exclusiveDevices: Set<String> = emptySet(),
)

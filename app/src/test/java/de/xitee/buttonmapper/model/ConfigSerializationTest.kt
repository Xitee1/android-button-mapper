package de.xitee.buttonmapper.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTrip_keepsActionListsAndSettings() {
        val config = AppConfig(
            serviceEnabled = true,
            buttons = listOf(
                ButtonConfig(
                    trigger = KeyTrigger("KEY_ASSISTANT", "gpio-keys"),
                    blockSystemFunction = true,
                    gestures = mapOf(
                        Gesture.SINGLE to listOf(
                            ConfiguredAction(config = ActionConfig.FlashlightToggle(autoOffEnabled = true, autoOffMinutes = 5)),
                            ConfiguredAction(config = ActionConfig.Vibrate(VibrationPattern.CUSTOM, 300, 70)),
                        ),
                    ),
                ),
            ),
        )
        val text = json.encodeToString(AppConfig.serializer(), config)
        assertEquals(config, json.decodeFromString(AppConfig.serializer(), text))
    }

    @Test
    fun mappedGestures_ignoresEmptyLists() {
        val button = ButtonConfig(
            trigger = KeyTrigger("KEY_ASSISTANT"),
            gestures = mapOf(Gesture.SINGLE to emptyList(), Gesture.HOLD to listOf(ConfiguredAction(config = ActionConfig.Vibrate()))),
        )
        assertEquals(setOf(Gesture.HOLD), button.mappedGestures)
    }
}

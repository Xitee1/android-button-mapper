package de.xitee.buttonmapper.input

enum class KeyAction { UP, DOWN, REPEAT;
    companion object {
        fun fromValue(value: Int) = when (value) { 0 -> UP; 1 -> DOWN; else -> REPEAT }
    }
}

data class InputKeyEvent(
    val devicePath: String,
    val deviceName: String,
    val code: Int,
    val action: KeyAction,
    val timeMillis: Long,
) {
    val keyName: String get() = KeyNames.nameOf(code)

    /** Identifies one physical key across events. */
    val keyId: String get() = "$deviceName/$code"
}

data class InputDevice(
    val path: String,
    val name: String,
    val keys: List<String>,
    val grabbed: Boolean,
) {
    /** Grabbing a device with the power key would make the phone unusable. */
    val canBeGrabbed: Boolean get() = "KEY_POWER" !in keys

    companion object {
        fun parse(line: String): InputDevice? {
            val parts = line.split('\t')
            if (parts.size < 4) return null
            return InputDevice(
                path = parts[0],
                name = parts[1],
                grabbed = parts[2] == "1",
                keys = parts[3].split(',').filter { it.isNotBlank() },
            )
        }
    }
}

package de.xitee.buttonmapper.action

import android.content.Context
import de.xitee.buttonmapper.model.ActionConfig

/** Runs [ActionConfig]s. Holds long-lived helpers (e.g. torch state tracking) for the service's lifetime. */
class ActionExecutor(context: Context) {

    private val flashlight = FlashlightController(context)
    private val vibration = VibrationController(context)

    fun execute(action: ActionConfig): Result<Unit> = runCatching {
        when (action) {
            is ActionConfig.FlashlightToggle -> flashlight.toggle(action.autoOffMillis)
            is ActionConfig.Vibrate -> vibration.vibrate(action)
        }
    }

    fun release() {
        flashlight.release()
    }
}

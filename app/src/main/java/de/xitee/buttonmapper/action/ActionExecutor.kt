package de.xitee.buttonmapper.action

import android.content.Context
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.FlashlightSettings

/** Runs [ActionConfig]s. Holds long-lived helpers (e.g. torch state tracking) for the service's lifetime. */
class ActionExecutor(
    context: Context,
    private val flashlightSettings: () -> FlashlightSettings,
) {

    private val flashlight = FlashlightController(context)

    fun execute(action: ActionConfig): Result<Unit> = runCatching {
        when (action) {
            ActionConfig.FlashlightToggle -> flashlight.toggle(flashlightSettings().autoOffMillis)
        }
    }

    fun release() {
        flashlight.release()
    }
}

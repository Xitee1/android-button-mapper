package de.xitee.buttonmapper.ui.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.ui.components.SwitchListItem
import de.xitee.buttonmapper.ui.components.ValueSlider

/** Whether an action type shows a settings section on the action page. */
val ActionConfig.hasSettings: Boolean
    get() = when (this) {
        is ActionConfig.FlashlightToggle -> true
    }

/** Settings section of an action. Each action type gets its own composable below. */
@Composable
fun ActionSettings(action: ActionConfig, onChange: (ActionConfig) -> Unit) {
    when (action) {
        is ActionConfig.FlashlightToggle -> FlashlightToggleSettings(action, onChange)
    }
}

@Composable
private fun FlashlightToggleSettings(
    action: ActionConfig.FlashlightToggle,
    onChange: (ActionConfig.FlashlightToggle) -> Unit,
) {
    SwitchListItem(
        headline = stringResource(R.string.flashlight_auto_off),
        checked = action.autoOffEnabled,
        onCheckedChange = { onChange(action.copy(autoOffEnabled = it)) },
        supporting = {
            Text(
                if (action.autoOffEnabled) stringResource(R.string.flashlight_auto_off_on, action.autoOffMinutes)
                else stringResource(R.string.flashlight_auto_off_off),
            )
        },
    )
    if (action.autoOffEnabled) {
        ValueSlider(
            label = { stringResource(R.string.flashlight_auto_off_after, it) },
            value = action.autoOffMinutes.toLong(),
            range = 1f..60f,
            step = 1,
        ) { onChange(action.copy(autoOffMinutes = it.toInt())) }
    }
}

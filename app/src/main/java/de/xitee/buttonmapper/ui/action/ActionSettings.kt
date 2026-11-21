package de.xitee.buttonmapper.ui.action

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.action.VibrationController
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.VibrationPattern
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.RadioListItem
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.components.SwitchListItem
import de.xitee.buttonmapper.ui.components.ValueSlider
import de.xitee.buttonmapper.ui.labelRes

/** Whether an action type has a settings page content. */
val ActionConfig.hasSettings: Boolean
    get() = when (this) {
        is ActionConfig.FlashlightToggle -> true
        is ActionConfig.Vibrate -> true
    }

/** Settings of an action. Each action type gets its own composable below. */
@Composable
fun ActionSettings(action: ActionConfig, onChange: (ActionConfig) -> Unit) {
    when (action) {
        is ActionConfig.FlashlightToggle -> FlashlightToggleSettings(action, onChange)
        is ActionConfig.Vibrate -> VibrateSettings(action, onChange)
    }
}

@Composable
private fun FlashlightToggleSettings(
    action: ActionConfig.FlashlightToggle,
    onChange: (ActionConfig.FlashlightToggle) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_auto_off))
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

@Composable
private fun VibrateSettings(action: ActionConfig.Vibrate, onChange: (ActionConfig.Vibrate) -> Unit) {
    val context = LocalContext.current
    val vibration = remember { VibrationController(context.applicationContext) }

    SectionHeader(stringResource(R.string.section_pattern))
    Column(Modifier.selectableGroup()) {
        VibrationPattern.entries.forEach { pattern ->
            RadioListItem(
                headline = stringResource(pattern.labelRes),
                selected = action.pattern == pattern,
                onClick = { onChange(action.copy(pattern = pattern)) },
            )
        }
    }

    if (action.pattern == VibrationPattern.CUSTOM) {
        SectionHeader(stringResource(R.string.section_custom_vibration))
        ValueSlider(
            label = { stringResource(R.string.vibration_duration, it) },
            value = action.durationMs.toLong(),
            range = 20f..1000f,
            step = 10,
        ) { onChange(action.copy(durationMs = it.toInt())) }
        if (vibration.hasAmplitudeControl) {
            ValueSlider(
                label = { stringResource(R.string.vibration_strength, it) },
                value = action.strengthPercent.toLong(),
                range = 10f..100f,
                step = 10,
            ) { onChange(action.copy(strengthPercent = it.toInt())) }
        } else {
            InfoText(stringResource(R.string.vibration_strength_unsupported))
        }
    }

    OutlinedButton(
        onClick = { vibration.vibrate(action) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) { Text(stringResource(R.string.vibration_test)) }
}

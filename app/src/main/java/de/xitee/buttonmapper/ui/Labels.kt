package de.xitee.buttonmapper.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.ConfiguredAction
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.VibrationPattern
import de.xitee.buttonmapper.shizuku.ReaderError
import de.xitee.buttonmapper.shizuku.ShizukuStatus

/** String resources for model types; kept out of the model so it stays Android-free. */

@get:StringRes
val Gesture.labelRes: Int
    get() = when (this) {
        Gesture.SINGLE -> R.string.gesture_single
        Gesture.DOUBLE -> R.string.gesture_double
        Gesture.HOLD -> R.string.gesture_hold
    }

@get:StringRes
val Gesture.descriptionRes: Int
    get() = when (this) {
        Gesture.SINGLE -> R.string.gesture_single_desc
        Gesture.DOUBLE -> R.string.gesture_double_desc
        Gesture.HOLD -> R.string.gesture_hold_desc
    }

@get:StringRes
val Gesture.shortLabelRes: Int
    get() = when (this) {
        Gesture.SINGLE -> R.string.gesture_single_short
        Gesture.DOUBLE -> R.string.gesture_double_short
        Gesture.HOLD -> R.string.gesture_hold_short
    }

@get:StringRes
val ActionConfig.labelRes: Int
    get() = when (this) {
        is ActionConfig.FlashlightToggle -> R.string.action_flashlight_toggle
        is ActionConfig.Vibrate -> R.string.action_vibrate
    }

@get:StringRes
val VibrationPattern.labelRes: Int
    get() = when (this) {
        VibrationPattern.TICK -> R.string.vibration_pattern_tick
        VibrationPattern.CLICK -> R.string.vibration_pattern_click
        VibrationPattern.DOUBLE_CLICK -> R.string.vibration_pattern_double_click
        VibrationPattern.HEAVY_CLICK -> R.string.vibration_pattern_heavy_click
        VibrationPattern.CUSTOM -> R.string.vibration_pattern_custom
    }

/** One-line summary of an action's settings, shown below its label. */
@Composable
fun actionSummary(action: ActionConfig): String = when (action) {
    is ActionConfig.FlashlightToggle ->
        if (action.autoOffEnabled) stringResource(R.string.flashlight_summary_auto_off, action.autoOffMinutes)
        else stringResource(R.string.flashlight_summary_no_auto_off)
    is ActionConfig.Vibrate ->
        if (action.pattern == VibrationPattern.CUSTOM) {
            stringResource(R.string.vibration_summary_custom, action.durationMs, action.strengthPercent)
        } else {
            stringResource(action.pattern.labelRes)
        }
}

/** Comma-separated labels of a gesture's actions, or null if it has none. */
@Composable
fun actionLabels(actions: List<ConfiguredAction>): String? =
    actions.takeIf { it.isNotEmpty() }?.map { stringResource(it.config.labelRes) }?.joinToString(", ")

@get:StringRes
val ShizukuStatus.labelRes: Int
    get() = when (this) {
        ShizukuStatus.NOT_RUNNING -> R.string.shizuku_not_running
        ShizukuStatus.NO_PERMISSION -> R.string.shizuku_no_permission
        ShizukuStatus.READY -> R.string.shizuku_ready
    }

fun ReaderError.message(context: Context): String = when (kind) {
    ReaderError.Kind.GRAB_POWER_KEY -> context.getString(R.string.error_grab_power_key, detail)
    ReaderError.Kind.GRAB_FAILED -> context.getString(R.string.error_grab_failed, detail)
    ReaderError.Kind.NO_DEVICES -> context.getString(R.string.error_no_devices)
    ReaderError.Kind.READ_FAILED -> context.getString(R.string.error_read_failed, detail)
    ReaderError.Kind.SERVICE_INVALID -> context.getString(R.string.error_service_invalid)
    ReaderError.Kind.CONNECTION_LOST -> context.getString(R.string.error_connection_lost)
    ReaderError.Kind.BIND_FAILED -> context.getString(R.string.error_bind_failed, detail)
    ReaderError.Kind.START_FAILED -> context.getString(R.string.error_start_failed, detail)
}

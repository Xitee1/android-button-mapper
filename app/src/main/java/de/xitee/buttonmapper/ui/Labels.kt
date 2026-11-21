package de.xitee.buttonmapper.ui

import android.content.Context
import androidx.annotation.StringRes
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.Gesture
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
val ActionConfig.labelRes: Int
    get() = when (this) {
        ActionConfig.FlashlightToggle -> R.string.action_flashlight_toggle
    }

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

package de.xitee.buttonmapper.action

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.VibrationPattern
import kotlin.math.roundToInt

/** Plays [ActionConfig.Vibrate] as hardware-button feedback (also allowed from the background service). */
class VibrationController(context: Context) {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** Whether the strength of a custom vibration can be adjusted on this device. */
    val hasAmplitudeControl: Boolean get() = vibrator.hasAmplitudeControl()

    fun vibrate(action: ActionConfig.Vibrate) {
        val effect = effectFor(action)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build(),
            )
        }
    }

    private fun effectFor(action: ActionConfig.Vibrate): VibrationEffect {
        if (Build.VERSION.SDK_INT >= 29) {
            when (action.pattern) {
                VibrationPattern.TICK -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                VibrationPattern.CLICK -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                VibrationPattern.DOUBLE_CLICK -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                VibrationPattern.HEAVY_CLICK -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                VibrationPattern.CUSTOM -> Unit
            }
        }
        return when (action.pattern) {
            VibrationPattern.TICK -> VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            VibrationPattern.CLICK -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            VibrationPattern.DOUBLE_CLICK -> VibrationEffect.createWaveform(longArrayOf(0, 25, 100, 25), -1)
            VibrationPattern.HEAVY_CLICK -> VibrationEffect.createOneShot(50, 255)
            VibrationPattern.CUSTOM -> {
                val amplitude = if (hasAmplitudeControl) {
                    (action.strengthPercent.coerceIn(1, 100) * 255 / 100.0).roundToInt().coerceIn(1, 255)
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                VibrationEffect.createOneShot(action.durationMs.toLong().coerceAtLeast(1), amplitude)
            }
        }
    }
}

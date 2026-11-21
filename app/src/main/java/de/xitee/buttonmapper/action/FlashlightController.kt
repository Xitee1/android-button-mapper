package de.xitee.buttonmapper.action

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import de.xitee.buttonmapper.R

/**
 * Toggles the torch of the main (back) camera, tracks its state via [CameraManager.TorchCallback]
 * and optionally turns it off again after a timeout. Main thread only.
 */
class FlashlightController(private val context: Context) {

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val torchState = HashMap<String, Boolean>()

    /** Keeps the CPU awake so the auto-off timer also runs while the screen is off. */
    private val autoOffWakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ButtonMapper:flashlightAutoOff")
        .apply { setReferenceCounted(false) }

    private val autoOff = Runnable {
        cameraId?.let { id ->
            runCatching { cameraManager.setTorchMode(id, false) }
                .onFailure { Log.w(TAG, "Auto-off failed", it) }
        }
        cancelAutoOff()
    }

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchState[cameraId] = enabled
            // Switched off by any means: no auto-off pending anymore.
            if (!enabled && cameraId == this@FlashlightController.cameraId) cancelAutoOff()
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            torchState[cameraId] = false
            if (cameraId == this@FlashlightController.cameraId) cancelAutoOff()
        }
    }

    private val cameraId: String? by lazy {
        val ids = cameraManager.cameraIdList
        val withFlash = ids.filter {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    }

    init {
        cameraManager.registerTorchCallback(callback, handler)
    }

    /** @param autoOffMillis turn off again after this time if the torch was switched on; null = never. */
    fun toggle(autoOffMillis: Long?) {
        val id = cameraId ?: throw IllegalStateException(context.getString(R.string.error_no_flash))
        val newState = !(torchState[id] ?: false)
        cameraManager.setTorchMode(id, newState)
        torchState[id] = newState
        cancelAutoOff()
        if (newState && autoOffMillis != null) {
            autoOffWakeLock.acquire(autoOffMillis + 10_000)
            handler.postDelayed(autoOff, autoOffMillis)
        }
    }

    fun release() {
        cancelAutoOff()
        cameraManager.unregisterTorchCallback(callback)
    }

    private fun cancelAutoOff() {
        handler.removeCallbacks(autoOff)
        if (autoOffWakeLock.isHeld) autoOffWakeLock.release()
    }

    companion object {
        private const val TAG = "FlashlightController"
    }
}

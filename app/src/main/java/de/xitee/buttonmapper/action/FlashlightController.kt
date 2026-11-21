package de.xitee.buttonmapper.action

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/** Toggles the torch of the main (back) camera and tracks its state via [CameraManager.TorchCallback]. */
class FlashlightController(context: Context) {

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val torchState = HashMap<String, Boolean>()

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchState[cameraId] = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            torchState[cameraId] = false
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
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun toggle() {
        val id = cameraId ?: error("Keine Kamera mit Blitz gefunden")
        val newState = !(torchState[id] ?: false)
        cameraManager.setTorchMode(id, newState)
        torchState[id] = newState
    }

    fun release() {
        cameraManager.unregisterTorchCallback(callback)
    }
}

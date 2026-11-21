package de.xitee.buttonmapper.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import de.xitee.buttonmapper.BuildConfig
import rikka.shizuku.Shizuku

/**
 * App-side handle for [EventReaderService]. Binds the Shizuku user service and keeps the reader
 * running with the requested set of exclusively grabbed devices. Main thread only.
 */
class EventReaderClient(
    private val listener: IKeyEventListener,
    private val onStateChanged: (connected: Boolean, error: ReaderError?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var reader: IEventReader? = null
    private var wanted = false
    private var bound = false
    private var grabDevices: Set<String> = emptySet()

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, EventReaderService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("evdev")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) {
                onStateChanged(false, ReaderError(ReaderError.Kind.SERVICE_INVALID))
                return
            }
            reader = IEventReader.Stub.asInterface(binder)
            startReader()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            reader = null
            bound = false
            onStateChanged(false, ReaderError(ReaderError.Kind.CONNECTION_LOST))
            if (wanted) handler.postDelayed({ if (wanted) bind() }, 2000)
        }
    }

    /** Starts (or restarts with new settings) the reader. */
    fun run(grabDevices: Set<String>) {
        wanted = true
        val changed = grabDevices != this.grabDevices
        this.grabDevices = grabDevices
        when {
            reader != null -> if (changed) startReader()
            !bound -> bind()
        }
    }

    fun shutdown() {
        wanted = false
        runCatching { reader?.stop() }
        reader = null
        if (bound) {
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            bound = false
        }
        onStateChanged(false, null)
    }

    private fun bind() {
        try {
            Shizuku.bindUserService(args, connection)
            bound = true
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService failed", e)
            onStateChanged(false, ReaderError(ReaderError.Kind.BIND_FAILED, e.message.orEmpty()))
        }
    }

    private fun startReader() {
        val r = reader ?: return
        try {
            r.start(listener, grabDevices.toTypedArray())
            onStateChanged(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            onStateChanged(false, ReaderError(ReaderError.Kind.START_FAILED, e.message.orEmpty()))
        }
    }

    private companion object {
        const val TAG = "EventReaderClient"
    }
}

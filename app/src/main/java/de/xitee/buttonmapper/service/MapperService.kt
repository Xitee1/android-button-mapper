package de.xitee.buttonmapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.xitee.buttonmapper.App
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.action.ActionExecutor
import de.xitee.buttonmapper.gesture.GestureEngine
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.input.KeyAction
import de.xitee.buttonmapper.model.AppConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.shizuku.EventReaderClient
import de.xitee.buttonmapper.shizuku.IKeyEventListener
import de.xitee.buttonmapper.shizuku.ShizukuMonitor
import de.xitee.buttonmapper.shizuku.ShizukuStatus
import de.xitee.buttonmapper.ui.MainActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Foreground service: keeps the Shizuku reader alive, detects gestures and runs actions. */
class MapperService : LifecycleService() {

    private val main = Handler(Looper.getMainLooper())
    private val repository by lazy { (application as App).repository }
    private var config = AppConfig()

    private lateinit var actions: ActionExecutor
    private lateinit var engine: GestureEngine
    private lateinit var client: EventReaderClient
    private lateinit var wakeLock: PowerManager.WakeLock

    private val listener = object : IKeyEventListener.Stub() {
        override fun onKeyEvent(devicePath: String, deviceName: String, code: Int, value: Int, timeMillis: Long) {
            val event = InputKeyEvent(devicePath, deviceName, code, KeyAction.fromValue(value), timeMillis)
            main.post { handleKeyEvent(event) }
        }

        override fun onDevices(devices: Array<String>) {
            MapperRuntime.devices.value = devices.mapNotNull(InputDevice::parse)
        }

        override fun onError(message: String) {
            MapperRuntime.lastError.value = message
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        MapperRuntime.serviceRunning.value = true

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ButtonMapper:gesture")
            .apply { setReferenceCounted(false) }
        actions = ActionExecutor(this)
        engine = GestureEngine(
            scope = lifecycleScope,
            timing = { config.timing },
            gesturesFor = ::mappedGestures,
            onGesture = ::runGesture,
        )
        client = EventReaderClient(listener) { connected, error ->
            MapperRuntime.readerConnected.value = connected
            if (error != null) MapperRuntime.lastError.value = error
            updateNotification()
        }

        lifecycleScope.launch { repository.config.collect { config = it } }
        lifecycleScope.launch {
            ShizukuMonitor.status
                .combine(repository.config.map { it.devicesToGrab }.distinctUntilChanged()) { s, grab -> s to grab }
                .collect { (status, grab) ->
                    if (status == ShizukuStatus.READY) client.run(grab) else client.shutdown()
                    updateNotification()
                }
        }
    }

    override fun onDestroy() {
        client.shutdown()
        engine.reset()
        actions.release()
        if (wakeLock.isHeld) wakeLock.release()
        MapperRuntime.serviceRunning.value = false
        MapperRuntime.readerConnected.value = false
        super.onDestroy()
    }

    private fun handleKeyEvent(event: InputKeyEvent) {
        MapperRuntime.lastEvent.value = event
        MapperRuntime.keyEvents.tryEmit(event)
        if (MapperRuntime.learning.value) return
        if (event.action == KeyAction.DOWN && mappedGestures(event).isNotEmpty()) {
            // Keep the CPU awake while hold / double-press timers run, also with the screen off.
            val t = config.timing
            wakeLock.acquire(t.holdMs + t.doublePressMs + 2000)
        }
        engine.onEvent(event)
    }

    private fun mappedGestures(event: InputKeyEvent): Set<Gesture> =
        config.mappings
            .filter { it.enabled && it.trigger.matches(event.keyName, event.deviceName) }
            .mapTo(HashSet()) { it.gesture }

    private fun runGesture(event: InputKeyEvent, gesture: Gesture) {
        config.mappings
            .filter { it.enabled && it.gesture == gesture && it.trigger.matches(event.keyName, event.deviceName) }
            .forEach { mapping ->
                val result = actions.execute(mapping.action)
                val msg = "${event.keyName} · ${gesture.label} → ${mapping.action.label}" +
                    (result.exceptionOrNull()?.let { " (Fehler: ${it.message})" } ?: "")
                Log.i(TAG, msg)
                MapperRuntime.lastActionMessage.value = msg
            }
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hintergrunddienst", NotificationManager.IMPORTANCE_MIN),
        )
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val text = when {
            ShizukuMonitor.status.value != ShizukuStatus.READY -> "Warte auf Shizuku …"
            MapperRuntime.readerConnected.value -> "Tasten werden überwacht"
            else -> "Verbinde …"
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_button)
            .setContentTitle("Button Mapper")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "MapperService"
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MapperService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MapperService::class.java))
        }
    }
}

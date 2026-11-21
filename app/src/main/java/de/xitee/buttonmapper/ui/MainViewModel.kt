package de.xitee.buttonmapper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.xitee.buttonmapper.App
import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.input.KeyAction
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.AppConfig
import de.xitee.buttonmapper.model.ButtonConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.KeyTrigger
import de.xitee.buttonmapper.model.Timing
import de.xitee.buttonmapper.service.MapperRuntime
import de.xitee.buttonmapper.service.MapperService
import de.xitee.buttonmapper.shizuku.ShizukuMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-scoped view model shared by all screens. */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as App).repository

    val config: StateFlow<AppConfig?> =
        repository.config.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shizukuStatus = ShizukuMonitor.status
    val serviceRunning = MapperRuntime.serviceRunning
    val readerConnected = MapperRuntime.readerConnected
    val devices = MapperRuntime.devices
    val lastEvent = MapperRuntime.lastEvent
    val lastAction = MapperRuntime.lastActionMessage
    val lastError = MapperRuntime.lastError

    /** Key captured in learn mode, null while waiting. */
    val learnedKey = MutableStateFlow<InputKeyEvent?>(null)
    private var learnJob: Job? = null

    init {
        // Re-start the service when the app is opened and it should be running.
        viewModelScope.launch {
            if (repository.current().serviceEnabled && !MapperRuntime.serviceRunning.value) {
                MapperService.start(application)
            }
        }
    }

    fun requestShizukuPermission() = ShizukuMonitor.requestPermission()

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(serviceEnabled = enabled) }
            if (enabled) MapperService.start(getApplication()) else MapperService.stop(getApplication())
        }
    }

    fun startLearning() {
        learnJob?.cancel()
        learnedKey.value = null
        MapperRuntime.learning.value = true
        learnJob = viewModelScope.launch {
            val event = MapperRuntime.keyEvents.first { it.action == KeyAction.DOWN }
            learnedKey.value = event
            MapperRuntime.learning.value = false
        }
    }

    fun stopLearning() {
        learnJob?.cancel()
        MapperRuntime.learning.value = false
    }

    /** Returns the id of the button for [event]: an existing one, or a newly created one. */
    suspend fun addButton(event: InputKeyEvent): String {
        var id = ""
        repository.update { cfg ->
            val existing = cfg.buttons.firstOrNull {
                it.trigger.keyName == event.keyName && it.trigger.deviceName == event.deviceName
            }
            if (existing != null) {
                id = existing.id
                cfg
            } else {
                val button = ButtonConfig(trigger = KeyTrigger(event.keyName, event.deviceName))
                id = button.id
                cfg.copy(buttons = cfg.buttons + button)
            }
        }
        return id
    }

    fun deleteButton(id: String) = update { cfg -> cfg.copy(buttons = cfg.buttons.filterNot { it.id == id }) }

    fun setButtonEnabled(id: String, enabled: Boolean) = updateButton(id) { it.copy(enabled = enabled) }

    fun setButtonBlocksSystemFunction(id: String, block: Boolean) =
        updateButton(id) { it.copy(blockSystemFunction = block) }

    /** Sets the action of a gesture; null removes it. */
    fun setAction(id: String, gesture: Gesture, action: ActionConfig?) = updateButton(id) {
        it.copy(actions = if (action == null) it.actions - gesture else it.actions + (gesture to action))
    }

    fun setTiming(timing: Timing) = update { it.copy(timing = timing) }

    private fun updateButton(id: String, transform: (ButtonConfig) -> ButtonConfig) = update { cfg ->
        cfg.copy(buttons = cfg.buttons.map { if (it.id == id) transform(it) else it })
    }

    private fun update(transform: (AppConfig) -> AppConfig) {
        viewModelScope.launch { repository.update(transform) }
    }

    override fun onCleared() {
        MapperRuntime.learning.value = false
    }
}

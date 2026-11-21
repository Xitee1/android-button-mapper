package de.xitee.buttonmapper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.xitee.buttonmapper.App
import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.input.KeyAction
import de.xitee.buttonmapper.model.AppConfig
import de.xitee.buttonmapper.model.Mapping
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

    private var learnJob: Job? = null

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

    fun saveMapping(mapping: Mapping) = update { cfg ->
        val others = cfg.mappings.filterNot { it.id == mapping.id }
        cfg.copy(mappings = others + mapping)
    }

    fun deleteMapping(id: String) = update { cfg -> cfg.copy(mappings = cfg.mappings.filterNot { it.id == id }) }

    fun setMappingEnabled(id: String, enabled: Boolean) = update { cfg ->
        cfg.copy(mappings = cfg.mappings.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun setTiming(timing: Timing) = update { it.copy(timing = timing) }

    fun setDeviceExclusive(deviceName: String, exclusive: Boolean) = update { cfg ->
        cfg.copy(exclusiveDevices = if (exclusive) cfg.exclusiveDevices + deviceName else cfg.exclusiveDevices - deviceName)
    }

    private fun update(transform: (AppConfig) -> AppConfig) {
        viewModelScope.launch { repository.update(transform) }
    }

    override fun onCleared() {
        MapperRuntime.learning.value = false
    }
}

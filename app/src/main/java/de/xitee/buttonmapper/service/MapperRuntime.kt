package de.xitee.buttonmapper.service

import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.shizuku.ReaderError
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** Live state shared between [MapperService] and the UI (same process). */
object MapperRuntime {
    val serviceRunning = MutableStateFlow(false)
    val readerConnected = MutableStateFlow(false)
    val lastError = MutableStateFlow<ReaderError?>(null)
    val devices = MutableStateFlow<List<InputDevice>>(emptyList())
    val lastEvent = MutableStateFlow<InputKeyEvent?>(null)
    val lastActionMessage = MutableStateFlow<String?>(null)

    /** While true, key events are only reported (for "learn key"), mappings are not executed. */
    val learning = MutableStateFlow(false)

    val keyEvents = MutableSharedFlow<InputKeyEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
}

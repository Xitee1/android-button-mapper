package de.xitee.buttonmapper.gesture

import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.input.KeyAction
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.Timing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Turns raw key down/up events into [Gesture]s, separately for every physical key.
 *
 * Only gestures that are actually mapped for a key are considered, so a key with only a single-press
 * mapping fires immediately on release instead of waiting for a possible second press.
 *
 * Not thread-safe: call [onEvent] from the thread [scope] runs on.
 */
class GestureEngine(
    private val scope: CoroutineScope,
    private val timing: () -> Timing,
    private val gesturesFor: (InputKeyEvent) -> Set<Gesture>,
    private val onGesture: (InputKeyEvent, Gesture) -> Unit,
) {
    private sealed interface State {
        data class Pressed(val gestures: Set<Gesture>, val holdJob: Job?) : State
        data class WaitingForSecond(val gestures: Set<Gesture>, val timeoutJob: Job) : State
        /** A gesture already fired for this press; ignore everything until the key is released. */
        data object Consumed : State
    }

    private val states = HashMap<String, State>()

    fun onEvent(event: InputKeyEvent) {
        when (event.action) {
            KeyAction.DOWN -> onDown(event)
            KeyAction.UP -> onUp(event)
            KeyAction.REPEAT -> Unit
        }
    }

    fun reset() {
        states.values.forEach {
            when (it) {
                is State.Pressed -> it.holdJob?.cancel()
                is State.WaitingForSecond -> it.timeoutJob.cancel()
                State.Consumed -> Unit
            }
        }
        states.clear()
    }

    private fun onDown(event: InputKeyEvent) {
        val id = event.keyId
        when (val state = states[id]) {
            is State.WaitingForSecond -> {
                state.timeoutJob.cancel()
                states[id] = State.Consumed
                onGesture(event, Gesture.DOUBLE)
            }
            is State.Pressed, State.Consumed -> Unit
            null -> {
                val gestures = gesturesFor(event)
                if (gestures.isEmpty()) return
                val holdJob = if (Gesture.HOLD in gestures) {
                    scope.launch {
                        delay(timing().holdMs)
                        states[id] = State.Consumed
                        onGesture(event, Gesture.HOLD)
                    }
                } else null
                states[id] = State.Pressed(gestures, holdJob)
            }
        }
    }

    private fun onUp(event: InputKeyEvent) {
        val id = event.keyId
        when (val state = states[id]) {
            is State.Pressed -> {
                state.holdJob?.cancel()
                if (Gesture.DOUBLE in state.gestures) {
                    val job = scope.launch {
                        delay(timing().doublePressMs)
                        states.remove(id)
                        if (Gesture.SINGLE in state.gestures) onGesture(event, Gesture.SINGLE)
                    }
                    states[id] = State.WaitingForSecond(state.gestures, job)
                } else {
                    states.remove(id)
                    if (Gesture.SINGLE in state.gestures) onGesture(event, Gesture.SINGLE)
                }
            }
            State.Consumed -> states.remove(id)
            is State.WaitingForSecond, null -> Unit
        }
    }
}

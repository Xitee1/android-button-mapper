package de.xitee.buttonmapper.gesture

import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.input.KeyAction
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.Timing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestureEngineTest {

    private val timing = Timing(doublePressMs = 300, holdMs = 500)

    private fun event(action: KeyAction) = InputKeyEvent("/dev/input/event0", "gpio-keys", 0x247, action, 0)

    private fun TestScope.engine(mapped: Set<Gesture>, fired: MutableList<Gesture>) =
        GestureEngine(this, { timing }, { mapped }, { _, g -> fired += g })

    @Test
    fun singleOnly_firesImmediatelyOnRelease() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(setOf(Gesture.SINGLE), fired)
        e.onEvent(event(KeyAction.DOWN)); e.onEvent(event(KeyAction.UP))
        assertEquals(listOf(Gesture.SINGLE), fired)
    }

    @Test
    fun singleWithDouble_waitsForWindow() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(setOf(Gesture.SINGLE, Gesture.DOUBLE), fired)
        e.onEvent(event(KeyAction.DOWN)); e.onEvent(event(KeyAction.UP))
        advanceTimeBy(299); runCurrent()
        assertEquals(emptyList<Gesture>(), fired)
        advanceTimeBy(2); runCurrent()
        assertEquals(listOf(Gesture.SINGLE), fired)
    }

    @Test
    fun doublePress() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(setOf(Gesture.SINGLE, Gesture.DOUBLE), fired)
        e.onEvent(event(KeyAction.DOWN)); e.onEvent(event(KeyAction.UP))
        advanceTimeBy(100); runCurrent()
        e.onEvent(event(KeyAction.DOWN)); e.onEvent(event(KeyAction.UP))
        advanceTimeBy(1000); runCurrent()
        assertEquals(listOf(Gesture.DOUBLE), fired)
    }

    @Test
    fun hold_suppressesSingle() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(setOf(Gesture.SINGLE, Gesture.HOLD), fired)
        e.onEvent(event(KeyAction.DOWN))
        advanceTimeBy(501); runCurrent()
        e.onEvent(event(KeyAction.REPEAT))
        e.onEvent(event(KeyAction.UP))
        advanceTimeBy(1000); runCurrent()
        assertEquals(listOf(Gesture.HOLD), fired)
    }

    @Test
    fun shortPress_withHoldMapped_isSingle() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(setOf(Gesture.SINGLE, Gesture.HOLD), fired)
        e.onEvent(event(KeyAction.DOWN))
        advanceTimeBy(200); runCurrent()
        e.onEvent(event(KeyAction.UP))
        advanceTimeBy(1000); runCurrent()
        assertEquals(listOf(Gesture.SINGLE), fired)
    }

    @Test
    fun unmappedKey_isIgnored() = runTest {
        val fired = mutableListOf<Gesture>()
        val e = engine(emptySet(), fired)
        e.onEvent(event(KeyAction.DOWN)); e.onEvent(event(KeyAction.UP))
        advanceTimeBy(1000); runCurrent()
        assertEquals(emptyList<Gesture>(), fired)
    }
}

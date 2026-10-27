@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import org.jetbrains.skiko.window.NativeWindowEvent
import kotlinx.coroutines.*
import kotlin.test.*

class NativeInputTest {
    @Test
    fun modernTextInputRequestReceivesCompositionAndCancelsItsImeSession() = runBlocking {
        val window = TestNativeWindowScope()
        val service = NativeTextInputService()
        val request = TestTextInputRequest()
        service.attach(window)
        val session = launch(start = CoroutineStart.UNDISPATCHED) { service.startInputMethod(request) }
        try {
            service.onTextEditing(NativeWindowEvent.TextEditing("😀茶", 1, 1))
            assertEquals(TextRange(2, 3), request.currentValue.selection)
            service.onTextEditing(NativeWindowEvent.TextEditing("", 0, 0))
            assertEquals("", request.currentValue.text)
            assertNull(request.currentValue.composition)
            service.onTextEditing(NativeWindowEvent.TextEditing("😀茶", 1, 1))
            service.onTextInput("中文")
            assertEquals("中文", request.currentValue.text)
            assertNull(request.currentValue.composition)
            assertEquals([2, 4, 18, 20], window.inputArea)
        }
        finally { session.cancelAndJoin() }
        service.onTextInput("ignored")
        assertEquals("中文", request.currentValue.text)
        assertEquals(1, window.inputStops)
        service.close()
    }

    @Test
    fun cancellingAnOldRequestDoesNotStopItsReplacement() = runBlocking {
        val window = TestNativeWindowScope()
        val service = NativeTextInputService()
        val first = TestTextInputRequest()
        val second = TestTextInputRequest()
        service.attach(window)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { service.startInputMethod(first) }
        val active = launch(start = CoroutineStart.UNDISPATCHED) { service.startInputMethod(second) }
        try {
            old.cancelAndJoin()
            assertEquals(0, window.inputStops)
            service.onTextInput("茶")
            assertEquals("", first.currentValue.text)
            assertEquals("茶", second.currentValue.text)
        }
        finally {
            old.cancelAndJoin()
            active.cancelAndJoin()
            service.close()
        }
        assertEquals(1, window.inputStops)
    }

    @Test
    fun unicodeImeOffsetsStayInsideUtf16Boundaries() {
        val text = "A😀茶"
        assertEquals(0, nativeUtf16Offset(text, -1))
        assertEquals(1, nativeUtf16Offset(text, 1))
        assertEquals(3, nativeUtf16Offset(text, 2))
        assertEquals(4, nativeUtf16Offset(text, Int.MAX_VALUE))
    }

    @Test
    fun modifiersRetainIndependentLocksAndRightAlt() {
        val modifiers = nativeKeyboardModifiers(0x4000 or 0x2000 or 0x1000 or 0x00C0 or 0x0003)
        assertTrue(modifiers.isCtrlPressed && modifiers.isShiftPressed && modifiers.isAltGraphPressed)
        assertTrue(modifiers.isCapsLockOn && modifiers.isNumLockOn)
        assertFalse(modifiers.isMetaPressed || modifiers.isScrollLockOn)
    }

    @Test
    fun keypadNavigationDependsOnNumLock() {
        assertEquals(Key.NumPad1, nativeKey(NativeWindowEvent.Key(89, 0, down = true, repeat = false, modifiers = 0x1000)))
        assertEquals(Key.NumPadMoveEnd, nativeKey(NativeWindowEvent.Key(89, 0, down = true, repeat = false, modifiers = 0)))
        assertEquals(Key.A, nativeKey(NativeWindowEvent.Key(4, 97, down = true, repeat = false, modifiers = 0)))
    }

    @Test
    fun imeCompositionReplacesSelectionAndCommitsSupplementaryCharacters() {
        val service = NativeTextInputService()
        val processor = EditProcessor()
        var value = TextFieldValue("ab", TextRange(1))
        processor.reset(value, null)
        service.startInput(value, ImeOptions.Default, { commands ->
            value = processor.apply(commands)
            service.updateState(null, value)
        }, {})
        service.onTextEditing(NativeWindowEvent.TextEditing("😀茶", 1, 1))
        assertEquals("a😀茶b", value.text)
        assertEquals(TextRange(3, 4), value.selection)
        assertEquals(TextRange(1, 4), value.composition)
        service.onTextInput("中文")
        assertEquals("a中文b", value.text)
        assertNull(value.composition)
        service.close()
    }

    @Test
    fun cancelledCompositionFinishesWithoutDeletingCommittedText() {
        val commands = mutableListOf<EditCommand>()
        val service = NativeTextInputService()
        service.startInput(TextFieldValue("茶"), ImeOptions.Default, { commands.addAll(it) }, {})
        service.onTextEditing(NativeWindowEvent.TextEditing("", 0, 0))
        assertIs<FinishComposingTextCommand>(commands.single())
        service.stopInput()
        service.onTextInput("ignored")
        assertEquals(1, commands.size)
    }

    @Test
    fun singleLineEnterInvokesImeActionAndMultilineDoesNot() {
        val service = NativeTextInputService()
        var action: ImeAction? = null
        service.startInput(TextFieldValue(), ImeOptions(singleLine = true, imeAction = ImeAction.Search), {}, { action = it })
        assertTrue(service.onEnter())
        assertEquals(ImeAction.Search, action)
        service.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
        assertFalse(service.onEnter())
        service.close()
    }

    @Test
    fun candidateAreaConvertsPhysicalPixelsToWindowCoordinates() {
        val window = TestNativeWindowScope(displayScale = 2f, pixelDensity = 2f)
        val service = NativeTextInputService()
        service.attach(window)
        service.notifyFocusedRect(Rect(3f, 5f, 16f, 18f))
        assertEquals([1, 2, 7, 7], window.inputArea)
        service.notifyFocusedRect(Rect(Float.NaN, 0f, 1f, 1f))
        assertEquals([1, 2, 7, 7], window.inputArea)
        service.notifyFocusedRect(Rect(1.8f, 1.8f, 2.2f, 2.2f))
        assertEquals([0, 0, 2, 2], window.inputArea)
    }

    @Test
    fun closingInputStopsTheHostAndDropsCallbacks() {
        val window = TestNativeWindowScope()
        val service = NativeTextInputService()
        var edits = 0
        service.attach(window)
        service.startInput(TextFieldValue(), ImeOptions.Default, { edits++ }, {})
        service.close()
        service.close()
        service.onTextInput("ignored")
        assertEquals(1, window.inputStarts)
        assertEquals(1, window.inputStops)
        assertEquals(0, edits)
    }
}

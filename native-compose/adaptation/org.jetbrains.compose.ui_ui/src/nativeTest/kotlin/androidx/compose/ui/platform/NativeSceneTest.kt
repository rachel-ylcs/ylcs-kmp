@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package androidx.compose.ui.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skiko.window.NativeWindowEvent
import kotlin.test.*

class NativeSceneTest {
    @Test
    fun inputBeforeTheFirstFrameUsesTheActualWindowConstraints() {
        NativeSceneFixture().use { fixture ->
            var measured = IntSize.Zero
            fixture.content {
                Layout(content = {}) { _, constraints ->
                    assertTrue(constraints.hasBoundedWidth && constraints.hasBoundedHeight)
                    measured = IntSize(constraints.maxWidth, constraints.maxHeight)
                    layout(measured.width, measured.height) {}
                }
            }
            fixture.host.onEvent(NativeWindowEvent.Pointer(NativeWindowEvent.Pointer.Kind.ENTER, 0f, 0f, 0))
            assertEquals(IntSize(96, 64), measured)
        }
    }

    @Test
    fun realCompositionMeasuresDrawsAndReactsToSnapshotState() {
        NativeSceneFixture().use { fixture ->
            var color by mutableStateOf(Color.Red)
            var measured = IntSize.Zero
            fixture.content {
                Layout(content = {}, modifier = Modifier.drawBehind { drawRect(color) }) { _, constraints ->
                    measured = IntSize(constraints.maxWidth, constraints.maxHeight)
                    layout(measured.width, measured.height) {}
                }
            }
            fixture.render()
            assertEquals(IntSize(96, 64), measured)
            assertEquals(Color.Red.toArgb(), fixture.pixel(24, 24))
            color = Color.Blue
            fixture.render()
            assertEquals(Color.Blue.toArgb(), fixture.pixel(24, 24))
        }
    }

    @Test
    fun pointerButtonsAndScrollReachTheComposedNode() {
        NativeSceneFixture().use { fixture ->
            var primary = false
            var scroll = 0f
            fixture.content {
                Layout(content = {}, modifier = Modifier
                    .onPointerEvent(PointerEventType.Press) { primary = it.buttons.isPrimaryPressed }
                    .onPointerEvent(PointerEventType.Scroll) { scroll = it.changes.first().scrollDelta.y }) { _, _ -> layout(96, 64) {} }
            }
            fixture.render()
            fixture.host.onEvent(NativeWindowEvent.Pointer(NativeWindowEvent.Pointer.Kind.PRESS, 12f, 14f, 1, 1))
            fixture.host.onEvent(NativeWindowEvent.Pointer(NativeWindowEvent.Pointer.Kind.SCROLL, 12f, 14f, 0, scrollY = -2f))
            assertTrue(primary)
            assertEquals(-2f, scroll)
        }
    }

    @Test
    fun focusedNodeReceivesPhysicalKeysAndModifiers() {
        NativeSceneFixture().use { fixture ->
            val requester = FocusRequester()
            var key: Key? = null
            var control = false
            fixture.content {
                Layout(content = {}, modifier = Modifier.focusRequester(requester).onKeyEvent {
                    key = it.key
                    control = it.isCtrlPressed
                    true
                }.focusTarget()) { _, _ -> layout(96, 64) {} }
            }
            fixture.render()
            requester.requestFocus()
            fixture.host.onEvent(NativeWindowEvent.Key(4, 97, true, false, 0x0040))
            assertEquals(Key.A, key)
            assertTrue(control)
        }
    }

    @Test
    fun densityAndPhysicalWindowSizeAreProvidedToContent() {
        NativeSceneFixture(TestNativeWindowScope(displayScale = 2f, pixelDensity = 1f)).use { fixture ->
            var density = 0f
            var size = IntSize.Zero
            fixture.content {
                density = LocalDensity.current.density
                size = LocalWindowInfo.current.containerSize
            }
            fixture.render()
            fixture.render()
            assertEquals(2f, density)
            assertEquals(IntSize(96, 64), size)
        }
    }

    @Test
    fun focusLifecycleAndViewModelDisposalBelongToTheWindow() {
        val fixture = NativeSceneFixture()
        var owner: LifecycleOwner? = null
        var cleared = false
        try {
            fixture.content {
                owner = LocalLifecycleOwner.current
                val store = checkNotNull(LocalViewModelStoreOwner.current).viewModelStore
                DisposableEffect(store) {
                    store.put("test", object : ViewModel() {
                        override fun onCleared() { cleared = true }
                    })
                    onDispose {}
                }
            }
            fixture.render()
            assertEquals(Lifecycle.State.RESUMED, owner?.lifecycle?.currentState)
            fixture.host.onEvent(NativeWindowEvent.Focus(false))
            assertEquals(Lifecycle.State.STARTED, owner?.lifecycle?.currentState)
            fixture.host.onEvent(NativeWindowEvent.Focus(true))
            assertEquals(Lifecycle.State.RESUMED, owner?.lifecycle?.currentState)
        }
        finally { fixture.close() }
        assertEquals(Lifecycle.State.DESTROYED, owner?.lifecycle?.currentState)
        assertTrue(cleared)
    }

    @Test
    fun frameClockAndEffectsAreDrivenAndCancelledByTheHost() {
        val fixture = NativeSceneFixture()
        var frame = 0L
        var disposed = 0
        var cancelled = false
        try {
            fixture.content {
                DisposableEffect(Unit) { onDispose { disposed++ } }
                LaunchedEffect(Unit) {
                    try {
                        frame = withFrameNanos { it }
                        awaitCancellation()
                    }
                    finally { cancelled = true }
                }
            }
            fixture.render()
            fixture.render()
            assertTrue(frame > 0L)
        }
        finally { fixture.close() }
        fixture.host.close()
        assertEquals(1, disposed)
        assertTrue(cancelled)
    }

    @Test
    fun escapeDismissesTheTopDialogWithoutClosingTheWindow() {
        NativeSceneFixture().use { fixture ->
            var dismissed = 0
            fixture.content {
                Dialog(onDismissRequest = { dismissed++ }) {
                    Layout(content = {}) { _, _ -> layout(32, 24) {} }
                }
            }
            fixture.render()
            fixture.render()
            fixture.host.onEvent(NativeWindowEvent.Key(41, 27, true, false, 0))
            fixture.host.onEvent(NativeWindowEvent.Key(41, 27, false, false, 0))
            assertEquals(1, dismissed)
            assertEquals(0, fixture.window.closes)
        }
    }

    @Test
    fun closeVetoIsRespectedAndASecondHostCanOpenAfterDisposal() {
        var accept = false
        NativeSceneFixture(onCloseRequest = { accept }).use { fixture ->
            fixture.host.onEvent(NativeWindowEvent.CloseRequested)
            assertEquals(0, fixture.window.closes)
            accept = true
            fixture.host.onEvent(NativeWindowEvent.CloseRequested)
            assertEquals(1, fixture.window.closes)
        }
        NativeSceneFixture().use { fixture ->
            fixture.content { Layout(content = {}) { _, _ -> layout(10, 10) {} } }
            fixture.render()
        }
    }
}

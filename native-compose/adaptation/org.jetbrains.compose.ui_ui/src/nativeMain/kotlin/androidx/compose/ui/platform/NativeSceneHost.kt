@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SkiaCanvasHolder
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import androidx.navigationevent.DirectNavigationEventInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.window.NativeWindowCursor
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowScope
import kotlin.concurrent.Volatile

/** Owns one composition, scheduler and architecture owner; all scene calls stay on the host thread. */
internal class NativeSceneHost(private val onCloseRequest: () -> Boolean = { true }) : AutoCloseable {
    private val dispatcher = nativeUiDispatcher().claim()
    private val owner: DefaultArchitectureComponentsOwner
    private val navigationInput = DirectNavigationEventInput()
    private val info = WindowInfoImpl()
    private val textInput = NativeTextInputService()
    private val canvasHolder = SkiaCanvasHolder()
    private val rendering = SingleComposeSceneRenderingScope(::invalidate)
    private val recomposer: FrameRecomposer
    private val scene: ComposeScene
    private val drops = mutableListOf<NativeWindowEvent.Drop>()
    private var closed = false
    @Volatile
    private var window: NativeWindowScope? = null

    private val platform = object : PlatformContext.Empty() {
        override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = owner
        override val windowInfo: WindowInfo get() = info
        override val taskDispatchers: TaskDispatchers = object : TaskDispatchers {
            override val Default = Dispatchers.Default
            override val IO = Dispatchers.IO
        }
        override val textInputService get() = textInput
        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing = textInput.startInputMethod(request)
        override fun setPointerIcon(pointerIcon: PointerIcon) {
            window?.setCursor((pointerIcon as? NativePointerIcon)?.cursor ?: NativeWindowCursor.DEFAULT)
        }
    }

    init {
        var createdOwner: DefaultArchitectureComponentsOwner? = null
        var createdRecomposer: FrameRecomposer? = null
        try {
            owner = DefaultArchitectureComponentsOwner(enforceMainThread = false)
            createdOwner = owner
            owner.enableSavedStateHandles()
            owner.navigationEventDispatcher.addInput(navigationInput)
            owner.setLifecycleState(Lifecycle.State.CREATED)
            recomposer = FrameRecomposer(dispatcher.context, ::invalidate)
            createdRecomposer = recomposer
            scene = CanvasLayersComposeScene(recomposer, platformContext = platform,
                invalidateLayout = rendering::onSceneInvalidation, invalidateDraw = rendering::onSceneInvalidation)
        }
        catch (error: Throwable) {
            val cleanup: List<() -> Unit> = [{ createdRecomposer?.close() },
                { createdOwner?.let { if (it.lifecycle.currentState != Lifecycle.State.INITIALIZED) it.setLifecycleState(Lifecycle.State.DESTROYED) } },
                { createdOwner?.viewModelStore?.clear() }, { createdOwner?.navigationEventDispatcher?.dispose() }, dispatcher::close]
            for (action in cleanup) {
                try { action() }
                catch (failure: Throwable) { if (failure !== error) error.addSuppressed(failure) }
            }
            throw error
        }
    }

    fun attach(window: NativeWindowScope) {
        check(!closed && this.window == null) { "Native scene host is already attached or closed" }
        this.window = window
        dispatcher.wake = window::invalidate
        textInput.attach(window)
        info.isWindowFocused = true
        updateGeometry(window.pixelWidth, window.pixelHeight)
        owner.setLifecycleState(Lifecycle.State.RESUMED)
        invalidate()
    }

    fun setContent(content: @Composable () -> Unit) {
        check(!closed)
        scene.setContent(content = content)
    }

    fun render(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        check(!closed)
        updateGeometry(width, height)
        dispatcher.runCurrent()
        with(rendering) {
            canvasHolder.drawInto(canvas) { scene.render(recomposer, this@drawInto, nanoTime) }
        }
        dispatcher.runCurrent()
        textInput.updateInputArea()
    }

    fun onEvent(event: NativeWindowEvent) {
        if (closed) return
        window?.let { updateGeometry(it.pixelWidth, it.pixelHeight) }
        dispatcher.runCurrent()
        when (event) {
            NativeWindowEvent.CloseRequested -> if (onCloseRequest()) window?.close()
            is NativeWindowEvent.Focus -> {
                info.isWindowFocused = event.focused
                if (!event.focused) {
                    scene.cancelPointerInput()
                    info.keyboardModifiers = PointerKeyboardModifiers()
                }
                owner.setLifecycleState(if (event.focused) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
            }
            is NativeWindowEvent.Key -> {
                val modifiers = nativeKeyboardModifiers(event.modifiers)
                info.keyboardModifiers = modifiers
                val consumed = scene.sendKeyEvent(KeyEvent(key = nativeKey(event), type = if (event.down) KeyEventType.KeyDown else KeyEventType.KeyUp,
                    codePoint = event.codePoint, isCtrlPressed = modifiers.isCtrlPressed, isMetaPressed = modifiers.isMetaPressed,
                    isAltPressed = modifiers.isAltPressed || modifiers.isAltGraphPressed, isShiftPressed = modifiers.isShiftPressed, nativeEvent = event))
                if (!consumed && event.down && (event.scanCode == 40 || event.scanCode == 88)) textInput.onEnter()
                if (!consumed && event.down && !event.repeat && event.scanCode == 41 && navigationInput.hasEnabledBackHandlers) navigationInput.backCompleted()
            }
            is NativeWindowEvent.Pointer -> pointer(event)
            is NativeWindowEvent.TextInput -> textInput.onTextInput(event.text)
            is NativeWindowEvent.TextEditing -> textInput.onTextEditing(event)
            NativeWindowEvent.DropStarted -> drops.clear()
            is NativeWindowEvent.Drop -> drops.add(event)
            NativeWindowEvent.DropCompleted -> drop()
        }
        dispatcher.runCurrent()
        invalidate()
    }

    private fun pointer(event: NativeWindowEvent.Pointer) {
        val kind = when (event.kind) {
            NativeWindowEvent.Pointer.Kind.MOVE -> PointerEventType.Move
            NativeWindowEvent.Pointer.Kind.PRESS -> PointerEventType.Press
            NativeWindowEvent.Pointer.Kind.RELEASE -> PointerEventType.Release
            NativeWindowEvent.Pointer.Kind.SCROLL -> PointerEventType.Scroll
            NativeWindowEvent.Pointer.Kind.ENTER -> PointerEventType.Enter
            NativeWindowEvent.Pointer.Kind.EXIT -> PointerEventType.Exit
        }
        val buttons = (event.buttons and 1) or ((event.buttons and 4) shr 1) or ((event.buttons and 2) shl 1) or (event.buttons and -8)
        val modifiers = nativeKeyboardModifiers(event.modifiers)
        info.keyboardModifiers = modifiers
        val button = when (event.button) {
            0 -> null
            1 -> PointerButton.Primary
            2 -> PointerButton.Tertiary
            3 -> PointerButton.Secondary
            4 -> PointerButton.Back
            5 -> PointerButton.Forward
            else -> PointerButton(event.button - 1)
        }
        val result = scene.sendPointerEvent(kind, Offset(event.x, event.y), scrollDelta = Offset(event.scrollX, event.scrollY),
            buttons = PointerButtons(buttons), keyboardModifiers = modifiers, nativeEvent = event, button = button)
        if (!result.anyChangeConsumed && kind == PointerEventType.Release) {
            if (event.button == 4 && navigationInput.hasEnabledBackHandlers) navigationInput.backCompleted()
            else if (event.button == 5 && navigationInput.hasEnabledForwardHandlers) navigationInput.forwardCompleted()
        }
    }

    private fun drop() {
        if (drops.isEmpty()) return
        val data = DragAndDropTransferData(drops.filterNot { it.isFile }.joinToString("\n") { it.text }.ifEmpty { null }, drops.filter { it.isFile }.map { it.text })
        val last = drops.last()
        val event = DragAndDropEvent(data, Offset(last.x, last.y))
        drops.clear()
        val target = scene.rootDragAndDropNode
        if (!target.acceptDragAndDropTransfer(event)) return
        try {
            target.onStarted(event)
            target.onEntered(event)
            target.onMoved(event)
            val _ = target.onDrop(event)
        }
        finally { target.onEnded(event) }
    }

    private fun invalidate() {
        window?.invalidate()
    }

    private fun updateGeometry(width: Int, height: Int) {
        val size = IntSize(width, height)
        scene.density = Density(window?.displayScale ?: 1f)
        scene.size = size
        info.containerSize = size
        info.containerDpSize = size.toSize().toDpSize(scene.density)
    }

    override fun close() {
        if (closed) return
        closed = true
        var failure: Throwable? = null
        val cleanup: List<() -> Unit> = [scene::close, recomposer::close, dispatcher::runCurrent, textInput::close,
            { owner.setLifecycleState(Lifecycle.State.DESTROYED) }, owner.viewModelStore::clear, owner.navigationEventDispatcher::dispose, dispatcher::close]
        for (action in cleanup) {
            try { action() }
            catch (error: Throwable) {
                val previous = failure
                if (previous == null) failure = error
                else if (previous !== error) previous.addSuppressed(error)
            }
        }
        drops.clear()
        window = null
        failure?.let { throw it }
    }
}

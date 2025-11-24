package love.yinlin.compose.game

import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Trigger

@Stable
abstract class Manager {
    abstract val size: Size // 画布大小
    abstract val fps: Int // 帧率

    abstract val currentTick: Long
    abstract fun onSceneComplete()

    private var scene: Spirit? = null // 主场景
    private val pointers = mutableLongObjectMapOf<Pointer>() // 指针集

    private val lock = SynchronizedObject()

    private var tickJob: Job? = null

    private fun CoroutineScope.startTickJob(): Job = launch {
        var lastTick = 0L
        while (true) {
            val tick = currentTick
            if (tick == 0L && lastTick != 0L) break
            lastTick = tick
            synchronized(lock) {
                (scene as? Dynamic)?.onUpdate(tick)
            }
            delay(1000L / fps)
        }
        tickJob = null
        scene = null
        pointers.clear()
        onSceneComplete()
    }

    protected fun onSceneCreate(spirit: Spirit) {
        scene = spirit
    }

    protected fun onScenePause() {
        tickJob?.cancel()
        tickJob = null
    }

    protected fun CoroutineScope.onSceneResume() {
        tickJob?.cancel()
        tickJob = startTickJob()
    }

    protected fun onSceneStop() {
        tickJob?.cancel()
        tickJob = null
        scene = null
        pointers.clear()
    }

    @Composable
    fun SceneContent(
        modifier: Modifier = Modifier,
        font: FontFamily,
    ) {
        BoxWithConstraints(modifier = modifier) {
            val canvasScale = with(LocalDensity.current) { maxWidth.toPx() } / size.width
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val textDrawer = remember(font, fontFamilyResolver) { TextDrawer(font, fontFamilyResolver) }

            Canvas(modifier = Modifier.fillMaxSize().clipToBounds().pointerInput(canvasScale) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        for (change in event.changes) {
                            val id = change.id.value
                            val position = change.position / canvasScale
                            val time = currentTick
                            when {
                                change.changedToDown() -> Pointer(id = id, position = position, startTime = time).let { pointer ->
                                    pointers[id] = pointer
                                    synchronized(lock) {
                                        (scene as? Trigger)?.onEvent(pointer)
                                    }
                                }
                                change.changedToUp() -> pointers.remove(id)?.let { pointer ->
                                    synchronized(lock) {
                                        (scene as? Trigger)?.onEvent(pointer.copy(endTime = time))
                                    }
                                }
                            }
                        }
                    }
                }
            }) {
                scale(scale = canvasScale, pivot = Offset.Zero) {
                    scene?.apply { onDraw(textDrawer) }
                }
            }
        }
    }
}
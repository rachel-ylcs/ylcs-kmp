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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerDownEvent
import love.yinlin.compose.game.traits.PointerUpEvent
import love.yinlin.compose.game.traits.Spirit

@Stable
abstract class Manager {
    abstract val size: Size // 画布大小
    abstract val fps: Int // 帧率

    abstract val currentTick: Long

    val assets = Assets() // 资源

    private var scene: Spirit? = null // 主场景

    private val pointers = mutableLongObjectMapOf<Offset>() // 指针集

    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    private var tickJob: Job? = null // 帧更新协程

    private fun clearEventChannel() {
        while (true) {
            if (!eventChannel.tryReceive().isSuccess) break
        }
    }

    private fun CoroutineScope.startTickJob(): Job = launch {
        scene?.let { spirit ->
            while (true) {
                val tick1 = currentTick

                // 事件处理
                while (true) spirit.onEvent(tick1, eventChannel.tryReceive().getOrNull() ?: break)
                // 帧更新
                spirit.onUpdate(tick1)

                val tick2 = currentTick

                val compensation = tick2 - tick1

                delay(1000L / fps - compensation)
            }
        }
        tickJob = null
        scene = null
        pointers.clear()
        clearEventChannel()
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
        clearEventChannel()
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
                            when {
                                change.changedToDown() -> {
                                    pointers[id] = position
                                    eventChannel.trySend(PointerDownEvent(id = id, position = position))
                                }
                                change.changedToUp() -> {
                                    pointers.remove(id)?.let { rawPosition ->
                                        // 抬起时的位置仍然按原来按下的位置计算
                                        eventChannel.trySend(PointerUpEvent(id = id, position = rawPosition))
                                    }
                                }
                            }
                        }
                    }
                }
            }) {
                scale(scale = canvasScale, pivot = Offset.Zero) {
                    scene?.apply { Drawer(this@scale, textDrawer).onDraw() }
                }
            }
        }
    }
}
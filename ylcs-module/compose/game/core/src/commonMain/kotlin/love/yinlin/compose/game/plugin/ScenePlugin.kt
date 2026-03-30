package love.yinlin.compose.game.plugin

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Camera
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.common.FontProvider
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer

@Stable
class ScenePlugin private constructor(
    private val fpsRate: Long,
    engine: Engine
) : Plugin(engine) {
    companion object {
        /**
         * @param fpsRate FPS统计频率(毫秒)
         */
        fun build(
            fpsRate: Long = 1000L
        ): (Engine) -> ScenePlugin = { engine ->
            ScenePlugin(fpsRate, engine)
        }
    }

    /**
     * 引擎时间刻
     */
    private var engineTime: Long = 0L
    private var lastRunningTime: Long = 0L

    /**
     * FPS
     */
    var fps: Int by mutableIntStateOf(0)
        private set

    // 相机
    val camera = Camera()

    // 实体
    private val entities = mutableStateListOf<Entity>()

    // 动态实体 - 更新
    private val dynamicEntities by derivedStateOf {
        entities.fastMapNotNull { it as? Dynamic }
    }

    // 绘制层实体 - 渲染
    private val layerEntities by derivedStateOf {
        entities.fastMapNotNull { it as? Layer }.sortedBy(Layer::layerOrder)
    }

    val isEmpty: Boolean get() = entities.isEmpty()
    val isNotEmpty: Boolean get() = entities.isNotEmpty()
    val entityCount: Int get() = entities.size

    operator fun plusAssign(entity: Entity) {
        entities += entity
        if (isInitialized) entity.onAttached(engine)
    }

    operator fun minusAssign(entity: Entity) {
        entities -= entity
        if (isInitialized) entity.onDetached(engine)
    }

    fun clear() {
        entities.clear()
        entities.fastForEachReversed { it.onDetached(engine) }
    }

    // 事件
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)

    // 清除事件
    private fun clearEventChannel() {
        while (true) {
            if (!eventChannel.tryReceive().isSuccess) break
        }
    }

    // 指针事件监听
    private suspend fun PointerInputScope.pointerInputLoop() {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val trackedId = firstDown.id

            // 指针按下
            val originPosition = camera.transformPointer(firstDown.position, size.toSize())
            eventChannel.trySend(Event.Pointer.Down(originPosition))

            // 持续跟踪指针
            while (true) {
                val event = awaitPointerEvent()

                val change = event.changes.find { it.id == trackedId }
                if (change == null || !change.pressed) { // 指针丢失或抬起
                    val upPosition = change?.position?.let { camera.transformPointer(it, size.toSize()) } ?: Offset.Zero
                    eventChannel.trySend(Event.Pointer.Up(upPosition, originPosition))
                    break
                }
                else { // 移动
                    if (change.positionChange() != Offset.Zero) {
                        val movePosition = camera.transformPointer(change.position, size.toSize())
                        eventChannel.trySend(Event.Pointer.Move(movePosition, originPosition))
                    }
                    change.consume()
                }
            }
        }
    }

    // 游戏循环
    private suspend fun CoroutineScope.engineLoop() {
        var lastTime = withFrameMillis { it }
        engineTime = lastTime - lastRunningTime

        var frameCount = 0L
        var lastFpsTime = lastTime

        try {
            while (isActive) {
                withFrameMillis { frameTime ->
                    // 每秒更新一次 FPS
                    lastTime = frameTime
                    val deltaFPSTime = frameTime - lastFpsTime
                    if (fpsRate in 1 ..< deltaFPSTime) {
                        fps = if (frameCount == 0L) 0 else (frameCount * 1000 / deltaFPSTime).toInt()
                        lastFpsTime = frameTime
                        frameCount = 0L
                    }
                    ++frameCount

                    // 更新游戏刻
                    val deltaTime = frameTime - engineTime

                    // 处理事件
                    while (true) {
                        // 事件转换
                        val event = eventChannel.tryReceive().getOrNull() ?: break
                        // 事件处理层级是逆向的，与渲染顺序相反
                        for (index in layerEntities.indices.reversed()) {
                            val layer = layerEntities[index]
                            if (layer.interactive) { // 可交互的层
                                if (layer.triggerVisibleLayer(deltaTime, event)) break // 消费完成
                            }
                        }
                    }

                    // 更新动态层
                    dynamicEntities.fastForEach { dynamic ->
                        if (dynamic.active) { // 已激活的层
                            dynamic.onUpdate(deltaTime)
                        }
                    }
                }
            }
        } finally {
            // 暂停记录累积刻
            lastRunningTime = lastTime - engineTime
        }
    }

    override fun onRelease() {
        clear()
        clearEventChannel()
    }

    // 渲染

    override val layerOrder: Int = LayerOrder.GameSurface

    @Composable
    override fun BoxScope.Content() {
        // 游戏时间刻
        LaunchedEffect(isInitialized, engine.isRunning) {
            if (isInitialized && engine.isRunning) engineLoop()
        }

        // 游戏画布
        Box(modifier = Modifier.fillMaxSize().onSizeChanged { // 相机坐标转换
            camera.updateViewport(it, engine.viewport)
        }.pointerInput(Unit) { // 事件监听
            pointerInputLoop()
        }.graphicsLayer { // 窗口坐标转换
            val _ = camera.requireDirty
            camera.transformLayer(this, size)
        }) {
            // 画布渲染
            val fontProvider = remember { engine.pluginOrNull<FontPlugin>()?.fontProvider ?: FontProvider.Default }
            val fontFamilyResolver = LocalFontFamilyResolver.current

            val cameraState by rememberDerivedState {
                val _ = camera.requireDirty
                camera.viewportSize to camera.viewportBounds
            }

            layerEntities.fastForEach { layer ->
                key(layer.id) {
                    val drawer = remember(fontFamilyResolver) {
                        Drawer(
                            fontFamilyResolver = fontFamilyResolver,
                            fontProvider = fontProvider
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                        val (viewportSize, bounds) = cameraState
                        val _ = layer.requireDirty
                        val layerVisible = layer.visible

                        // 预绘制处理
                        drawer.withRawCacheScope(this) {
                            with(layer) {
                                if (layerVisible) drawer.prepareDrawVisibleLayer(viewportSize, bounds)
                            }
                        }

                        // 绘制
                        onDrawBehind {
                            drawer.withRawScope(this) {
                                with(layer) {
                                    if (layerVisible) drawer.drawVisibleLayer()
                                }
                            }
                        }
                    }.zIndex(layer.layerOrder.toFloat()))
                }
            }
        }
    }
}
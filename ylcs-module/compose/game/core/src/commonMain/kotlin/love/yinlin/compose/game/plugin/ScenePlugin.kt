package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.asset.AssetProvider
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.font.FontProvider
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer
import kotlin.uuid.ExperimentalUuidApi

@Stable
class ScenePlugin private constructor(
    private val fpsRate: Long,
    cameraConfig: Camera.Config,
    override val extraModifier: Modifier,
    engine: Engine
) : Plugin(engine) {
    /**
     * @param fpsRate FPS统计频率(毫秒)，为0表示禁用
     */
    @Stable
    class Factory(
        val fpsRate: Long = 1000L,
        val cameraConfig: Camera.Config = Camera.Config(),
        val extraModifier: Modifier = Modifier,
    ) : PluginFactory {
        override fun build(engine: Engine): Plugin = ScenePlugin(fpsRate, cameraConfig, extraModifier, engine)
    }

    /**
     * FPS
     */
    var fps: Int by mutableIntStateOf(0)
        private set

    // 相机
    val camera = Camera(cameraConfig)

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
        if (isInitialized) entity.onAttached(this)
    }

    operator fun plusAssign(items: Iterable<Entity>) {
        entities += items
        if (isInitialized) {
            for (item in items) {
                item.onAttached(this)
            }
        }
    }

    operator fun minusAssign(entity: Entity) {
        entities -= entity
        if (isInitialized) entity.onDetached(this)
    }

    operator fun minusAssign(items: Iterable<Entity>) {
        entities -= items.toSet()
        if (isInitialized) {
            for (item in items.reversed()) {
                item.onDetached(this)
            }
        }
    }

    fun reset() {
        entities.clear()
        entities.fastForEachReversed { it.onDetached(this) }
        camera.reset()
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
        awaitPointerEventScope {
            val pointerMap = mutableMapOf<Long, Event>()

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                for (change in event.changes) {
                    val id = change.id.value
                    val position = change.position
                    val eventSize = size.toSize()

                    when {
                        // 初次按下
                        change.changedToDown() -> {
                            // 事件处理层级是逆向的，与渲染顺序相反
                            for (index in layerEntities.indices.reversed()) {
                                val layer = layerEntities[index]
                                if (layer.interactive) { // 可交互的层
                                    // 根据层类型转换坐标
                                    val transformPosition = camera.transformPointer(layer.layerType == LayerType.Absolute, position, eventSize)
                                    // 构造受击检测
                                    val (visible, arg) = layer.hitTestVisibleLayer(transformPosition) ?: continue
                                    // 消费完成
                                    val event = Event.Pointer.Down(id, transformPosition, layer, visible, arg)
                                    pointerMap[id] = event
                                    // 发送按下事件到消息队列
                                    eventChannel.trySend(event)
                                    break
                                }
                            }
                            // 空置点击不处理
                        }

                        // 抬起
                        change.changedToUp() -> {
                            // 检查是否是游离指针
                            val event = pointerMap[id] as? Event.Pointer.Down
                            if (event != null) {
                                // 移除指针
                                pointerMap.remove(id)
                                val layer = event.layer
                                val visible = event.source
                                // 转换坐标
                                val transformPosition = camera.transformPointer(layer.layerType == LayerType.Absolute, position, eventSize)
                                // 发送抬起事件到消息队列
                                eventChannel.trySend(Event.Pointer.Up(id, transformPosition, layer, visible, event.position, event.arg))
                            }
                        }

                        // 指针移动
                        change.pressed && change.positionChanged() -> {
                            // 检查是否是游离指针
                            val event = pointerMap[id] as? Event.Pointer.Down
                            if (event != null) {
                                val layer = event.layer
                                val visible = event.source
                                // 转换坐标
                                val transformPosition = camera.transformPointer(layer.layerType == LayerType.Absolute, position, eventSize)
                                // 发送移动事件到消息队列
                                eventChannel.trySend(Event.Pointer.Move(id, transformPosition, layer, visible, event.position, event.arg))
                            }
                        }
                    }
                }
            }
        }
    }

    // 游戏循环
    private suspend fun CoroutineScope.engineLoop() {
        var lastTime = withFrameMillis { it }
        var frameCount = 0L
        var lastFpsTime = lastTime

        while (isActive) {
            withFrameMillis { frameTime ->
                // 更新引擎刻
                val deltaTime = (frameTime - lastTime).toInt()
                lastTime = frameTime

                // 每秒更新一次 FPS
                val deltaFPSTime = frameTime - lastFpsTime
                if (fpsRate in 1 ..< deltaFPSTime) {
                    fps = if (frameCount == 0L) 0 else (frameCount * 1000 / deltaFPSTime).toInt()
                    lastFpsTime = frameTime
                    frameCount = 0L
                }
                ++frameCount

                // 处理事件
                while (true) {
                    // 事件转换
                    val event = eventChannel.tryReceive().getOrNull() ?: break
                    // 事件处理层级是逆向的，与渲染顺序相反
                    for (index in layerEntities.indices.reversed()) {
                        val layer = layerEntities[index]
                        if (layer.interactive) { // 可交互的层
                            if (layer.triggerVisibleLayer(event)) break // 消费完成
                        }
                    }
                }

                // 更新相机动画
                camera.updateAnimation(deltaTime)

                // 更新动态层
                dynamicEntities.fastForEach { dynamic ->
                    if (dynamic.active) { // 已激活的层
                        dynamic.onUpdate(deltaTime)
                    }
                }
            }
        }
    }

    override fun onRelease() {
        reset()
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
        }.pointerInput(Unit) { // 指针事件监听
            pointerInputLoop()
        }) {
            val density = LocalDensity.current
            val fontFamilyResolver = LocalFontFamilyResolver.current
            // 字体转接器
            val fontProvider = remember { engine.pluginOrNull<FontPlugin>()?.fontProvider ?: FontProvider.Default }
            // 资源转接器
            val assetProvider = remember { engine.pluginOrNull<AssetPlugin>()?.assetProvider ?: AssetProvider.Default }

            layerEntities.fastForEach { layer ->
                @OptIn(ExperimentalUuidApi::class)
                key(layer.id) {
                    val drawer = remember {
                        Drawer(
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                            fontProvider = fontProvider,
                            assetProvider = assetProvider
                        )
                    }

                    LaunchedEffect(Unit) {
                        with(layer) { drawer.preInitialDraw() }
                    }

                    Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                        if (layer.layerType == LayerType.Relative) camera.transformLayerRelative(this, size)
                        else camera.transformLayerAbsolute(this)
                    }.drawWithCache {
                        if (layer.layerType == LayerType.Relative) {
                            camera.whenDirty { viewportSize, bounds ->
                                layer.drawCacheVisibleLayerRelative(this, drawer, viewportSize, bounds)
                            }
                        }
                        else layer.drawCacheVisibleLayerAbsolute(this, drawer, camera.viewportSize)
                    }.zIndex(layer.layerOrder.toFloat()))
                }
            }
        }
    }
}
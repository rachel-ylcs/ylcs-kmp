package love.yinlin.compose.game.plugin

import androidx.collection.MutableLongLongMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer
import kotlin.uuid.ExperimentalUuidApi

@Stable
class ScenePlugin private constructor(
    private val fpsRate: Long,
    cameraConfig: Camera.Config,
    engine: Engine
) : Plugin(engine) {
    /**
     * @param fpsRate FPS统计频率(毫秒)，为0表示禁用
     */
    @Stable
    class Factory(
        val fpsRate: Long = 1000L,
        val cameraConfig: Camera.Config = Camera.Config(),
    ) : PluginFactory {
        override fun build(engine: Engine): Plugin = ScenePlugin(fpsRate, cameraConfig, engine)
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

    operator fun minusAssign(entity: Entity) {
        entities -= entity
        if (isInitialized) entity.onDetached(this)
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
            val pointerMap = MutableLongLongMap()

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                for (change in event.changes) {
                    val id = change.id.value
                    val position = camera.transformPointer(change.position, size.toSize())

                    when {
                        // 1. 初次按下
                        change.changedToDown() -> {
                            pointerMap[id] = position.packedValue
                            eventChannel.trySend(Event.Pointer.Down(position))
                        }

                        // 2. 抬起
                        change.changedToUp() -> {
                            val origin = pointerMap.getOrDefault(id, position.packedValue)
                            pointerMap.remove(id)
                            eventChannel.trySend(Event.Pointer.Up(position, Offset(origin)))
                        }

                        // 3. 指针移动
                        change.pressed && change.positionChanged() -> {
                            val origin = pointerMap.getOrDefault(id, position.packedValue)
                            eventChannel.trySend(Event.Pointer.Move(position, Offset(origin)))
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
                            if (layer.triggerVisibleLayer(deltaTime, event)) break // 消费完成
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
            // 注意指针监听必须放在layer前面，然后手动坐标转换
            // 如果放后面看起来坐标经layer自动转换了，但是原点在中心后非第四象限外的坐标将不接受指针事件了
            pointerInputLoop()
        }.graphicsLayer { // 窗口坐标转换
            camera.whenDirtyTransformLayer(this, size)
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

                    Box(modifier = Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                        camera.whenDirty { viewportSize, bounds ->
                            layer.whenDirty {
                                val layerVisible = layer.visible

                                // 绘制预处理
                                if (layerVisible) {
                                    drawer.withRawCacheScope(this) {
                                        layer.prepareDrawVisibleLayer(this, viewportSize, bounds)
                                    }
                                }

                                // 绘制
                                onDrawWithContent {
                                    if (layerVisible) {
                                        drawer.withRawScope(this) {
                                            layer.drawVisibleLayer(this)
                                        }
                                    }
                                }
                            }
                        }
                    }.zIndex(layer.layerOrder.toFloat()))
                }
            }
        }
    }
}
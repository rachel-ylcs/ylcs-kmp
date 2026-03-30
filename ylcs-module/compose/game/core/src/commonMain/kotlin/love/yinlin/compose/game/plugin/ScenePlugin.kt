package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Camera
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.FontProvider
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer

@Stable
class ScenePlugin(engine: Engine) : Plugin(engine) {
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
        if (isInitialized) {
            entities.clear()
            entities.fastForEachReversed { it.onDetached(engine) }
        }
    }

    override fun onRelease() = clear()

    // 事件

    // 渲染

    override val layerOrder: Int = LayerOrder.GameSurface

    @Composable
    override fun BoxScope.Content() {
        // 引擎更新
        LaunchedEffect(isInitialized, engine.isRunning) {
            if (isInitialized && engine.isRunning) {
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
                            if (deltaFPSTime > 1000L) {
                                fps = if (frameCount == 0L) 0 else (frameCount * 1000 / deltaFPSTime).toInt()
                                lastFpsTime = frameTime
                                frameCount = 0L
                            }
                            ++frameCount

                            val deltaTime = frameTime - engineTime
                            dynamicEntities.fastForEach { dynamic ->
                                if (dynamic.active) dynamic.onUpdate(deltaTime)
                            }
                        }
                    }
                } finally {
                    lastRunningTime = lastTime - engineTime
                }
            }
        }

        // 游戏画布
        Box(modifier = Modifier.fillMaxSize().onSizeChanged {
            camera.updateViewport(it, engine.viewport)
        }.graphicsLayer {
            val _ = camera.requireDirty
            camera.transformLayer(this, size)
        }) {
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
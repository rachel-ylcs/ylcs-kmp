package love.yinlin.compose.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.*
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.Plugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.window.FocusWindowEffect
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.cpuContext
import love.yinlin.reflect.metaClassName
import love.yinlin.reflect.metaRawClassName

@OptIn(CompatibleRachelApi::class)
@Stable
class Engine(
    val viewport: Viewport, // 视口类型
    vararg userPlugins: (Engine) -> Plugin, // 插件集
) {
    /**
     * 是否加载
     */
    var isInitialized: Boolean by mutableStateOf(false)
        private set

    /**
     * 是否运行
     */
    var isRunning: Boolean by mutableStateOf(false)
        private set

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

    private val scope = CoroutineScope(SupervisorJob() + cpuContext)

    private val defaultPlugins = listOf(
        FontPlugin(this),
        ScenePlugin(this),
    )

    @PublishedApi internal val plugins = (defaultPlugins + userPlugins.map { it.invoke(this) }).fastDistinctBy(Plugin::id)

    private val dynamicPlugins = plugins.fastFilter { it.dynamic }

    private val visiblePlugins = plugins.fastFilter { it.layerOrder != LayerOrder.Invisible }

    inline fun <reified T : Plugin> plugin(): T = plugins.fastFirst { it.id == metaClassName<T>() } as T
    inline fun <reified T : Plugin> pluginOrNull(): T? = plugins.fastFirstOrNull { it.id == metaClassName<T>() } as? T

    // 拓扑排序
    private val topologicInfo: List<Pair<Plugin, List<String>>> get() {
        // 默认依赖
        val defaultDependencies = defaultPlugins.fastMap { it::class }

        val ids = plugins.associateBy(Plugin::id)
        val dependenciesMap = mutableMapOf<String, List<String>>()

        // 入度表 + 邻接表
        val inDegree = mutableMapOf<String, Int>()
        val adjacencyList = mutableMapOf<String, MutableList<String>>()

        // 初始化入度表
        plugins.fastForEach { plugin ->
            val id = plugin.id
            // 为非默认插件添加默认依赖
            val actualDependencies = if (plugin in defaultPlugins) plugin.dependencies else defaultDependencies + plugin.dependencies
            val validDeps = actualDependencies.fastMapNotNull { clz ->
                val dependentId = clz.metaRawClassName
                require(dependentId in ids) { "plugin $dependentId is not installed" }
                // 排除自身依赖
                if (dependentId != id) dependentId else null
            }

            dependenciesMap[id] = validDeps
            inDegree[id] = validDeps.size

            validDeps.fastForEach { dependentId ->
                adjacencyList.getOrPut(dependentId) { mutableListOf() }.add(id)
            }

            if (validDeps.isEmpty()) inDegree[id] = 0
        }

        // 初始化队列
        val queue = ArrayDeque<String>()
        inDegree.forEach { (id, degree) ->
            if (degree == 0) queue += id
        }

        val result = mutableListOf<Pair<Plugin, List<String>>>()

        // BFS
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            result += ids[currentId]!! to dependenciesMap[currentId]!!

            adjacencyList[currentId]?.forEach { dependentId ->
                val updatedDegree = (inDegree[dependentId] ?: 0) - 1
                inDegree[dependentId] = updatedDegree
                if (updatedDegree == 0) queue += dependentId
            }
        }

        // 循环依赖
        require(result.size == plugins.size) { "plugin loop dependencies" }

        return result
    }

    /**
     * 初始化
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        val result = Coroutines.sync { future ->
            scope.launch {
                future.send {
                    coroutineScope {
                        // 依赖表
                        val taskMap = mutableMapOf<String, Deferred<Boolean>>()
                        // 先拓扑排序
                        topologicInfo.fastMap { (plugin, dependencies) ->
                            // 并行加载
                            val task = async {
                                // 等待依赖插件完成
                                for (dependentId in dependencies) {
                                    val dependencyTask = taskMap[dependentId]
                                    require(dependencyTask != null) { "dependent plugins $dependentId is not initialized" }
                                    dependencyTask.await()
                                }
                                plugin.onInitialize()
                                plugin.isInitialized
                            }
                            taskMap[plugin.id] = task
                            task
                        }.awaitAll().fastAll { it }
                    }
                }
            }
        } ?: false
        isInitialized = result
        return result
    }

    /**
     * 销毁
     */
    fun release() {
        if (isInitialized) plugins.fastForEachReversed { it.onRelease() }
        scope.cancel()
        isInitialized = false
    }

    @Composable
    fun ViewportContent(modifier: Modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(isInitialized, isRunning) {
            if (isInitialized && isRunning) {
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
                            dynamicPlugins.fastForEach { it.onUpdate(deltaTime) }
                        }
                    }
                } finally {
                    lastRunningTime = lastTime - engineTime
                }
            }
        }

        FocusWindowEffect { isFocus ->
            isRunning = isFocus
        }

        Layout(
            modifier = modifier,
            content = {
                Box(modifier = Modifier.background(Color.Black).clipToBounds()) {
                    if (isInitialized) {
                        visiblePlugins.fastForEach { plugin ->
                            Box(modifier = Modifier.fillMaxSize().zIndex(plugin.layerOrder.zIndex)) {
                                with(plugin) { Content() }
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val maxWidth = constraints.maxWidth
            val maxHeight = constraints.maxHeight

            val bounds = viewport.applyWindowBounds(IntSize(maxWidth, maxHeight))
            val placeable = measurables.first().measure(Constraints.fixed(bounds.width, bounds.height))

            layout(maxWidth, maxHeight) {
                placeable.placeRelative(bounds.left, bounds.top)
            }
        }
    }
}
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
import androidx.compose.ui.util.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.*
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.common.Viewport
import love.yinlin.compose.game.plugin.Plugin
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

    private val scope = CoroutineScope(SupervisorJob() + cpuContext)

    // 插件依赖图
    @PublishedApi
    internal val pluginMap = userPlugins.map { it.invoke(this) }.associateBy(Plugin::id)
    private val pluginDependencyMap: Map<String, List<String>>
    private val plugins: List<Plugin>
    private val visiblePlugins: List<Plugin>

    init {
        // 依赖图
        val dependenciesMap = mutableMapOf<String, List<String>>()

        // 入度表 + 邻接表
        val inDegree = mutableMapOf<String, Int>()
        val adjacencyList = mutableMapOf<String, MutableList<String>>()

        // 初始化入度表
        for ((id, plugin) in pluginMap) {
            val validDeps = plugin.dependencies.fastMapNotNull { clz ->
                val dependentId = clz.metaRawClassName
                require(dependentId in pluginMap) { "plugin $dependentId is not installed" }
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

        val result = mutableListOf<Plugin>()

        // BFS
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            result += pluginMap[currentId]!!

            adjacencyList[currentId]?.forEach { dependentId ->
                val updatedDegree = (inDegree[dependentId] ?: 0) - 1
                inDegree[dependentId] = updatedDegree
                if (updatedDegree == 0) queue += dependentId
            }
        }

        // 循环依赖
        require(result.size == pluginMap.size) { "plugin loop dependencies" }

        pluginDependencyMap = dependenciesMap
        plugins = result
        visiblePlugins = result.fastFilter { it.layerOrder != LayerOrder.Invisible }
    }

    inline fun <reified T : Plugin> plugin(): T = pluginMap[metaClassName<T>()] as T
    inline fun <reified T : Plugin> pluginOrNull(): T? = pluginMap[metaClassName<T>()] as? T

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
                        plugins.fastMap { plugin ->
                            val id = plugin.id
                            // 并行加载
                            val task = async {
                                val dependencies = pluginDependencyMap[id] ?: emptyList()
                                // 等待依赖插件完成
                                for (dependentId in dependencies) {
                                    val dependencyTask = taskMap[dependentId]
                                    require(dependencyTask != null) { "dependent plugin $dependentId is not initialized" }
                                    dependencyTask.await()
                                }
                                if (!plugin.isInitialized) {
                                    val pluginResult = plugin.onInitialize()
                                    plugin.isInitialized = pluginResult
                                    pluginResult
                                } else true
                            }
                            taskMap[id] = task
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
        if (isInitialized) {
            plugins.fastForEachReversed { plugin ->
                if (plugin.isInitialized) {
                    plugin.onRelease()
                    plugin.isInitialized = false
                }
            }
        }
        scope.cancel()
        isInitialized = false
    }

    @Composable
    fun ViewportContent(modifier: Modifier = Modifier.fillMaxSize()) {
        FocusWindowEffect { isFocus ->
            isRunning = isFocus
        }

        Layout(
            modifier = modifier,
            content = {
                Box(modifier = Modifier.background(Color.Black).clipToBounds()) {
                    visiblePlugins.fastForEach { plugin ->
                        key(plugin.id) {
                            Box(modifier = Modifier.fillMaxSize().zIndex(plugin.layerOrder.toFloat())) {
                                with(plugin) { Content() }
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val maxWidth = constraints.maxWidth
            val maxHeight = constraints.maxHeight

            val bounds = viewport.applyWindowBounds(maxWidth, maxHeight)
            val placeable = measurables.first().measure(Constraints.fixed(bounds.width, bounds.height))

            layout(maxWidth, maxHeight) {
                placeable.placeRelative(bounds.left, bounds.top)
            }
        }
    }
}
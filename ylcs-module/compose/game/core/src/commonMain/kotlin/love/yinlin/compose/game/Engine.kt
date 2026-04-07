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
import love.yinlin.collection.DependencyAnalyzer
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.compose.game.viewport.Viewport
import love.yinlin.compose.game.plugin.Plugin
import love.yinlin.compose.game.plugin.PluginFactory
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.cpuContext
import love.yinlin.reflect.metaClassName
import love.yinlin.reflect.metaRawClassName

@OptIn(CompatibleRachelApi::class)
@Stable
class Engine(
    val viewport: Viewport, // 视口类型
    val backgroundColor: Color, // 背景
    vararg userPlugins: PluginFactory, // 插件集
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

    // 插件依赖图
    @PublishedApi
    internal val pluginMap = userPlugins.map { it.build(this) }.associateBy(Plugin::id)
    private val pluginDependencyMap: Map<String, List<String>>
    private val plugins: List<Plugin>
    private val visiblePlugins: List<Plugin>

    init {
        val analyzer = DependencyAnalyzer(
            items = pluginMap.values,
            keyProvider = Plugin::id,
            dependenciesProvider = { plugin -> plugin.dependencies.map { it.metaRawClassName } }
        )

        pluginDependencyMap = analyzer.dependenciesMap
        plugins = analyzer.result
        visiblePlugins = plugins.fastFilter { it.layerOrder != LayerOrder.Invisible }
    }

    inline fun <reified T : Plugin> plugin(): T = pluginMap[metaClassName<T>()] as T
    inline fun <reified T : Plugin> pluginOrNull(): T? = pluginMap[metaClassName<T>()] as? T

    internal val scope = CoroutineScope(SupervisorJob() + cpuContext)

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
                        plugins.fastMap { plugin ->
                            val id = plugin.id
                            // 并行加载
                            val task = async {
                                val dependencies = pluginDependencyMap[id] ?: emptyList()
                                // 等待依赖插件完成
                                for (dependentId in dependencies) {
                                    val dependencyTask = taskMap[dependentId]
                                    require(dependencyTask != null) { "Dependent plugin $dependentId is not created" }
                                    dependencyTask.await()
                                }
                                if (!plugin.isInitialized) {
                                    val pluginResult = plugin.onInitialize()
                                    Coroutines.main { plugin.isInitialized = pluginResult }
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
        Layout(
            modifier = modifier,
            content = {
                Box(modifier = Modifier.background(backgroundColor).clipToBounds()) {
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
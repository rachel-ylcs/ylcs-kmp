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
import kotlinx.coroutines.*
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.OffScreenEffect
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.Plugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.coroutines.cpuContext
import love.yinlin.extension.catchingDefault
import love.yinlin.reflect.metaClassName
import love.yinlin.reflect.metaRawClassName

@OptIn(CompatibleRachelApi::class)
@Stable
class Engine(
    val viewport: Viewport, // 视口类型
    vararg plugins: (Engine) -> Plugin, // 插件集
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
     * 启动时间
     */
    var engineTime: Long by mutableLongStateOf(0L)
        private set

    /**
     * 运行时间
     */
    var runningTime: Long by mutableLongStateOf(0L)
        private set

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
    @PublishedApi internal val plugins = (defaultPlugins + plugins.map { it.invoke(this) }).fastDistinctBy(Plugin::id)

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
    fun initialize(onInitialize: suspend () -> Unit) {
        if (!isInitialized) {
            scope.launch {
                isInitialized = catchingDefault(false) {
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
                        }.awaitAll()
                        plugins.fastAll { it.isInitialized }
                    }
                }

                if (isInitialized) onInitialize()
            }
        }
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
        LaunchedEffect(isInitialized) {
            if (isInitialized) {
                var frameCount = 0L
                var lastTime = 0L

                while (isActive) {
                    withFrameMillis { frameTime ->
                        if (engineTime == 0L) {
                            engineTime = frameTime
                            runningTime = frameTime
                        }

                        // 每秒更新一次 FPS
                        val deltaFPSTime = frameTime - lastTime
                        if (deltaFPSTime > 1000L) {
                            fps = if (frameCount == 0L) 0 else (frameCount * 1000 / deltaFPSTime).toInt()
                            lastTime = frameTime
                            frameCount = 0L
                        }
                        ++frameCount

                        val deltaTime = frameTime - engineTime
                        plugins.fastForEach { it.onUpdate(deltaTime) }
                    }
                }
            }
        }

        OffScreenEffect { isForeground ->
            isRunning = isForeground
        }

        Layout(
            modifier = modifier,
            content = {
                Box(modifier = Modifier.background(Color.Black).clipToBounds()) {
                    plugins.fastForEach { with(it) { Content() } }
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
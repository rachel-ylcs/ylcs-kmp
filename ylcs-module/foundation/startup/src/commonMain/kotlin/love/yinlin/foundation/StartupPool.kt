package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import love.yinlin.collection.DependencyAnalyzer
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 启动池
 *
 * 负责管理启动服务的构建、异步初始化、读取、依赖解析
 */
open class StartupPool(rawContext: PlatformContext) : PlatformContextProvider(rawContext) {
    @PublishedApi
    internal var isClean = false
    @PublishedApi
    internal val factoryList = mutableListOf<StartupFactory<*>>()
    @PublishedApi
    internal val startupMap = mutableMapOf<String, Startup>()

    private var dependenciesMap = emptyMap<String, List<String>>()
    private var dependenciesList = emptyList<StartupFactory<*>>()

    inline fun <reified S : Startup> require(id: String): S = startupMap[id] as S

    inline fun <reified S : Startup> requireOrNull(id: String): S? = startupMap[id]?.let { startup ->
        if (startup.canSafeAccess) startup as? S else null
    }

    inline fun <reified S : Startup> requireClass(): S = startupMap[StartupID<S>()] as S

    inline fun <reified S : Startup> requireClassOrNull(): S? = startupMap[StartupID<S>()]?.let { startup ->
        if (startup.canSafeAccess) startup as? S else null
    }

    inline fun <reified S : Startup, F : StartupFactory<S>> startup(factory: F): StartupDelegate<S> {
        if (isClean) throw IllegalStateException("startup pool is already clean, don't call it outside the scope of the application")
        factoryList += factory
        return StartupDelegate { _, _ -> startupMap[factory.id] as S }
    }

    inline fun <reified S : Startup, F : StartupFactory<S>> startupOrNull(factory: F): StartupNullableDelegate<S> {
        if (isClean) throw IllegalStateException("startup pool is already clean, don't call it outside the scope of the application")
        factoryList += factory
        return StartupNullableDelegate { _, _ ->
            startupMap[factory.id]?.let { startup ->
                if (startup.canSafeAccess) startup as? S else null
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    inline fun startup(
        id: String = Uuid.generateV7().toString(),
        dependencies: List<String> = emptyList(),
        crossinline block: suspend () -> Unit
    ): StartupDelegate<Startup> = startup(StartupFactory.anonymous(id, dependencies, block))

    fun initPool(scope: CoroutineScope) {
        // 先对服务拓扑排序
        val analyzer = DependencyAnalyzer(
            items = factoryList,
            keyProvider = StartupFactory<*>::id,
            dependenciesProvider = StartupFactory<*>::dependencies
        )
        dependenciesMap = analyzer.dependenciesMap
        dependenciesList = analyzer.result
        factoryList.clear()
        isClean = true

        // 初始化
        val pool = this
        scope.launch(mainContext) {
            coroutineScope {
                // 依赖表
                val mutex = Mutex()
                val taskMap = mutableMapOf<String, Deferred<Unit>>()
                dependenciesList.map { factory ->
                    val id = factory.id
                    // 并行加载
                    val task = async(factory.dispatcher) {
                        val dependencies = dependenciesMap[id] ?: emptyList()
                        // 等待依赖服务完成
                        for (dependentId in dependencies) {
                            val dependencyTask = taskMap[dependentId]
                            require(dependencyTask != null) { "Dependent startup $dependentId is not created"}
                            dependencyTask.await()
                        }
                        val startup = factory.build(pool)
                        Coroutines.catchingNull { startup.init() } ?: throw StartupError(id, "init")
                        mutex.with { startupMap[id] = startup } // LinkedHashMap线程不安全
                    }
                    taskMap[id] = task
                    task
                }.awaitAll()
            }
        }
    }

    fun initPoolLater(scope: CoroutineScope) {
        scope.launch {
            coroutineScope {
                dependenciesList.map { factory ->
                    val id = factory.id
                    // 并行加载
                    async(factory.dispatcher) {
                        val startup = startupMap[id]
                        if (startup != null) Coroutines.catchingNull { startup.initLater() } ?: throw StartupError(id, "initLater")
                    }
                }.awaitAll()
            }
        }
    }

    fun destroyPoolBefore() {
        // 逆向析构
        for (factory in dependenciesList.asReversed()) {
            val id = factory.id
            val startup = startupMap[id]
            if (startup != null) catchingNull { startup.destroyBefore() } ?: throw StartupError(id, "destroyBefore")
        }
    }

    fun destroyPool() {
        // 逆向析构
        for (factory in dependenciesList.asReversed()) {
            val id = factory.id
            val startup = startupMap[id]
            if (startup != null) catchingNull { startup.destroy() } ?: throw StartupError(id, "destroy")
        }
    }
}
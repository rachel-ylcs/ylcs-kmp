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
import love.yinlin.coroutines.cpuContext
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingNull
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 启动池
 *
 * 负责管理启动服务的构建、异步初始化、读取、依赖解析
 */
open class StartupPool(
    rawContext: PlatformContext,
    @PublishedApi internal val startupMap: StartupMap,
) : PlatformContextProvider(rawContext) {
    @PublishedApi
    internal var isClean = false
    @PublishedApi
    internal val factoryList = mutableListOf<StartupFactory<*>>()

    private var dependenciesMap = emptyMap<String, List<String>>()
    private var dependenciesList = emptyList<StartupFactory<*>>()
    private val taskMap = mutableMapOf<String, Deferred<Unit>>()

    inline fun <reified S : Startup, F : StartupFactory<S>> startup(factory: F): StartupDelegate<S> {
        if (isClean) throw IllegalStateException("startup pool is already clean, don't call it outside the scope of the application")
        factoryList += factory
        return object : StartupDelegate<S> {
            val delegate by startupMap.delegate(factory.id)
            override fun getValue(thisRef: Any?, property: KProperty<*>): S = delegate as S
        }
    }

    inline fun <reified S : Startup, F : StartupFactory<S>> startup(factory: F, dependencies: List<String>): StartupDelegate<S> =
        startup(StartupFactoryWithDependency(factory, dependencies))

    @OptIn(ExperimentalUuidApi::class)
    inline fun sync(
        id: String = Uuid.generateV7().toString(),
        dependencies: List<String> = emptyList(),
        crossinline block: () -> Unit
    ): StartupDelegate<Startup> = startup(StartupFactory.sync(id, dependencies, block))

    @OptIn(ExperimentalUuidApi::class)
    inline fun async(
        id: String = Uuid.generateV7().toString(),
        dependencies: List<String> = emptyList(),
        crossinline block: suspend () -> Unit
    ): StartupDelegate<Startup> = startup(StartupFactory.async(id, dependencies, block))

    inline fun <reified S : Startup, F : StartupFactory<S>> startupLazy(factory: F): StartupDelegate<S?> {
        if (isClean) throw IllegalStateException("startup pool is already clean, don't call it outside the scope of the application")
        factoryList += factory
        return object : StartupDelegate<S?> {
            val delegate by startupMap.delegate(factory.id)
            override fun getValue(thisRef: Any?, property: KProperty<*>): S? = delegate as? S
        }
    }

    inline fun <reified S : Startup, F : StartupFactory<S>> startupLazy(factory: F, dependencies: List<String>): StartupDelegate<S?> =
        startupLazy(StartupFactoryWithDependency(factory, dependencies))

    inline fun <reified S : Startup> require(id: String): S = startupMap[id] as S

    inline fun <reified S : Startup> requireOrNull(id: String): S? = startupMap[id] as? S

    inline fun <reified S : Startup> requireClass(): S = startupMap[StartupID<S>()] as S

    inline fun <reified S : Startup> requireClassOrNull(): S? = startupMap[StartupID<S>()] as? S

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

        // 同步服务
        val syncStartupList = dependenciesList.filter { it.dispatcher == null }
        val syncStartupSet = syncStartupList.mapTo(mutableSetOf()) { it.id } // 同步服务集合

        for (syncStartup in syncStartupList) {
            val id = syncStartup.id
            val dependencies = dependenciesMap[id] ?: emptyList()
            // 确保同步服务不依赖异步服务
            dependencies.find { it !in syncStartupSet }?.let { dependentId ->
                throw IllegalStateException("sync startup $id is dependent on async startup $dependentId")
            }
            // 初始化同步服务
            val startup = catchingNull { syncStartup.build(pool) } ?: throw StartupError(id, "init")
            startupMap[id] = startup
        }

        // 异步服务
        scope.launch(mainContext) {
            coroutineScope {
                val mutex = Mutex()
                // 遍历依赖表
                dependenciesList.mapNotNull { factory ->
                    val id = factory.id
                    val dependencies = dependenciesMap[id] ?: emptyList()
                    val dispatcher = factory.dispatcher

                    if (dispatcher != null) {
                        // 并行加载
                        val task = async(dispatcher) {
                            // 等待依赖服务完成
                            for (dependentId in dependencies) {
                                val dependencyTask = taskMap[dependentId]
                                // 依赖服务已经启动或依赖服务为同步服务
                                if (dependencyTask == null) require(dependentId in syncStartupSet) { "Dependent startup $dependentId is not created"}
                                else dependencyTask.await()
                            }
                            // 初始化异步服务
                            val startup = Coroutines.catchingNull {
                                factory.build(pool).also { it.init() }
                            } ?: throw StartupError(id, "init")
                            Coroutines.main {
                                mutex.with { startupMap[id] = startup } // LinkedHashMap线程不安全
                            }
                        }
                        taskMap[id] = task
                        task
                    }
                    else null
                }.awaitAll()
                dependenciesMap = emptyMap()
                taskMap.clear()
            }
        }
    }

    fun initPoolLater(scope: CoroutineScope) {
        scope.launch {
            coroutineScope {
                dependenciesList.map { factory ->
                    val id = factory.id
                    // 并行加载
                    async(factory.dispatcher ?: cpuContext) {
                        // 等待init完成，同步任务不影响
                        taskMap[id]?.await()
                        // 继续initLater
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
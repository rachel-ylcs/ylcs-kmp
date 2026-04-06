package love.yinlin.foundation

import kotlin.coroutines.CoroutineContext

/**
 * 服务工厂
 */
sealed interface StartupFactory<S : Startup> {
    /**
     * 唯一ID
     */
    val id: String

    /**
     * 依赖
     */
    val dependencies: List<String>

    /**
     * 协程调度器
     *
     * 为空表示同步服务
     */
    val dispatcher: CoroutineContext?

    /**
     * 构建服务
     */
    fun build(pool: StartupPool): S

    companion object {
        @PublishedApi
        internal inline fun sync(
            id: String,
            dependencies: List<String>,
            crossinline block: () -> Unit
        ): StartupFactory<Startup> = object : SyncStartupFactory<Startup>() {
            override val id: String = id
            override val dependencies: List<String> = dependencies
            override fun build(pool: StartupPool): Startup = object : SyncStartup(pool) {
                init { block() }
            }
        }

        @PublishedApi
        internal inline fun async(
            id: String,
            dependencies: List<String>,
            crossinline block: suspend () -> Unit
        ): StartupFactory<Startup> = object : AsyncStartupFactory<Startup>() {
            override val id: String = id
            override val dependencies: List<String> = dependencies
            override fun build(pool: StartupPool): Startup = object : AsyncStartup(pool) {
                override suspend fun init() = block()
            }
        }
    }
}
package love.yinlin.foundation

import love.yinlin.coroutines.cpuContext
import kotlin.coroutines.CoroutineContext

/**
 * 服务工厂
 */
interface StartupFactory<S : Startup> {
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
     */
    val dispatcher: CoroutineContext

    /**
     * 构建服务
     */
    fun build(pool: StartupPool): S


    companion object {
        @PublishedApi
        internal inline fun anonymous(
            id: String,
            dependencies: List<String>,
            crossinline block: suspend () -> Unit
        ): StartupFactory<Startup> = object : StartupFactory<Startup> {
            override val id: String = id
            override val dependencies: List<String> = dependencies
            override val dispatcher: CoroutineContext = cpuContext
            override fun build(pool: StartupPool): Startup = object : Startup(pool) {
                override suspend fun init() = block()
            }
        }
    }
}
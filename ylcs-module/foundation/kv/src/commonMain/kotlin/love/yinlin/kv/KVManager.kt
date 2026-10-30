package love.yinlin.kv

import love.yinlin.concurrent.Lock
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

/** 显式初始化应用级 KV；配置层可使用该实例，也可注入独立的 KV 实例。 */
object KVManager {
    private val lock = Lock()
    private var service: KV? = null

    val isReady: Boolean get() = lock.synchronized { service != null }

    val kv: KV get() = lock.synchronized { checkNotNull(service) { "Initialize KVManager before accessing configuration" } }

    fun initialize(context: PlatformContext, initPath: File) {
        lock.synchronized {
            check(service == null) { "KVManager is already initialized" }
            service = KV(context, initPath)
        }
    }

    fun shutdown() {
        val previous = lock.synchronized { service.also { service = null } }
        previous?.close()
    }
}

package love.yinlin.exception

import love.yinlin.concurrent.Lock

/** 应用级未捕获异常监听；shutdown 恢复注册前的处理器，不接管 KV 等日志存储资源。 */
object ExceptionManager {
    private val lock = Lock()
    private var registration: AutoCloseable? = null

    val isReady: Boolean get() = lock.synchronized { registration != null }

    fun initialize(crashKey: String, handler: ExceptionHandler) {
        lock.synchronized {
            check(registration == null) { "ExceptionManager is already initialized" }
            registration = setupPlatformExceptionHandler(crashKey, handler)
        }
    }

    fun shutdown() {
        lock.synchronized {
            registration?.close()
            registration = null
        }
    }
}

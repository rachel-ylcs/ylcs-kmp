package love.yinlin.picker

import love.yinlin.concurrent.Lock
import love.yinlin.foundation.PlatformContextProvider

/** 应用级选择器服务。注册的 context 由调用方持有，服务会读取其最新的窗口或 Activity。 */
object PickerManager {
    private val lock = Lock()
    private var service: Picker? = null

    val isReady: Boolean get() = lock.synchronized { service != null }

    val picker: Picker get() = lock.synchronized { checkNotNull(service) { "Initialize PickerManager before using the picker" } }

    fun initialize(context: PlatformContextProvider) {
        lock.synchronized {
            check(service == null) { "PickerManager is already initialized" }
            service = Picker(context)
        }
    }

    fun shutdown() {
        val previous = lock.synchronized { service.also { service = null } }
        previous?.close()
    }
}

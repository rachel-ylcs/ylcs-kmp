package love.yinlin.compose.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.image.data.ImageConfiguration
import love.yinlin.concurrent.Lock
import love.yinlin.extension.catchingError
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.PlatformContext
import love.yinlin.foundation.buildFileClient
import love.yinlin.fs.PlatformFileSystem
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

/**
 * 应用唯一的图片管理器，只能成功 initialize 一次。
 * 未就绪时图片组件显示空占位；初始化完成后自动开始加载。
 */
object ImageManager {
    private val lock = Lock()
    private var initialized = false
    private var active: ImageLoader? by mutableRefStateOf(null)
    private var client: NetClient? = null
    private var platformContext: PlatformContext? = null

    internal val loaderOrNull: ImageLoader? get() = active
    internal val loader: ImageLoader get() = lock.synchronized {
        checkNotNull(active) { "Initialize ImageManager before displaying images" }
    }

    val isReady: Boolean get() = active != null
    val configuration: ImageConfiguration get() = loader.configuration
    val context: PlatformContext get() = lock.synchronized { checkNotNull(platformContext) { "ImageManager is not initialized" } }
    val memoryCacheSize: Long get() = active?.memoryCacheSize ?: 0L

    fun initialize(context: PlatformContext, appName: String = "Rachel", configuration: ImageConfiguration = ImageConfiguration()) {
        lock.synchronized {
            check(!initialized) { "ImageManager can only be initialized once" }

            val boundConfiguration = configuration.copy(inputs = configuration.inputs.toList(), formats = configuration.formats.toList())
            val components = ImageComponents(boundConfiguration.inputs, boundConfiguration.formats)
            val network = buildFileClient()

            catchingError {
                val directory = if (platform in Platform.Web) null else boundConfiguration.cacheDirectory ?: PlatformFileSystem.cachePath(context, appName)
                val service = ImageLoader(network.delegate, components, boundConfiguration, directory, context)
                platformContext = context
                client = network
                initialized = true
                active = service
            }?.let { error ->
                network.close()
                throw error
            }
        }
    }

    suspend fun clearCache() = loader.clearCache()

    fun shutdown() {
        lock.synchronized {
            val service = active
            active = null
            service?.close()
            client?.close()
            client = null
            platformContext = null
        }
    }
}

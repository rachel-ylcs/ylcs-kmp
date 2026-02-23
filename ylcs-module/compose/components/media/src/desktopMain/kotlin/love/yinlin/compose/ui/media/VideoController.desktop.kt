package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context
import love.yinlin.platform.NativeLibLoader
import love.yinlin.platform.Platform
import love.yinlin.media.LinuxVideoController
import love.yinlin.media.MacOSVideoController
import love.yinlin.platform.platform
import love.yinlin.media.WindowsVideoController
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Paint

@Stable
@NativeLibApi
actual abstract class VideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoState(context, topBar, bottomBar) {
    internal companion object {
        init {
            NativeLibLoader.resource("media")
        }

        fun build(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?): VideoController = when (platform) {
            Platform.Windows -> WindowsVideoController(context, topBar, bottomBar)
            Platform.MacOS -> MacOSVideoController(context, topBar, bottomBar)
            else -> LinuxVideoController(context, topBar, bottomBar)
        }
    }

    internal val paint = Paint().apply { this.isAntiAlias = true }

    internal var updateCount by mutableLongStateOf(0L)
    var image: Bitmap? = null
        protected set

    protected var isRelease = false

    protected var nativeHandle: Long = nativeCreate()
        private set

    protected abstract fun nativeCreate(): Long
    protected abstract fun nativeRelease(handle: Long)

    actual override fun release() {
        if (!isRelease) {
            isRelease = true
            nativeRelease(nativeHandle)
            nativeHandle = 0L
            image?.let {
                if (!it.isClosed) it.close()
            }
            image = null
            if (!paint.isClosed) paint.close()
        }
    }
}
package love.yinlin.startup

import com.github.panpf.sketch.Sketch
import love.yinlin.service.PlatformContext

actual fun buildSketch(context: PlatformContext): Sketch {
    return Sketch.Builder(com.github.panpf.sketch.PlatformContext.INSTANCE).build()
}
package love.yinlin.startup

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import love.yinlin.Context

actual fun buildSketch(context: Context): Sketch.Builder = Sketch.Builder(PlatformContext.INSTANCE)
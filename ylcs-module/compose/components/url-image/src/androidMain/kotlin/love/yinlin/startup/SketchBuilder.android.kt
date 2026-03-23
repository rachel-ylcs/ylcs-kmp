package love.yinlin.startup

import com.github.panpf.sketch.Sketch
import love.yinlin.foundation.PlatformContext

actual fun buildSketch(context: PlatformContext): Sketch.Builder = Sketch.Builder(context)
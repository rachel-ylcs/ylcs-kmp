package love.yinlin.startup

import com.github.panpf.sketch.Sketch
import love.yinlin.foundation.PlatformContext

internal expect fun buildSketch(context: PlatformContext): Sketch.Builder
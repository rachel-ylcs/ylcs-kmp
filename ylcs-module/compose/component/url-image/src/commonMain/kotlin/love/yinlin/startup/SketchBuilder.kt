package love.yinlin.startup

import com.github.panpf.sketch.Sketch
import love.yinlin.service.PlatformContext

internal expect fun buildSketch(context: PlatformContext): Sketch.Builder
package love.yinlin.startup

import com.github.panpf.sketch.Sketch
import love.yinlin.foundation.Context

actual fun buildSketch(context: Context): Sketch.Builder = Sketch.Builder(context.application)
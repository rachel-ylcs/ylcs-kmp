package love.yinlin.nativecompose.offscreen

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation

internal const val SkottieSampleJson = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"nm":"Native fixture","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Blue square","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[8,8,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"sw":32,"sh":32,"sc":"#0000ff","ip":0,"op":60,"st":0,"bm":0}]}"""

internal fun drawSkottieSample(canvas: Canvas) {
    Animation.makeFromString(SkottieSampleJson).use { animation ->
        animation.seek(0.5f).render(canvas, Rect.makeXYWH(548f, 240f, 64f, 64f))
    }
}

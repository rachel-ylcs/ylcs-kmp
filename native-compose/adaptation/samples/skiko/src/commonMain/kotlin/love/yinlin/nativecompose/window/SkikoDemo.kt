package love.yinlin.nativecompose.window

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation

fun drawSkikoDemo(canvas: Canvas, width: Int, height: Int) {
    canvas.clear(Color.WHITE)
    Paint().use { paint ->
        paint.color = Color.makeRGB(30, 105, 190)
        canvas.drawRect(Rect.makeXYWH(24f, 24f, width - 48f, height / 2f), paint)
        val fonts = FontMgr.default
        val families: Array<String?> = ["Arial", "DejaVu Sans", "Liberation Sans", "Noto Sans"]
        val face = fonts.matchFamiliesStyle(families, FontStyle.NORMAL) ?: if (fonts.familiesCount > 0) fonts.matchFamilyStyle(fonts.getFamilyName(0), FontStyle.NORMAL) else null
        face?.use { face ->
            Font(face, 26f).use { font ->
                paint.color = Color.BLACK
                canvas.drawString("Skiko Native + SDL3 | resize / close", 24f, height - 40f, font, paint)
            }
        } ?: error("No system font available")
    }
    Animation.makeFromString(DemoAnimation).use { animation ->
        animation.seek(0.5f).render(canvas, Rect.makeXYWH(width - 116f, 28f, 88f, 88f))
    }
}

private const val DemoAnimation = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"nm":"Native fixture","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Blue square","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[8,8,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"sw":32,"sh":32,"sc":"#0000ff","ip":0,"op":60,"st":0,"bm":0}]}"""
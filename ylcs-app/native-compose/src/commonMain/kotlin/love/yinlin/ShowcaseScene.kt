package love.yinlin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skia.skottie.Animation
import org.jetbrains.skia.svg.SVGDOM
import kotlin.math.sin

/** Owns the native drawing resources for one window; close before destroying its renderer. */
internal class ShowcaseScene {
    private val face = checkNotNull(FontMgr.default.matchFamiliesStyle(
        ["Arial", "DejaVu Sans", "Liberation Sans", "Helvetica"], FontStyle.NORMAL))
    private val title = Font(face, 30f)
    private val text = Font(face, 15f)
    private val paint = Paint().apply { isAntiAlias = true }
    private val animation = Animation.makeFromString(AnimationJson)
    private val svg = Data.makeFromBytes(Svg.encodeToByteArray()).use { SVGDOM(it) }
    private val fonts = FontCollection().apply { setDefaultFontManager(FontMgr.default) }
    private val paragraph = TextStyle().use { textStyle ->
        textStyle.fontFamilies = ["Arial", "DejaVu Sans", "Helvetica"]
        textStyle.fontSize = 18f
        textStyle.color = Ink
        ParagraphStyle().use { style ->
            style.textStyle = textStyle
            ParagraphBuilder(style, fonts).use { builder ->
                builder.addText("Native text: café / Ελληνικά / אבג\nSkia shapes, ICU and bidirectional layout.")
                builder.build()
            }
        }
    }
    private val image = Surface.makeRasterN32Premul(160, 80).use { surface ->
        surface.canvas.clear(Teal)
        paint.color = Amber
        surface.canvas.drawCircle(80f, 40f, 28f, paint)
        surface.makeImageSnapshot().use { snapshot ->
            checkNotNull(snapshot.encodeToData(EncodedImageFormat.WEBP)).use { data ->
                Image.makeFromEncoded(data.bytes)
            }
        }
    }
    var paused = false
    private var seconds = 0f

    fun advance(deltaSeconds: Float) {
        if (!paused) seconds += deltaSeconds.coerceIn(0f, .1f)
    }

    fun render(canvas: Canvas, width: Int, height: Int) {
        canvas.clear(Background)
        canvas.save()
        try {
            val viewport = Constraints(maxWidth = width, maxHeight = height).constrain(IntSize(width, height))
            val designSize = with(Density(1f)) { Size(1000.dp.toPx(), 660.dp.toPx()) }
            val scale = minOf(viewport.width / designSize.width, viewport.height / designSize.height)
            val origin = Offset((viewport.width - designSize.width * scale) / 2f, (viewport.height - designSize.height * scale) / 2f)
            trace("preview-transform") { canvas.translate(origin.x, origin.y) }
            canvas.scale(scale, scale)
            paint.color = Teal
            canvas.drawCircle(48f, 48f, 9f, paint)
            label(canvas, "Rachel / Native graphics lab", 72f, 59f, title)
            label(canvas, "Skiko Core + Skottie  |  SDL3  |  Kotlin/Native", 36f, 96f)

            card(canvas, 36f, 126f, 448f, 216f, "01  Vector drawing + SVG")
            canvas.save()
            canvas.translate(66f, 184f)
            svg.render(canvas)
            canvas.restore()
            paint.color = Teal
            for (index in 0 ..< 14) {
                val bar = 20f + 46f * (1f + sin(seconds * 2f + index * .45f))
                canvas.drawRRect(RRect.makeXYWH(236f + index * 14f, 300f - bar, 8f, bar, 4f), paint)
            }

            card(canvas, 504f, 126f, 460f, 216f, "02  Lottie timeline / Skottie")
            animation.seek((seconds % 2f) / 2f).render(canvas, Rect.makeXYWH(530f, 176f, 404f, 120f))
            label(canvas, if (paused) "PAUSED  /  Space to resume" else "PLAYING  /  Space to pause", 530f, 318f)

            card(canvas, 36f, 362f, 568f, 206f, "03  Fonts + paragraph shaping")
            paragraph.layout(510f)
            paragraph.paint(canvas, 60f, 426f)

            card(canvas, 624f, 362f, 340f, 206f, "04  Image codec round trip")
            canvas.drawImage(image, 650f, 420f)
            label(canvas, "WebP encode -> decode", 650f, 540f)
            label(canvas, "Resize the window  /  Space: pause  /  Esc: close", 36f, 615f)
            label(canvas, "Current stage: Compose UI util / geometry / unit + Skiko", 36f, 640f)
        }
        finally { canvas.restore() }
    }

    private fun card(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, heading: String) {
        paint.color = Color.WHITE
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, 14f), paint)
        label(canvas, heading, x + 24f, y + 34f)
    }

    private fun label(canvas: Canvas, value: String, x: Float, y: Float, font: Font = text) {
        paint.color = Ink
        canvas.drawString(value, x, y, font, paint)
    }

    fun close() {
        image.close()
        paragraph.close()
        fonts.close()
        svg.close()
        animation.close()
        paint.close()
        text.close()
        title.close()
        face.close()
    }

    private companion object {
        val Background = Color.makeRGB(237, 242, 241)
        val Ink = Color.makeRGB(29, 55, 57)
        val Teal = Color.makeRGB(18, 126, 124)
        val Amber = Color.makeRGB(242, 173, 71)
        const val Svg = """<svg xmlns="http://www.w3.org/2000/svg" width="140" height="120" viewBox="0 0 140 120"><path d="M15 88 L70 12 L125 88 Z" fill="#127e7c"/><circle cx="70" cy="80" r="29" fill="#f2ad47"/><path d="M22 106 Q70 72 118 106" fill="none" stroke="#1d3739" stroke-width="6"/></svg>"""
        const val AnimationJson = """{"v":"5.7.4","fr":60,"ip":0,"op":120,"w":400,"h":120,"nm":"Rachel motion","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Moving tile","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":1,"k":[{"t":0,"s":[0],"e":[180]},{"t":60,"s":[180],"e":[360]},{"t":120,"s":[360]}]},"p":{"a":1,"k":[{"t":0,"s":[50,60,0],"e":[350,60,0]},{"t":60,"s":[350,60,0],"e":[50,60,0]},{"t":120,"s":[50,60,0]}]},"a":{"a":0,"k":[25,25,0]},"s":{"a":0,"k":[100,100,100]}},"sw":50,"sh":50,"sc":"#127e7c","ip":0,"op":120,"st":0,"bm":0}]}"""
    }
}

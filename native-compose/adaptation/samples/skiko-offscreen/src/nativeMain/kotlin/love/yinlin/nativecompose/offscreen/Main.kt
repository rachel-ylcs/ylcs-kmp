@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.nativecompose.offscreen

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

/** Exercises the C++ bridge, pixel readback, fonts and PNG codec without a window system. */
fun main(args: Array<String>) {
    val output = args.firstOrNull() ?: "skiko-offscreen.png"
    Surface.makeRasterN32Premul(640, 360).use { surface ->
        surface.canvas.clear(Color.WHITE)
        Paint().use { paint ->
            paint.color = Color.makeRGB(30, 105, 190)
            surface.canvas.drawRect(Rect.makeXYWH(24f, 24f, 592f, 200f), paint)
            val fonts = FontMgr.default
            val families: Array<String?> = ["Arial", "DejaVu Sans", "Liberation Sans", "Noto Sans"]
            val face = fonts.matchFamiliesStyle(families, FontStyle.NORMAL)
                ?: if (fonts.familiesCount > 0) fonts.matchFamilyStyle(fonts.getFamilyName(0), FontStyle.NORMAL) else null
            face?.use { typeface ->
                Font(typeface, 28f).use { font ->
                    paint.color = Color.BLACK
                    surface.canvas.drawString("Skiko 0.156 / Kotlin Native", 24f, 282f, font, paint)
                }
            } ?: error("No system font available; install a font before running this smoke test")
        }
        drawSkottieSample(surface.canvas)
        Bitmap().use { bitmap ->
            check(bitmap.allocPixels(ImageInfo.makeN32Premul(640, 360)))
            check(surface.readPixels(bitmap, 0, 0))
            check(bitmap.getColor(40, 40) == Color.makeRGB(30, 105, 190)) { "Skia pixel readback failed" }
            check(bitmap.getColor(0, 0) == Color.WHITE) { "Skia surface clear failed" }
            check(bitmap.getColor(564, 256) == Color.BLUE) { "Skottie Native animation rendering failed" }
        }
        surface.makeImageSnapshot().use { image ->
            checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
                val bytes = data.bytes
                val file = checkNotNull(fopen(output, "wb")) { "Cannot open output: $output" }
                try {
                    bytes.usePinned { buffer ->
                        check(fwrite(buffer.addressOf(0), 1.convert(), bytes.size.convert(), file).toLong() == bytes.size.toLong()) {
                            "Incomplete PNG write: $output"
                        }
                    }
                }
                finally { check(fclose(file) == 0) { "Cannot close output: $output" } }
                println("PASS: pixel readback, font rendering, Skottie animation and PNG encoding ($output, ${bytes.size} bytes)")
            }
        }
    }
}

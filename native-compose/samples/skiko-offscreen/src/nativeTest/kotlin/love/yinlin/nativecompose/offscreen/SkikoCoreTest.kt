package love.yinlin.nativecompose.offscreen

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skia.svg.SVGDOM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Integration tests against the real Native KLIB and embedded Skia/C++ archives. */
class SkikoCoreTest {
    @Test
    fun rasterDrawingAndPixelReadback() {
        Surface.makeRasterN32Premul(64, 64).use { surface ->
            surface.canvas.clear(Color.WHITE)
            Paint().use { paint ->
                paint.color = Color.BLUE
                surface.canvas.drawRect(Rect.makeXYWH(8f, 8f, 32f, 32f), paint)
            }
            Bitmap().use { bitmap ->
                assertTrue(bitmap.allocPixels(ImageInfo.makeN32Premul(64, 64)))
                assertTrue(surface.readPixels(bitmap, 0, 0))
                assertEquals(Color.BLUE, bitmap.getColor(16, 16))
                assertEquals(Color.WHITE, bitmap.getColor(0, 0))
            }
        }
    }

    @Test
    fun imageCodecsAndInvalidInput() {
        Surface.makeRasterN32Premul(32, 24).use { surface ->
            surface.canvas.clear(Color.RED)
            surface.makeImageSnapshot().use { image ->
                val formats: Array<EncodedImageFormat> = [EncodedImageFormat.PNG, EncodedImageFormat.JPEG, EncodedImageFormat.WEBP]
                for (format in formats) {
                    assertNotNull(image.encodeToData(format)).use { data ->
                        assertTrue(data.size > 0)
                        Image.makeFromEncoded(data.bytes).use { decoded ->
                            assertEquals(32, decoded.width)
                            assertEquals(24, decoded.height)
                        }
                    }
                }
            }
        }
        assertFailsWith<IllegalArgumentException> { Image.makeFromEncoded("invalid image".encodeToByteArray()) }
    }

    @Test
    fun svgParsingAndRendering() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect x="8" y="8" width="32" height="32" fill="#0000ff"/></svg>"""
        Data.makeFromBytes(svg.encodeToByteArray()).use { data ->
            SVGDOM(data).use { document ->
                Surface.makeRasterN32Premul(64, 64).use { surface ->
                    surface.canvas.clear(Color.WHITE)
                    document.render(surface.canvas)
                    Bitmap().use { bitmap ->
                        assertTrue(bitmap.allocPixels(ImageInfo.makeN32Premul(64, 64)))
                        assertTrue(surface.readPixels(bitmap, 0, 0))
                        assertEquals(Color.BLUE, bitmap.getColor(16, 16))
                        assertEquals(Color.WHITE, bitmap.getColor(0, 0))
                    }
                }
            }
        }
    }

    @Test
    fun fontsShapingAndParagraphLayout() {
        assertTrue(FontMgr.default.familiesCount > 0, "Install system fonts before running the Core tests")
        FontCollection().use { fonts ->
            fonts.setDefaultFontManager(FontMgr.default)
            TextStyle().use { textStyle ->
                textStyle.fontFamilies = ["Arial", "DejaVu Sans", "Liberation Sans", "Helvetica"]
                textStyle.fontSize = 20f
                ParagraphStyle().use { style ->
                    style.textStyle = textStyle
                    ParagraphBuilder(style, fonts).use { builder ->
                        builder.addText("Rachel café אבג\nNative paragraph")
                        builder.build().use { paragraph ->
                            paragraph.layout(320f)
                            assertTrue(paragraph.height > 0f)
                            assertTrue(paragraph.longestLine > 0f)
                            assertTrue(paragraph.lineMetrics.size >= 2)
                            Surface.makeRasterN32Premul(320, 128).use { surface ->
                                surface.canvas.clear(Color.WHITE)
                                paragraph.paint(surface.canvas, 0f, 0f)
                            }
                        }
                    }
                }
            }
        }
    }
}

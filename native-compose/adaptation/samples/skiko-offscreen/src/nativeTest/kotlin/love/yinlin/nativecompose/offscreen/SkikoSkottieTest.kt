package love.yinlin.nativecompose.offscreen

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Exercises the separate Skottie KLIB, its C++ bridge and its transitive Core dependency. */
class SkikoSkottieTest {
    @Test
    fun animationSeeksAndRendersPixels() {
        // A self-contained Lottie solid layer; it needs neither external assets nor fonts.
        Animation.makeFromString(SkottieSampleJson).use { animation ->
            assertEquals(64f, animation.width)
            assertEquals(64f, animation.height)
            Surface.makeRasterN32Premul(64, 64).use { surface ->
                val times: FloatArray = [0f, 0.5f]
                for (time in times) {
                    surface.canvas.clear(Color.WHITE)
                    animation.seek(time).render(surface.canvas)
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
    fun invalidAnimationHasDefinedFailure() {
        assertFailsWith<IllegalArgumentException> { Animation.makeFromString("invalid animation") }
        assertFailsWith<IllegalArgumentException> { Animation.makeFromString("{}") }
    }
}

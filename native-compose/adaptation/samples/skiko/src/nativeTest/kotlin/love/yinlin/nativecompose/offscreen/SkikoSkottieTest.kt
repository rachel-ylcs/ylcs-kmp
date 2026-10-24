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
                val times: FloatArray = [0f, 0.75f]
                for (time in times) {
                    surface.canvas.clear(Color.WHITE)
                    animation.seek(time).render(surface.canvas)
                    Bitmap().use { bitmap ->
                        assertTrue(bitmap.allocPixels(ImageInfo.makeN32Premul(64, 64)))
                        assertTrue(surface.readPixels(bitmap, 0, 0))
                        assertEquals(Color.BLUE, bitmap.getColor(if (time == 0f) 16 else 48, 16))
                        assertEquals(Color.WHITE, bitmap.getColor(if (time == 0f) 48 else 16, 16))
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

private const val SkottieSampleJson = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"nm":"Native fixture","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Blue square","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":1,"k":[{"t":0,"s":[8,8,0],"h":1},{"t":30,"s":[40,8,0],"h":1},{"t":60,"s":[8,8,0]}]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"sw":16,"sh":32,"sc":"#0000ff","ip":0,"op":60,"st":0,"bm":0}]}"""
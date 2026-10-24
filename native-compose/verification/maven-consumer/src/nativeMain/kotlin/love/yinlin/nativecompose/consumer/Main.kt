@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.nativecompose.consumer

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation
import platform.posix.*

/** Runs without the source build, SDL SDK, samples or adaptation source directories. */
fun main(args: Array<String>) {
    Surface.makeRasterN32Premul(64, 64).use { surface ->
        surface.canvas.clear(Color.WHITE)
        Animation.makeFromString(Fixture).use { animation -> animation.seek(.5f).render(surface.canvas) }
        Bitmap().use { pixels ->
            check(pixels.allocPixels(ImageInfo.makeN32Premul(64, 64)))
            check(surface.readPixels(pixels, 0, 0))
            check(pixels.getColor(16, 16) == Color.BLUE)
            check(pixels.getColor(0, 0) == Color.WHITE)
        }
        surface.makeImageSnapshot().use { image ->
            checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
                Image.makeFromEncoded(data.bytes).use { decoded -> check(decoded.width == 64 && decoded.height == 64) }
                val bytes = data.bytes
                val output = checkNotNull(fopen("maven-consumer.png", "wb"))
                try {
                    bytes.usePinned { check(fwrite(it.addressOf(0), 1.convert(), bytes.size.convert(), output).toLong() == bytes.size.toLong()) }
                }
                finally { check(fclose(output) == 0) }
            }
        }
    }
    println("PASS: Maven-only Skottie -> Core; animation pixels, PNG encode/decode and output")
    if ("--window" in args) runConsumerWindow()
}

private const val Fixture = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Blue","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[8,8,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"sw":32,"sh":32,"sc":"#0000ff","ip":0,"op":60,"st":0,"bm":0}]}"""

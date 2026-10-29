import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.GalleryApplication
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<NativeGalleryApplication>()

private class NativeGalleryApplication(override val commandArgs: Array<String>) : GalleryApplication<NativeGalleryApplication>(applicationReference, PlatformContext.Instance) {
    override val title = "Rachel UI Gallery"
    override val initSize = DpSize(1280.dp, 768.dp)
}

fun main(args: Array<String>) = NativeGalleryApplication(args).run()

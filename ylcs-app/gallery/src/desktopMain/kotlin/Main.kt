import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.GalleryApplication
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<DesktopGalleryApplication>()

private class DesktopGalleryApplication(override val commandArgs: Array<String>) : GalleryApplication<DesktopGalleryApplication>(applicationReference, PlatformContext.Instance) {
    override val title = "Rachel UI Gallery"
    override val initSize = DpSize(1280.dp, 768.dp)
}

fun main(args: Array<String>) = DesktopGalleryApplication(args).run()

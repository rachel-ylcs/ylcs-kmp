import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.GalleryApplication
import love.yinlin.PageItem
import love.yinlin.currentPage
import love.yinlin.galleryPreviewComponentTitle
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<DesktopGalleryApplication>()

private class DesktopGalleryApplication(override val commandArgs: Array<String>) : GalleryApplication<DesktopGalleryApplication>(applicationReference, PlatformContext.Instance) {
    override val title = "Rachel UI Gallery"
    override val initSize = DpSize(1280.dp, 768.dp)
}

fun main(args: Array<String>) {
    System.getenv("GALLERY_PREVIEW")?.takeIf { it.isNotBlank() }?.let { selector ->
        val target = selector.split(':', limit = 2)
        require(target.size == 2) { "Invalid GALLERY_PREVIEW: $selector" }
        val page = PageItem.entries.firstOrNull { it.id == target[0] } ?: error("Unknown Gallery page: ${target[0]}")
        require(page.components.any { it.title == target[1] }) { "Unknown Gallery component: $selector" }
        currentPage = page.page
        galleryPreviewComponentTitle = target[1]
    }
    DesktopGalleryApplication(args).run()
}

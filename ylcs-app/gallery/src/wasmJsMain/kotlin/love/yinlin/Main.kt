package love.yinlin

import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<WebGalleryApplication>()

private class WebGalleryApplication : GalleryApplication<WebGalleryApplication>(applicationReference, PlatformContext.Instance)

fun main() = WebGalleryApplication().run()

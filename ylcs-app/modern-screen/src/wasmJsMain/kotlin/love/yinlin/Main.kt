package love.yinlin

import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<WebModernScreenApplication>()

private class WebModernScreenApplication : ModernScreenApplication<WebModernScreenApplication>(applicationReference, PlatformContext.Instance)

fun main() = WebModernScreenApplication().run()

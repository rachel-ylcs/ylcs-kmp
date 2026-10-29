import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import love.yinlin.ModernScreenApplication
import love.yinlin.extension.LazyReference
import love.yinlin.foundation.PlatformContext

private val applicationReference = LazyReference<DesktopModernScreenApplication>()

private class DesktopModernScreenApplication(override val commandArgs: Array<String>) : ModernScreenApplication<DesktopModernScreenApplication>(applicationReference, PlatformContext.Instance) {
    override val title = "Rachel UI Modern Screen"
    override val initSize = DpSize(1280.dp, 768.dp)
}

fun main(args: Array<String>) = DesktopModernScreenApplication(args).run()

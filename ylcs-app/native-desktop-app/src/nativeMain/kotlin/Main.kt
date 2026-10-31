import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.Local
import love.yinlin.RachelApplication
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.img_logo
import love.yinlin.compose.Colors
import love.yinlin.compose.DefaultTopBar
import love.yinlin.compose.DefaultTopBarActions
import love.yinlin.compose.Theme
import love.yinlin.compose.WindowController
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.foundation.PlatformContext
import love.yinlin.foundation.os.SingleInstance
import org.jetbrains.compose.resources.DrawableResource
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (!SingleInstance.run("${Local.info.appName}.lock")) exitProcess(0)

    object : RachelApplication(PlatformContext.Instance) {
        override val commandArgs: Array<String> = args
        override val title: String = "银临茶舍"
        override val icon: DrawableResource = Res.drawable.img_logo

        @Composable
        override fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit, Colors.Transparent) {
            Icon(
                icon = if (controller.alwaysOnTop) Icons.MobiledataOff else Icons.VerticalAlignTop,
                tip = if (controller.alwaysOnTop) Theme.value.windowAlwaysTopDisableText else Theme.value.windowAlwaysTopEnableText,
                onClick = { controller.alwaysOnTop = !controller.alwaysOnTop },
                modifier = Modifier.size(Theme.size.smallIcon)
            )

            DefaultTopBarActions(controller, onExit)
        }
    }.run()
}

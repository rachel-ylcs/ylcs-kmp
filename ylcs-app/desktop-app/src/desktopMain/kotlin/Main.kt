package love.yinlin

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.img_logo
import love.yinlin.compose.Colors
import love.yinlin.compose.DefaultTopBar
import love.yinlin.compose.DefaultTopBarActions
import love.yinlin.compose.Theme
import love.yinlin.compose.WindowController
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.SingleInstance
import love.yinlin.uri.toUri
import org.jetbrains.compose.resources.DrawableResource
import java.awt.Desktop

fun main(args: Array<String>) {
    SingleInstance.run("${Local.info.appName}.lock")

    // 配置 macOS 深度链接
    Platform.use(Platform.MacOS) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            DeepLink.navigate(event.uri.toUri())
        }
    }

    object : RachelApplication(PlatformContext.Instance) {
        override val commandArgs: Array<String> = args
        override val title: String = "银临茶舍"
        override val icon: DrawableResource = Res.drawable.img_logo

        @Composable
        override fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit, Colors.Transparent) {
            if (config.userProfile?.hasPrivilegeVIPCalendar == true) {
                Icon(
                    icon = Icons.CleaningServices,
                    tip = "GC",
                    onClick = System::gc,
                    modifier = Modifier.size(Theme.size.smallIcon)
                )
            }

            Icon(
                icon = if (controller.alwaysOnTop) Icons.MobiledataOff else Icons.VerticalAlignTop,
                tip = if (controller.alwaysOnTop) Theme.value.windowAlwaysTopDisableText else Theme.value.windowAlwaysTopEnableText,
                onClick = { controller.alwaysOnTop = !controller.alwaysOnTop },
                modifier = Modifier.size(Theme.size.smallIcon)
            )

            DefaultTopBarActions(controller, onExit)
        }

        @Composable
        override fun ApplicationScope.MultipleWindow() {
            musicPlayer?.floatingLyrics?.Content()
        }
    }.run()
}
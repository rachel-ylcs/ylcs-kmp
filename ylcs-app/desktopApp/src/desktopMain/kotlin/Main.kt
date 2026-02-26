package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.launch
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.img_logo
import love.yinlin.compose.DefaultTopBar
import love.yinlin.compose.DefaultTopBarActions
import love.yinlin.compose.Theme
import love.yinlin.compose.WindowController
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.coroutines.ioContext
import love.yinlin.data.MimeType
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.foundation.StartupDelegate
import love.yinlin.platform.AutoUpdate
import love.yinlin.platform.SingleInstance
import love.yinlin.startup.StartupComposeSwingRender
import love.yinlin.startup.StartupMacOSDeepLink
import org.jetbrains.compose.resources.DrawableResource

fun main() = object : RachelApplication(PlatformContextDelegate()) {
    override val title: String = "银临茶舍"
    override val icon: DrawableResource = Res.drawable.img_logo

    @Composable
    override fun TopBar(controller: WindowController, onExit: () -> Unit) = DefaultTopBar(controller, onExit) {
        if (config.userProfile?.hasPrivilegeVIPCalendar == true) {
            Icon(
                icon = Icons.CleaningServices,
                tip = "GC",
                onClick = System::gc
            )
        }

        Icon(
            icon = Icons.RocketLaunch,
            tip = "加载更新包",
            onClick = {
                mainScope.launch(ioContext) {
                    picker.pickPath(mimeType = listOf(MimeType.ZIP), filter = listOf("*.zip"))?.let { path ->
                        AutoUpdate.start(path.path)
                    }
                }
            }
        )

        Icon(
            icon = if (controller.alwaysOnTop) Icons.MobiledataOff else Icons.VerticalAlignTop,
            tip = if (controller.alwaysOnTop) Theme.value.windowAlwaysTopDisableText else Theme.value.windowAlwaysTopEnableText,
            onClick = { controller.alwaysOnTop = !controller.alwaysOnTop }
        )

        DefaultTopBarActions(controller, onExit)
    }

    @Composable
    override fun ApplicationScope.MultipleWindow() {
        // TODO:
//        mp.floatingLyrics.let {
//            if (it.isAttached) it.Content()
//        }
    }

    private val setupSingleInstance by sync(priority = StartupDelegate.HIGH7) { SingleInstance.run("${Local.info.appName}.lock") }

    private val setComposeRender by service(factory = ::StartupComposeSwingRender)

    private val setupMacOSDeepLink by service(
        StartupMacOSDeepLink.Handler { DeepLink.openUri(it) },
        factory = ::StartupMacOSDeepLink
    )
}.run()
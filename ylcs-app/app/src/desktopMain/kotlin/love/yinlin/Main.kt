package love.yinlin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.data.MimeType
import love.yinlin.fixup.FixupWindowsSwingMaximize
import love.yinlin.platform.*
import love.yinlin.resources.*
import love.yinlin.startup.StartupComposeSwingRender
import love.yinlin.startup.StartupMacOSDeepLink
import love.yinlin.startup.StartupSingleInstance
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString

fun main() = object : RachelApplication(PlatformContextDelegate) {
    override val title: String = runBlocking { getString(Res.string.app_name) }
    override val icon: DrawableResource = Res.drawable.img_logo
    override val actionAlwaysOnTop: Boolean = true
    override val tray: Boolean = true
    override val trayHideNotification: String = "已隐藏到任务栏托盘中"

    override fun onCreateDelay() {
        FixupWindowsSwingMaximize.setBounds(context.window)
    }

    @Composable
    override fun TopBar(actions: @Composable (ActionScope.() -> Unit)) {
        super.TopBar {
            if (config.userProfile?.hasPrivilegeVIPCalendar == true) {
                Action(
                    icon = Icons.Outlined.CleaningServices,
                    tip = "GC",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    System.gc()
                }
            }

            ActionSuspend(
                icon = Icons.Outlined.RocketLaunch,
                tip = "加载更新包",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                picker.pickPath(mimeType = listOf(MimeType.ZIP), filter = listOf("*.zip"))?.let { path ->
                    Coroutines.io {
                        AutoUpdate.start(path.path)
                    }
                }
            }

            actions()
        }
    }

    @Composable
    override fun ApplicationScope.MultipleWindow() {
        mp.floatingLyrics.let {
            if (it.isAttached) it.Content()
        }
    }

    private val setupVLC by sync(priority = StartupDelegate.HIGH3) {
        val vlcPath = when (platform) {
            Platform.Windows -> "vlc"
            Platform.Linux -> "bin/vlc"
            Platform.MacOS -> "MacOS/vlc"
            else -> ""
        }
        System.setProperty("jna.library.path", Path(os.storage.appPath, vlcPath).toString())
    }

    private val singleInstance by service(priority = StartupDelegate.HIGH8, factory = ::StartupSingleInstance)

    private val setComposeRender by service(priority = StartupDelegate.HIGH8, factory = ::StartupComposeSwingRender)

    private val setupMacOSDeepLink by service(
        StartupMacOSDeepLink.Handler { uri ->
            DeepLink.openUri(uri)
        },
        factory = ::StartupMacOSDeepLink
    )
}.run()
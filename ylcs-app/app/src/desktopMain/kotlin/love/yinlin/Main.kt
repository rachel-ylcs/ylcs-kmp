package love.yinlin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.ApplicationScope
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
import org.jetbrains.compose.resources.StringResource
import kotlin.io.path.Path

fun main() = object : RachelApplication(PlatformContextDelegate) {
    override val title: StringResource = Res.string.app_name
    override val icon: DrawableResource = Res.drawable.img_logo
    override val actionAlwaysOnTop: Boolean = true
    override val tray: Boolean = true
    override val trayHideNotification: String = "已隐藏到任务栏托盘中"

    override fun onCreate() {
        super.onCreate()
        // TODO: 悬浮歌词
        // musicFactory.instance.floatingLyrics = ActualFloatingLyrics().apply { isAttached = true }
    }

    override fun ComposeWindow.onWindowCreate() {
        FixupWindowsSwingMaximize.setBounds(this)
    }

    @Composable
    override fun ActionScope.Actions() {
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
    }

    @Composable
    override fun ApplicationScope.MultipleWindow() {
        // TODO: 悬浮歌词
//        (musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let {
//            if (it.isAttached && config.enabledFloatingLyrics) it.Content()
//        }
    }

    private val setupVLC by sync(priority = StartupDelegate.HIGH3) {
        val vlcPath = Path(System.getProperty("compose.application.resources.dir")).parent.parent.let {
            when (platform) {
                Platform.Windows -> it.resolve("vlc")
                Platform.Linux -> it.resolve("bin/vlc")
                Platform.MacOS -> it.resolve("MacOS/vlc")
                else -> it
            }
        }
        System.setProperty("jna.library.path", vlcPath.toString())
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
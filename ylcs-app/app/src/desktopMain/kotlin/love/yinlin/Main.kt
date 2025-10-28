package love.yinlin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import love.yinlin.compose.composeApplication
import love.yinlin.data.MimeType
import love.yinlin.fixup.FixupWindowsSwingMaximize
import love.yinlin.platform.*
import love.yinlin.resources.*
import love.yinlin.service.PlatformContext

fun main() {
    service.init(PlatformContext)

    service.musicFactory.instance.floatingLyrics = ActualFloatingLyrics().apply { isAttached = true }

    composeApplication(
        title = Res.string.app_name,
        icon = Res.drawable.img_logo,
        actionAlwaysOnTop = true,
        onWindowCreate = {
            FixupWindowsSwingMaximize.setBounds(this)
            service.picker.bindWindow(this.windowHandle)
        },
        actions = {
            if (service.config.userProfile?.hasPrivilegeVIPCalendar == true) {
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
                service.picker.pickPath(mimeType = listOf(MimeType.ZIP), filter = listOf("*.zip"))?.let { path ->
                    Coroutines.io {
                        AutoUpdate.start(path.path)
                    }
                }
            }
        },
        tray = true,
        trayHideNotification = "已隐藏到任务栏托盘中",
        multiWindow = {
            // 悬浮歌词
            (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let {
                if (it.isAttached && service.config.enabledFloatingLyrics) it.Content()
            }
        },
        entry = { framework -> AppEntry { framework() } }
    ) {
        ScreenEntry()
    }
}
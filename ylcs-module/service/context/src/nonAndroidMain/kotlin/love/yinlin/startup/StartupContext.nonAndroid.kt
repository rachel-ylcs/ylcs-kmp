package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.PlatformPage
import love.yinlin.service.StartupArgs
import love.yinlin.service.SyncStartup

actual class StartupContext : SyncStartup {
    actual val platformContext = PlatformContext
    actual val platformPage = PlatformPage

    actual override fun init(context: PlatformContext, args: StartupArgs) {}
}
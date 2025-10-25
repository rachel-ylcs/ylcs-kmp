package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

actual class StartupContext : SyncStartup {
    actual val platformContext = PlatformContext

    actual override fun init(context: PlatformContext, args: Array<Any?>) {}
}
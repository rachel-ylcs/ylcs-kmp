package love.yinlin.startup

import love.yinlin.platform.Platform
import love.yinlin.service.PlatformContext
import love.yinlin.service.PlatformPage
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupInitialize
import love.yinlin.service.SyncStartup

@StartupInitialize(Platform.Android)
expect class StartupContext() : SyncStartup {
    val platformContext: PlatformContext
    val platformPage: PlatformPage
    override fun init(context: PlatformContext, args: StartupArgs)
}
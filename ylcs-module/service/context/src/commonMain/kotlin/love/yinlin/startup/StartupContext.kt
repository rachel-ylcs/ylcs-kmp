package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

expect class StartupContext() : SyncStartup {
    val platformContext: PlatformContext
    override fun init(context: PlatformContext, args: Array<Any?>)
}
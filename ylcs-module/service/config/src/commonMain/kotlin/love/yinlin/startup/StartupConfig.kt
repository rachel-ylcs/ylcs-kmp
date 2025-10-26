package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupFetcher
import love.yinlin.service.SyncStartup

@StartupFetcher(index = 0, name = "kv", returnType = StartupKV::class)
class StartupConfig : SyncStartup {
    override fun init(context: PlatformContext, args: StartupArgs) {

    }
}
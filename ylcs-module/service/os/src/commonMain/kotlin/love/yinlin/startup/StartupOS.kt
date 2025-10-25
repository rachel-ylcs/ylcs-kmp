package love.yinlin.startup

import love.yinlin.platform.*
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.SyncStartup

@StartupArg(index = 0, name = "appName", type = String::class)
class StartupOS : SyncStartup {
    lateinit var application: OSApplication
        private set
    lateinit var net: OSNet
        private set
    lateinit var storage: OSStorage
        private set

    override fun init(context: PlatformContext, args: StartupArgs) {
        application = buildOSApplication(context)
        net = buildOSNet(context)
        storage = buildOSStorage(context, args[0])
    }
}
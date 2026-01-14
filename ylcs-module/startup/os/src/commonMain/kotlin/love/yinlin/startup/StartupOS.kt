package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup

@StartupArg(index = 0, name = "appName", type = String::class)
class StartupOS : SyncStartup() {
    lateinit var application: OSApplication
        private set
    lateinit var net: OSNet
        private set
    lateinit var storage: OSStorage
        private set

    override fun init(context: Context, args: StartupArgs) {
        application = buildOSApplication(context)
        net = buildOSNet(context)
        storage = buildOSStorage(context, args[0])
    }
}
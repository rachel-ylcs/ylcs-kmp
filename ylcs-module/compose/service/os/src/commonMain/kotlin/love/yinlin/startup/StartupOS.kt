package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.platform.*
import love.yinlin.Context
import love.yinlin.StartupArg
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup

@StartupArg(index = 0, name = "appName", type = String::class)
@Stable
class StartupOS : SyncStartup {
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
package love.yinlin.startup

import love.yinlin.platform.*
import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

class StartupOS : SyncStartup {
    lateinit var application: OSApplication
        private set
    lateinit var net: OSNet
        private set
    lateinit var storage: OSStorage
        private set

    override fun init(context: PlatformContext, args: Array<Any?>) {
        val appName = args[0] as String

        application = buildOSApplication(context)
        net = buildOSNet(context)
        storage = buildOSStorage(context, appName)
    }
}
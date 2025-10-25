package love.yinlin.startup

import love.yinlin.platform.releaseSingleInstance
import love.yinlin.platform.requestSingleInstance
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.SyncStartup
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StartupSingleInstance : SyncStartup {
    override fun init(context: PlatformContext, args: StartupArgs) {
        if (!requestSingleInstance()) {
            releaseSingleInstance()
            exitProcess(0)
        }
        Runtime.getRuntime().addShutdownHook(thread(start = false) { releaseSingleInstance() })
    }
}
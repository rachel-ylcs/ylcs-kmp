package love.yinlin.startup

import love.yinlin.platform.releaseSingleInstance
import love.yinlin.platform.requestSingleInstance
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StartupSingleInstance : SyncStartup {
    override fun init(context: Context, args: StartupArgs) {
        if (!requestSingleInstance()) {
            releaseSingleInstance()
            exitProcess(0)
        }
        Runtime.getRuntime().addShutdownHook(thread(start = false) { releaseSingleInstance() })
    }
}
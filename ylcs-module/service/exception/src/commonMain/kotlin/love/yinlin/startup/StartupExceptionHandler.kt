package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

abstract class StartupExceptionHandler : SyncStartup {
    fun interface Handler {
        fun handle(key: String, e: Throwable, error: String)
    }

    lateinit var crashKey: String
        private set

    override fun init(context: PlatformContext, args: Array<Any?>) {
        crashKey = args[0] as String
    }
}

expect fun buildStartupExceptionHandler(): StartupExceptionHandler
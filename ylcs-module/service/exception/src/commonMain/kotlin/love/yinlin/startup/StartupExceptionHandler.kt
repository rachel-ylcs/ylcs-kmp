package love.yinlin.startup

import love.yinlin.service.SyncStartup

abstract class StartupExceptionHandler : SyncStartup {
    fun interface Handler {
        fun handle(e: Throwable, error: String)
    }
}

expect fun buildStartupExceptionHandler(): StartupExceptionHandler
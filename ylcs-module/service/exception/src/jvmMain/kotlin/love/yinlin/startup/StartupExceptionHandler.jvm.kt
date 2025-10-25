package love.yinlin.startup

import love.yinlin.service.PlatformContext

actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    override fun init(context: PlatformContext, args: Array<Any?>) {
        val handler = args[0] as Handler
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handler.handle(e, e.stackTraceToString())
        }
    }
}
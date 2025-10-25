package love.yinlin.startup

import love.yinlin.service.PlatformContext

actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    override fun init(context: PlatformContext, args: Array<Any?>) {
        super.init(context, args)
        val handler = args[1] as Handler
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
    }
}
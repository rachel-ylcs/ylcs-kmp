package love.yinlin

expect class AppContext(context: PlatformContext, appName: String) {
    val appName: String
}

lateinit var appContext: AppContext
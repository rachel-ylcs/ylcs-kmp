package love.yinlin.compose.ui

actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.SDKVersion()

    actual suspend fun init() { }
}
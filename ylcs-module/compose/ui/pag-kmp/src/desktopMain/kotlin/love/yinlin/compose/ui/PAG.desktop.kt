package love.yinlin.compose.ui

actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.sdkVersion

    actual suspend fun init() { }
}
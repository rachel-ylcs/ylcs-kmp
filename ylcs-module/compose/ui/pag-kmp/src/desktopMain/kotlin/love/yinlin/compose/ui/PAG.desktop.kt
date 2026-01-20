package love.yinlin.compose.ui

actual object PAG {
    actual val sdkVersion: String by PlatformPAG::sdkVersion

    actual suspend fun init() { }
}
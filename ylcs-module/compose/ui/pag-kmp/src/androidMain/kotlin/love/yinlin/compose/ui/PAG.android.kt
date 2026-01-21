package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.SDKVersion()

    actual suspend fun init() { }
}
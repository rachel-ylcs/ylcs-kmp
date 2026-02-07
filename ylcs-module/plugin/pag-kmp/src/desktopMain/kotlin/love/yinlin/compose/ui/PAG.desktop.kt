package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.sdkVersion

    actual suspend fun init() { }
}
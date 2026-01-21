@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import kotlinx.cinterop.ExperimentalForeignApi

@Stable
actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.SDKVersion() ?: ""

    actual suspend fun init() { }
}
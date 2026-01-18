@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.ExperimentalForeignApi

actual object PAG {
    actual val sdkVersion: String get() = PlatformPAG.SDKVersion()

    actual suspend fun init() { }
}
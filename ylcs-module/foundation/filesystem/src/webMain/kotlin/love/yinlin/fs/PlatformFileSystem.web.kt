@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.fs

import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.foundation.PlatformContextDelegate
import kotlin.js.*

private fun webPlatformName(): String = js("(typeof navigator !== \"undefined\" && navigator.platform) || \"unknown\"")

actual object PlatformFileSystem {
    private val isWin32 by lazy { webPlatformName().startsWith("win", true) }
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = if (isWin32) "\r\n" else "\n"
    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File("$PathSeparator$appName")
    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File("$PathSeparator$appName${PathSeparator}data")
    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = File("$PathSeparator$appName${PathSeparator}cache${PathSeparator}temp")
}
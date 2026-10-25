@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package androidx.compose.foundation

import kotlin.native.OsFamily
import kotlin.native.Platform

internal enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        val Current: DesktopPlatform = when (Platform.osFamily) {
            OsFamily.LINUX -> Linux
            OsFamily.WINDOWS -> Windows
            OsFamily.MACOSX -> MacOS
            else -> Unknown
        }
    }
}

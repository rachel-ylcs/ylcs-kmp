package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.extension.localComposition

@Stable
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    override fun toString(): String = when (this) {
        SYSTEM -> "系统"
        LIGHT -> "浅色"
        DARK -> "深色"
    }
}

val LocalDarkMode = localComposition<Boolean>()
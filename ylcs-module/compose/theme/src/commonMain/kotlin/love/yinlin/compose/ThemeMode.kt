package love.yinlin.compose

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.compose.extension.localComposition

@Stable
@Serializable
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    override fun toString(): String = when (this) {
        SYSTEM -> "系统"
        LIGHT -> "浅色"
        DARK -> "深色"
    }
}

val LocalDarkMode = localComposition<Boolean>()
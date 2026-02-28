package love.yinlin.compose

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK; }
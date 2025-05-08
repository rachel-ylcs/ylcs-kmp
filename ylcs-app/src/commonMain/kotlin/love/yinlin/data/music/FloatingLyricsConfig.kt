package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.common.Colors

@Stable
@Serializable
data class FloatingLyricsConfig(
    // 左侧偏移 0.0 ~ 1.0
    val left: Float = 0f,
    // 右侧偏移 0.0 ~ 1.0
    val right: Float = 1f,
    // 纵向偏移 0.0 ~ 2.0
    val top: Float = 1f,
    // 字体大小 0.75 ~ 1.5
    val textSize: Float = 1f,
    // 字体颜色
    val textColor: ULong = Colors.Steel4.value,
    // 背景颜色
    val backgroundColor: ULong = Colors.Transparent.value
)
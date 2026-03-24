package love.yinlin.media.lyrics

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.compose.Colors

@Stable
@Serializable
data class LyricsEngineConfig(
    // 字体大小 0.75 ~ 1.5
    val textSize: Float = 1f,
    // 歌词前景色
    val textColor: ULong = Colors.Green5.value,
    // 歌词背景色
    val textBackgroundColor: ULong = Colors.White.value,
    // 浮窗背景色
    val backgroundColor: ULong = Colors.Transparent.value,
    // Android配置
    val android: Android = Android(),
    // iOS配置
    val iOS: IOS = IOS,
    // Desktop配置
    val desktop: Desktop = Desktop(),
    // Web配置
    val web: Web = Web,
) {
    val textSizeProgress: Float get() = textSize / 0.75f - 1f

    fun copyTextSize(percent: Float) = this.copy(textSize = ((percent + 1f) * 0.75f).coerceIn(0.75f, 1.5f))

    @Stable
    @Serializable
    data class Android(
        // 左侧偏移 0.0 ~ 1.0
        val left: Float = 0f,
        // 右侧偏移 0.0 ~ 1.0
        val right: Float = 1f,
        // 纵向偏移 0.0 ~ 1.0
        val top: Float = 1f,
        // 布局方向 true 顶部
        val placeTop: Boolean = true,
    ) {
        val leftProgress: Float get() = left
        val rightProgress: Float get() = right
        val topProgress: Float get() = top

        fun copyLeft(percent: Float) = this.copy(left = percent.coerceIn(0f, 1f))
        fun copyRight(percent: Float) = this.copy(right = percent.coerceIn(0f, 1f))
        fun copyTop(percent: Float) = this.copy(top = percent.coerceIn(0f, 1f))
    }

    @Stable
    @Serializable
    data object IOS

    @Stable
    @Serializable
    data class Desktop(
        // 左边
        val x: Float = 0f,
        // 顶边
        val y: Float = 0f,
        // 宽度
        val width: Float = 600f,
        // 高度
        val height: Float = 50f,
    )

    @Stable
    @Serializable
    data object Web
}
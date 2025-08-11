package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap

// 游戏配置
internal data object RhymeConfig {
    const val FPS = 60 // 帧率
    const val PAUSE_TIME = 3 // 暂停时间
    const val LOCK_SCRIM_ALPHA = 0.5f // 锁定遮罩透明度
}

internal val Size.Companion.Game get() = Size(1920f, 1080f)

// 图片资源集
@Stable
internal data class ImageSet(
    val record: ImageBitmap,
    val noteLayoutMap: ImageBitmap,
    val clickAnimationNote: ImageBitmap,
)

// 贴图数据
@Stable
internal sealed interface DrawImageData {
    val srcSize: Size
    val srcOffsets: List<Offset>
    val dstOffset: Offset
    val dstSize: Size
}
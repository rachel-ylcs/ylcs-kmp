package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable

@Stable
enum class LayerType {
    /**
     * 相对定位，会随着相机移动
     */
    Relative,
    /**
     * 绝对定位，与相机位置无关
     */
    Absolute;
}
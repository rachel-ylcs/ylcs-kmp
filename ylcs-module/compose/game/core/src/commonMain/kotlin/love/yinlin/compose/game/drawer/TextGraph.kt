package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.util.packFloats
import kotlin.jvm.JvmInline

@Stable
@JvmInline
value class TextGraph internal constructor(internal val paragraph: Paragraph) {
    internal fun widthScale(height: Float): Long {
        val rawHeight = paragraph.height
        val scale = if (rawHeight == 0f) 0f else height / rawHeight
        return packFloats(paragraph.width * scale, scale)
    }

    /**
     * 根据提供的高度计算宽度
     */
    fun width(height: Float): Float {
        val rawHeight = paragraph.height
        return if (rawHeight == 0f) 0f else paragraph.width * height / rawHeight
    }

    /**
     * 根据提供的高度计算大小
     */
    fun size(height: Float): Size {
        val rawHeight = paragraph.height
        return Size(if (rawHeight == 0f) 0f else paragraph.width * height / rawHeight, height)
    }
}
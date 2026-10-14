package love.yinlin.compose.ui.widget

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
internal data class HexViewMetrics(
    val columns: Int,
    val width: Int,
    val rowHeight: Int,
    val padding: Float,
    val offsetWidth: Float,
    val hexCellWidth: Float,
    val asciiCellWidth: Float,
    val groupGap: Float,
    val asciiGap: Float,
    val showAscii: Boolean,
) {
    companion object {
        const val HEX_VIEW_GROUP_SIZE = 8
    }

    val hexStart: Float = padding + offsetWidth
    val hexEnd: Float = hexStart + columns * hexCellWidth + (columns / HEX_VIEW_GROUP_SIZE - 1) * groupGap
    val asciiStart: Float = hexEnd + asciiGap

    fun hexPosition(column: Int): Float = hexStart + column * hexCellWidth + column / HEX_VIEW_GROUP_SIZE * groupGap

    fun columnAt(x: Float, ascii: Boolean, clamp: Boolean): Int? {
        val start = if (ascii) asciiStart else hexStart
        val end = if (ascii) asciiStart + columns * asciiCellWidth else hexEnd
        if (!clamp && x !in start ..< end) return null

        val position = (x - start).coerceAtLeast(0f)
        val column = if (ascii) {
            (position / asciiCellWidth).toInt()
        }
        else {
            val groupWidth = HEX_VIEW_GROUP_SIZE * hexCellWidth + groupGap
            val group = (position / groupWidth).toInt()
            val withinGroup = position - group * groupWidth
            if (!clamp && withinGroup >= HEX_VIEW_GROUP_SIZE * hexCellWidth) return null
            group * HEX_VIEW_GROUP_SIZE + (withinGroup / hexCellWidth).toInt().coerceAtMost(HEX_VIEW_GROUP_SIZE - 1)
        }
        return column.coerceIn(0, columns - 1)
    }

    fun indexAt(position: Offset, listState: LazyListState, size: Int, ascii: Boolean, clamp: Boolean): Int? {
        if (size == 0) return null
        val column = columnAt(position.x, ascii, clamp) ?: return null
        val layout = listState.layoutInfo
        val start = layout.viewportStartOffset.toFloat()
        val end = layout.viewportEndOffset.toFloat()
        if (end <= start || (!clamp && position.y !in start ..< end)) return null
        val y = if (clamp) position.y.coerceIn(start, (end - 1f).coerceAtLeast(start)) else position.y
        val row = layout.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size } ?: return null
        val index = row.index.toLong() * columns + column
        return if (clamp) index.coerceAtMost(size.toLong() - 1).toInt() else index.toInt().takeIf { index < size }
    }
}
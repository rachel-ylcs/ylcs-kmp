package love.yinlin.media.lyrics

// 有序歌词采用上界二分查找；非有序自定义歌词保留原先的扫描语义
internal class LyricsTimeline<T : TextLine>(private val lines: List<T>) {
    private val ordered: Boolean = run {
        for (index in 1 ..< lines.size) {
            if (lines[index - 1].position > lines[index].position) {
                return@run false
            }
        }
        true
    }

    fun indexAt(position: Long): Int {
        if (!ordered) {
            val next = lines.indexOfFirst { it.position > position }
            return if (next == -1) lines.lastIndex else next - 1
        }
        var low = 0
        var high = lines.size
        while (low < high) {
            val middle = low + (high - low) / 2
            if (lines[middle].position <= position) low = middle + 1
            else high = middle
        }
        return low - 1
    }
}
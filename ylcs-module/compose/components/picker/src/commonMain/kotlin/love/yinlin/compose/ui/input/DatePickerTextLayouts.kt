package love.yinlin.compose.ui.input

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

@Stable
internal class DatePickerTextLayouts(private val measurer: TextMeasurer, private val style: TextStyle) {
    private val days = arrayOfNulls<TextLayoutResult>(31)
    private val weekdays = arrayOfNulls<TextLayoutResult>(7)
    private val months = arrayOfNulls<TextLayoutResult>(12)

    fun day(day: Int): TextLayoutResult = cached(days, day - 1) { day.toString() }
    fun weekday(index: Int): TextLayoutResult = cached(weekdays, index) { "一二三四五六日"[index].toString() }
    fun month(index: Int): TextLayoutResult = cached(months, index) { "${index + 1}月" }
    fun year(year: Int): TextLayoutResult = measurer.measure(year.toString(), style, softWrap = false, maxLines = 1)

    private inline fun cached(cache: Array<TextLayoutResult?>, index: Int, label: () -> String): TextLayoutResult {
        val layout = cache[index]
        if (layout != null && !layout.multiParagraph.intrinsics.hasStaleResolvedFonts) return layout
        val result = measurer.measure(label(), style, softWrap = false, maxLines = 1, skipCache = true)
        cache[index] = result
        return result
    }
}

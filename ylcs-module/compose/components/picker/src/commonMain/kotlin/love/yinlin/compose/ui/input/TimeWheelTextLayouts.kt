package love.yinlin.compose.ui.input

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

@Stable
internal class TimeWheelTextLayouts(private val measurer: TextMeasurer, private val style: TextStyle) {
    companion object {
        private val TimeWheelLabels = Array(TimePickerState.MINUTES) { it.toString().padStart(2, '0') }
    }

    private val layouts = arrayOfNulls<TextLayoutResult>(TimeWheelLabels.size)

    operator fun get(index: Int): TextLayoutResult {
        val cached = layouts[index]
        if (cached != null && !cached.multiParagraph.intrinsics.hasStaleResolvedFonts) return cached
        val layout = measurer.measure(TimeWheelLabels[index], style, softWrap = false, maxLines = 1, skipCache = true)
        layouts[index] = layout
        return layout
    }
}
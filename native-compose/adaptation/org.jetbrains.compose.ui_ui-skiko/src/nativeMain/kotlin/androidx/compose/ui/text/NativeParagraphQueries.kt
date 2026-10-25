package androidx.compose.ui.text

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.text.style.ResolvedTextDirection
import org.jetbrains.skia.BreakIterator
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.RectHeightMode
import org.jetbrains.skia.paragraph.RectWidthMode
import org.jetbrains.skia.paragraph.Paragraph as SkParagraph

internal fun nativeRangeForRect(paragraph: SkParagraph, text: String, locale: String?, rect: Rect, granularity: TextGranularity, inclusionStrategy: TextInclusionStrategy): TextRange {
    require(granularity == TextGranularity.Character || granularity == TextGranularity.Word) { "Unsupported text granularity" }
    if (text.isEmpty() || rect.isEmpty) return TextRange.Zero
    val iterator = if (granularity == TextGranularity.Word) BreakIterator.makeWordInstance(locale) else BreakIterator.makeCharacterInstance(locale)
    return iterator.use {
        it.setText(text)
        var start = it.first()
        var end = it.next()
        var first = -1
        var last = -1
        while (end != BreakIterator.DONE) {
            val isWord = granularity != TextGranularity.Word || it.ruleStatus >= BreakIterator.WORD_NONE_LIMIT
            if (isWord) {
                val pieces = paragraph.getRectsForRange(start, end, RectHeightMode.STRUT, RectWidthMode.TIGHT)
                if (pieces.any { piece -> inclusionStrategy.isIncluded(piece.rect.toComposeRect(), rect) }) {
                    if (first == -1) first = start
                    last = end
                }
            }
            start = end
            end = it.next()
        }
        if (first == -1) TextRange.Zero else TextRange(first, last)
    }
}

internal fun nativeFillBoundingBoxes(host: Paragraph, paragraph: SkParagraph, text: String, locale: String?, range: TextRange, array: FloatArray, arrayStart: Int) {
    require(range.max <= text.length) { "Text range exceeds the paragraph" }
    require(arrayStart in 0 .. array.size && range.length <= (array.size - arrayStart) / 4) { "Destination does not contain the requested bounding boxes" }
    if (range.collapsed) return
    BreakIterator.makeCharacterInstance(locale).use { iterator ->
        iterator.setText(text)
        var clusterStart = iterator.preceding(range.min + 1).coerceAtLeast(0)
        var clusterEnd = iterator.following(clusterStart)
        var previous: Rect? = if (clusterStart == 0) null else {
            val previousStart = iterator.preceding(clusterStart)
            paragraph.getRectsForRange(previousStart, clusterStart, RectHeightMode.STRUT, RectWidthMode.TIGHT).firstOrNull()?.rect?.toComposeRect()
        }
        var bounds = paragraph.getRectsForRange(clusterStart, clusterEnd, RectHeightMode.STRUT, RectWidthMode.TIGHT).firstOrNull()?.rect?.toComposeRect()
            ?: host.getBoundingBox(clusterStart)
        var output = arrayStart
        for (offset in range.min ..< range.max) {
            if (offset >= clusterEnd) {
                previous = bounds
                clusterStart = clusterEnd
                clusterEnd = iterator.following(clusterStart)
                bounds = paragraph.getRectsForRange(clusterStart, clusterEnd, RectHeightMode.STRUT, RectWidthMode.TIGHT).firstOrNull()?.rect?.toComposeRect()
                    ?: host.getBoundingBox(clusterStart)
            }
            val line = host.getLineForOffset(offset)
            val isContinuation = offset != clusterStart || bounds == previous
            val isNewline = text[clusterStart] == '\n' || text[clusterStart] == '\r'
            val edge = when {
                isNewline -> host.getHorizontalPosition(clusterStart, true)
                host.getBidiRunDirection(clusterStart) == ResolvedTextDirection.Rtl -> bounds.left
                else -> bounds.right
            }
            array[output++] = if (isContinuation || isNewline) edge else bounds.left
            array[output++] = host.getLineTop(line)
            array[output++] = if (isContinuation || isNewline) edge else bounds.right
            array[output++] = host.getLineBottom(line)
        }
    }
}

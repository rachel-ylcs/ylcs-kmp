package love.yinlin.nativecompose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.clearSkikoComposeImplementation
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextGranularity
import androidx.compose.ui.text.TextInclusionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiSkikoTest {
    @BeforeTest
    fun registerBackend() = registerSkikoComposeImplementation()

    @AfterTest
    fun clearBackend() = clearSkikoComposeImplementation()

    @Test
    fun wordSelectionUsesActualBoundsAndSkipsWhitespace() {
        val paragraph = paragraph("  hello world  ")
        val full = Rect(0f, 0f, paragraph.width, paragraph.height)
        assertEquals(TextRange(2, 13), paragraph.getRangeForRect(full, TextGranularity.Word, TextInclusionStrategy.AnyOverlap))
        assertEquals(TextRange.Zero, paragraph.getRangeForRect(Rect(-20f, -20f, -10f, -10f), TextGranularity.Word, TextInclusionStrategy.AnyOverlap))
    }

    @Test
    fun characterSelectionKeepsACombiningSequenceTogether() {
        val paragraph = paragraph("a\u0301b")
        val bounds = paragraph.getBoundingBox(0)
        assertEquals(TextRange(0, 2), paragraph.getRangeForRect(bounds, TextGranularity.Character, TextInclusionStrategy.ContainsCenter))
    }

    @Test
    fun boundingBoxesHandleCombiningMarksSurrogatesAndReversedRanges() {
        val paragraph = paragraph("a\u0301😀b")
        val boxes = FloatArray(24) { -99f }
        paragraph.fillBoundingBoxes(TextRange(5, 0), boxes, 2)
        assertEquals(-99f, boxes[0])
        assertEquals(-99f, boxes[23])
        assertTrue(boxes[4] > boxes[2])
        assertEquals(boxes[6], boxes[8])
        assertTrue(boxes[12] > boxes[10])
        assertEquals(boxes[14], boxes[16])
        assertFalse(boxes.sliceArray(2 .. 21).any { it == -99f })
    }

    @Test
    fun boundingBoxesValidateRangesAndDestinationCapacity() {
        val paragraph = paragraph("abc")
        assertFailsWith<IllegalArgumentException> { paragraph.fillBoundingBoxes(TextRange(0, 4), FloatArray(16), 0) }
        assertFailsWith<IllegalArgumentException> { paragraph.fillBoundingBoxes(TextRange(0, 3), FloatArray(11), 0) }
        assertFailsWith<IllegalArgumentException> { paragraph.fillBoundingBoxes(TextRange.Zero, FloatArray(0), -1) }
        paragraph.fillBoundingBoxes(TextRange.Zero, FloatArray(0), 0)
    }

    private fun paragraph(text: String, width: Int = 240): Paragraph = Paragraph(
        text = text,
        style = TextStyle(color = Color.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace, textDirection = TextDirection.Content),
        constraints = Constraints(maxWidth = width),
        density = Density(1f),
        fontFamilyResolver = createFontFamilyResolver(),
        defaultLocaleList = LocaleList(Locale("en-US"))
    )
}

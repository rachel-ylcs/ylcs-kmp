package love.yinlin.nativecompose.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiGeometryTest {
    @Test
    fun offsetsAndSizesRetainPackedValuesThroughArithmetic() {
        val offset = Offset(-12.5f, 48f) + Offset(2.5f, -8f)
        assertEquals(Offset(-10f, 40f), offset)
        assertEquals(Offset(-20f, 80f), offset * 2f)
        assertEquals(Size(80f, 40f), Size(40f, 20f) * 2f)
    }

    @Test
    fun intersectionsUseExclusiveRightAndBottomEdges() {
        val rect = Rect(0f, 0f, 100f, 80f)
        assertTrue(rect.contains(Offset.Zero))
        assertFalse(rect.contains(Offset(100f, 80f)))
        assertEquals(Rect(60f, 20f, 100f, 80f), rect.intersect(Rect(60f, 20f, 120f, 120f)))
        assertEquals(Rect(10f, 20f, 110f, 100f), rect.translate(Offset(10f, 20f)))
    }

    @Test
    fun cornerRadiiAndRectangleCentersAreCalculated() {
        assertEquals(CornerRadius(24f, 36f), CornerRadius(8f, 12f) * 3f)
        assertEquals(Offset(30f, 35f), Rect(10f, 20f, 50f, 50f).center)
    }
}

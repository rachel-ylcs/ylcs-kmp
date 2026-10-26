package love.yinlin.nativecompose.ui

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiUnitTest {
    @Test
    fun densityAndFontScaleConvertBothDirections() {
        with(Density(2f, 1.5f)) {
            assertEquals(48f, 16.sp.toPx())
            assertEquals(24.dp, 16.sp.toDp())
            assertEquals(16.sp, 24.dp.toSp())
            assertEquals(24, 12.dp.roundToPx())
        }
    }

    @Test
    fun constraintsClampBothDimensions() {
        val constraints = Constraints(minWidth = 20, maxWidth = 100, minHeight = 40, maxHeight = 200)
        assertEquals(IntSize(20, 200), constraints.constrain(IntSize(10, 600)))
        assertEquals(IntSize(80, 120), constraints.constrain(IntSize(80, 120)))
    }

    @Test
    fun invalidAndUnboundedConstraintsRetainTheirContracts() {
        assertFailsWith<IllegalArgumentException> { Constraints(minWidth = 20, maxWidth = 10) }
        val constraints = Constraints(maxWidth = Constraints.Infinity, maxHeight = 200)
        assertFalse(constraints.hasBoundedWidth)
        assertTrue(constraints.hasBoundedHeight)
    }

    @Test
    fun integerCoordinatesPreserveNegativeValues() {
        assertEquals(IntOffset(-10, 40), IntOffset(-12, 48) + IntOffset(2, -8))
        assertEquals(IntSize(80, 40), IntSize(40, 20) * 2)
    }
}

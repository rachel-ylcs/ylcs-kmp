package love.yinlin.foundation.os

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestBatteryLevel {
    @Test
    fun percentage() {
        assertEquals(BatteryLevel.Value(0), BatteryLevel.fromPercentage(0))
        assertEquals(BatteryLevel.Value(100), BatteryLevel.fromPercentage(100))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromPercentage(-1))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromPercentage(101))
        assertFailsWith<IllegalArgumentException> { BatteryLevel.Value(101) }
    }

    @Test
    fun fraction() {
        assertEquals(BatteryLevel.Value(56), BatteryLevel.fromFraction(0.556))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromFraction(-1.0))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromFraction(Double.NaN))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromFraction(Double.POSITIVE_INFINITY))
    }

    @Test
    fun windowsFlags() {
        assertEquals(BatteryLevel.NoBattery, BatteryLevel.fromWindows(128, 255))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromWindows(255, 255))
        assertEquals(BatteryLevel.Unknown, BatteryLevel.fromWindows(0, 255))
        assertEquals(BatteryLevel.Value(0), BatteryLevel.fromWindows(2, 0))
        assertEquals(BatteryLevel.Value(100), BatteryLevel.fromWindows(1, 100))
    }
}

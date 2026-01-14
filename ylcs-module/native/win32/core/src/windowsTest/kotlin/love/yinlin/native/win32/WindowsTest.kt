package love.yinlin.native.win32

import kotlin.test.Test

class WindowsTest {
    @Test
    fun testStandardPath() {
        println(StandardPath.Windows.path)
        println(StandardPath.Desktop.path)
        println(StandardPath.Running.path)
        println(StandardPath.Temp.path)
    }

    @Test
    fun testDiskName() {
        println(DriverInfo.entries)
    }
}
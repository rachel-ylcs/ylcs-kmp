package love.yinlin

import love.yinlin.win32.DriverInfo
import love.yinlin.win32.StandardPath
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
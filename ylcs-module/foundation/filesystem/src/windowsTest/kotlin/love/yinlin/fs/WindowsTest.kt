package love.yinlin.fs

import kotlin.test.Test

class WindowsTest {
    @Test
    fun testStandardPath() {
        println(StandardPath.Windows.path)
        println(StandardPath.Running.path)
        println(StandardPath.Temp.path)
        println(StandardPath.Desktop.path)
        println(StandardPath.Documents.path)
        println(StandardPath.Music.path)
        println(StandardPath.Video.path)
        println(StandardPath.Fonts.path)
        println(StandardPath.AppData.path)
        println(StandardPath.ProgramFiles.path)
        println(StandardPath.Picture.path)
    }

    @Test
    fun testDiskName() {
        println(DriverInfo.entries)
    }

    @Test
    fun testNativePath() {
        val path1 = File("C:\\好好好\\测试啊.txt")
        println(path1)
        println(path1.name)
        println(path1.isAbsolute)
        println(path1.parent)

        val path2 = File("/sdcard/data/测试啊.txt")
        println(path2)
        println(path2.name)
        println(path2.isAbsolute)
        println(path2.parent)
    }
}
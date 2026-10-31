package love.yinlin.fs

import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.foundation.PlatformContext
import platform.windows.CreateDirectoryW
import platform.windows.RemoveDirectoryW
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WindowsTest {
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testCurrentDirectoryWithUnicode() {
        val original = assertNotNull(PlatformFileSystem.getCurrentDirectory())
        val directory = File("${StandardPath.Temp.path}rachel-cwd-${Random.nextLong().toULong()}-中文")
        assertTrue(CreateDirectoryW(directory.path, null) != 0)
        try {
            assertTrue(PlatformFileSystem.setCurrentDirectory(directory))
            val current = assertNotNull(PlatformFileSystem.getCurrentDirectory())
            assertEquals(directory, current)
            assertTrue(current.isAbsolute)
            assertEquals(current, PlatformFileSystem.appPath(PlatformContext.Instance, "test"))
            assertEquals(File(current, "data"), PlatformFileSystem.dataPath(PlatformContext.Instance, "test"))
        }
        finally {
            val restored = PlatformFileSystem.setCurrentDirectory(original)
            val removed = RemoveDirectoryW(directory.path) != 0
            assertTrue(restored)
            assertTrue(removed)
        }
    }

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
package love.yinlin

import kotlinx.datetime.LocalDateTime
import kotlinx.io.files.Path
import love.yinlin.win32.createTime
import kotlin.test.Test

class WindowsTest {
    @Test
    fun test() {
        Path("C:\\Users\\Administrator\\Desktop\\123.txt").createTime = LocalDateTime(2025, 9, 12, 12, 31, 45)
    }
}
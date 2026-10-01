package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.data.redbook.RedBookUserInfo
import love.yinlin.tpl.redbook.RedBookAPI
import org.junit.Test

class TestRedBookAPI {
    @Test
    fun testRedBook() = runTest {
        println(RedBookAPI.requestUserProfile(RedBookUserInfo.Default[0].id))
    }
}
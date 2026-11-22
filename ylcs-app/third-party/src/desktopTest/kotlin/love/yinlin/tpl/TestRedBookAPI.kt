package love.yinlin.tpl

import love.yinlin.data.redbook.RedBookUserInfo
import love.yinlin.tpl.redbook.RedBookAPI
import kotlin.test.Test

class TestRedBookAPI {
    @Test
    fun testRedBook() = TPTest {
        val profile by tp { RedBookAPI.requestUserProfile(RedBookUserInfo.Default[0].id) }
    }
}

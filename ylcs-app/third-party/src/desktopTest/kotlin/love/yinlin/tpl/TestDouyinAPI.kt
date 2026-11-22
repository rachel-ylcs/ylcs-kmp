package love.yinlin.tpl

import love.yinlin.data.douyin.DouyinUserInfo
import love.yinlin.tpl.douyin.DouyinAPI
import kotlin.test.Test

class TestDouyinAPI {
    @Test
    fun testDouyinAPI() = TPTest {
        val cookie by tp { DouyinAPI.generateCookie() }

        val user = DouyinUserInfo.Default[0].id

        val items by tp {
            val first = DouyinAPI.requestUserDouyin(user, cookie)
            if (first.isNullOrEmpty()) DouyinAPI.requestUserDouyin(user, cookie)
            else first
        }
    }
}

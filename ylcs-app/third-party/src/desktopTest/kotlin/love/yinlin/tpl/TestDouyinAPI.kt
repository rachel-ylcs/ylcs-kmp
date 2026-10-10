package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.data.douyin.DouyinUserInfo
import love.yinlin.tpl.douyin.DouyinAPI
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestDouyinAPI {
    @Test
    fun testDouyinAPI() = runTest {
        val cookie = DouyinAPI.generateCookie()
        val user = DouyinUserInfo.Default[0].id
        // 第一次查询
        var items = DouyinAPI.requestUserDouyin(user, cookie)
        if (items.isNullOrEmpty()) items = DouyinAPI.requestUserDouyin(user, cookie)
        assertNotNull(items)
        println(items)
    }
}
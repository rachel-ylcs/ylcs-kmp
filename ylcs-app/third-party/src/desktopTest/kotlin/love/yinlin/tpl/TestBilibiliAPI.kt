package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.data.bilibili.BilibiliUserInfo
import love.yinlin.tpl.bilibili.BilibiliAPI
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBilibiliAPI {
    @Test
    fun testBilibili() = runTest {
        val rachel = BilibiliUserInfo.Default[0].id
        val cookie = BilibiliAPI.generateCookie()!!
        println(cookie)
        val [items1, offset1] = BilibiliAPI.requestUserDynamic(rachel, cookie)!!
        println(items1.size)
        val [items2, offset2] = BilibiliAPI.requestUserDynamic(rachel, cookie, offset1)!!
        println(items2.size)
        val random = (items1 + items2).random()
        val details = BilibiliAPI.requestDynamicDetails(random.id, cookie)!!
        assertEquals(random.id, details.id)
        println(details)
    }
}
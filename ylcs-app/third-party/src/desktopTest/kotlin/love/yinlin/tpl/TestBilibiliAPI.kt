package love.yinlin.tpl

import love.yinlin.data.bilibili.BilibiliUserInfo
import love.yinlin.tpl.bilibili.BilibiliAPI
import kotlin.test.Test

class TestBilibiliAPI {
    @Test
    fun testBilibili() = TPTest {
        val rachel = BilibiliUserInfo.Default[0].id

        val cookie by tp { BilibiliAPI.generateCookie() }

        val page1 by tp { BilibiliAPI.requestUserDynamic(rachel, cookie) }
        val [items1, offset1] = page1

        val page2 by tp { BilibiliAPI.requestUserDynamic(rachel, cookie, offset1) }
        val random = (items1 + page2.first).random()

        val details by tp { BilibiliAPI.requestDynamicDetails(random.id, cookie) }
        tpAssert { details.id == random.id }
    }
}

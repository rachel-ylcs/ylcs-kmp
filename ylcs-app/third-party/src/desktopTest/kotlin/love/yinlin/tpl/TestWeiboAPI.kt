package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.tpl.weibo.WeiboAPI
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestWeiboAPI {
    @Test
    fun testWeiboAPI() = runTest {
        // 获取 cookie
        val cookie = WeiboAPI.generateCookie()
        println(cookie)
        assertTrue { cookie.sub.isNotEmpty() }
        assertTrue { cookie.subp.isNotEmpty() }
        assertEquals(6, cookie.xsrfToken.length)

        val userInfo = WeiboUserInfo.Default[0]
        val uid = userInfo.id

        // 获取用户信息
        val user = WeiboAPI.requestUser(uid, cookie)
        assertNotNull(user)
        println(user)

        // 获取用户相册
        val albums = WeiboAPI.requestUserAlbum(uid, cookie)
        assertNotNull(albums)
        assertTrue { albums.isNotEmpty() }

        // 获取相册图片
        val album = albums[0]
        val pics = WeiboAPI.requestUserAlbumPics(album.containerId, 1, WeiboAlbum.DEFAULT_LIMIT, cookie)
        assertNotNull(pics)
        assertTrue { pics.items.isNotEmpty() && pics.count > 0 }

        // 获取用户微博
        val weibos = WeiboAPI.requestUserWeibo(uid, cookie)
        assertNotNull(weibos)
        assertTrue { weibos.isNotEmpty() }

        // 获取微博评论
        val weibo = weibos[0]
        val comments = WeiboAPI.requestWeiboComment(weibo.id, cookie)
        assertNotNull(comments)
        assertTrue { comments.isNotEmpty() }

        // 搜索用户
        val findUser = WeiboAPI.searchUser("银临Rachel", cookie)
        assertNotNull(findUser)
        assertTrue { findUser.isNotEmpty() }
        assertEquals(findUser[0].id, userInfo.id)

        // 超话
        val chaohua1 = WeiboAPI.requestChaohua(1, cookie)
        assertNotNull(chaohua1)
        assertTrue { chaohua1.isNotEmpty() }
        val chaohua2 = WeiboAPI.requestChaohua(2, cookie)
        assertNotNull(chaohua2)
        assertTrue { chaohua2.isNotEmpty() }
    }
}
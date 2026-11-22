package love.yinlin.tpl

import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.tpl.weibo.WeiboAPI
import kotlin.test.Test

class TestWeiboAPI {
    @Test
    fun globalTest() = TPTest {
        // 获取 cookie
        val cookie by tp { WeiboAPI.generateCookie() }
        tpAssert { cookie.sub.isNotEmpty() }
        tpAssert { cookie.subp.isNotEmpty() }
        tpAssert { cookie.xsrfToken.length == 6 }

        val userInfo = WeiboUserInfo.Default[0]
        val uid = userInfo.id

        // 获取用户信息
        val user by tp { WeiboAPI.requestUser(uid, cookie) }

        // 获取用户相册
        val albums by tp { WeiboAPI.requestUserAlbum(uid, cookie) }
        tpAssert { albums.isNotEmpty() }

        // 获取相册图片
        val album = albums[0]
        val pics by tp { WeiboAPI.requestUserAlbumPics(album.containerId, 1, WeiboAlbum.DEFAULT_LIMIT, cookie) }
        tpAssert { pics.items.isNotEmpty() && pics.count > 0 }

        // 获取用户微博
        val weibos by tp { WeiboAPI.requestUserWeibo(uid, cookie) }
        tpAssert { weibos.isNotEmpty() }

        // 获取微博评论
        val weibo = weibos[0]
        val comments by tp { WeiboAPI.requestWeiboComment(weibo.id, cookie) }
        tpAssert { comments.isNotEmpty() }

        // 搜索用户
        val findUser by tp { WeiboAPI.searchUser("银临Rachel", cookie) }
        tpAssert { findUser.isNotEmpty() }
        tpAssert { findUser[0].id == userInfo.id }

        // 超话
        val chaohua1 by tp { WeiboAPI.requestChaohua(1, cookie) }
        tpAssert { chaohua1.isNotEmpty() }
        val chaohua2 by tp { WeiboAPI.requestChaohua(2, cookie) }
        tpAssert { chaohua2.isNotEmpty() }

        // 新图片接口
        val newWeibo by tp { WeiboAPI.requestWeiboDetails("5350312425555052", cookie) }
        tpAssert { newWeibo.medias.size > 9 }
    }
}

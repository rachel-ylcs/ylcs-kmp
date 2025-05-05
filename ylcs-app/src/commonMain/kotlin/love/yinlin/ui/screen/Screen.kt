package love.yinlin.ui.screen

import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.ui.screen.common.*
import love.yinlin.ui.screen.world.*
import love.yinlin.ui.screen.msg.weibo.*
import love.yinlin.ui.screen.music.*
import love.yinlin.ui.screen.community.*
import love.yinlin.ui.screen.music.loader.ScreenCreateMusic
import love.yinlin.ui.screen.music.loader.ScreenImportMusic
import love.yinlin.ui.screen.music.loader.ScreenKugouMusic
import love.yinlin.ui.screen.music.loader.ScreenNetEaseCloudMusic
import love.yinlin.ui.screen.music.loader.ScreenQQMusic
import love.yinlin.ui.screen.settings.*

fun ScreenRouteScope.screens() {
    // 主页
    screen(::ScreenMain)

    // 通用
    screen(::ScreenWebpage)
    screen(::ScreenImagePreview, type<List<Picture>>())
    screen(::ScreenVideo)

    // 世界
    screen(::ScreenActivityDetails)
    screen(::ScreenAddActivity)
    screen(::ScreenModifyActivity)
    screen(::ScreenActivityLink)

	// 设置
    screen(::ScreenSettings)

	// 微博
    screen(::ScreenWeiboDetails)
    screen(::ScreenWeiboUser)
    screen(::ScreenWeiboFollows)
    screen(::ScreenWeiboAlbum)

	// 听歌
    screen(::ScreenMusicLibrary)
    screen(::ScreenPlaylistLibrary)
    screen(::ScreenMusicDetails)

    screen(::ScreenImportMusic)
    screen(::ScreenCreateMusic)
    screen(::ScreenQQMusic)
    screen(::ScreenNetEaseCloudMusic)
    screen(::ScreenKugouMusic)

	// 社区
    screen(::ScreenLogin)
    screen(::ScreenUserCard)
    screen(::ScreenTopic, type<Topic>())
    screen(::ScreenMail)
    screen(::ScreenAddTopic)
}
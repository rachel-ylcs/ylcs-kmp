package love.yinlin.ui.screen

import love.yinlin.data.common.Picture
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.ui.screen.common.*
import love.yinlin.ui.screen.msg.weibo.*
import love.yinlin.ui.screen.music.*
import love.yinlin.ui.screen.community.*
import love.yinlin.ui.screen.msg.activity.ScreenActivityDetails
import love.yinlin.ui.screen.msg.activity.ScreenActivityLink
import love.yinlin.ui.screen.msg.activity.ScreenAddActivity
import love.yinlin.ui.screen.msg.activity.ScreenModifyActivity
import love.yinlin.ui.screen.msg.pictures.ScreenPictures
import love.yinlin.ui.screen.music.loader.ScreenCreateMusic
import love.yinlin.ui.screen.music.loader.ScreenImportMusic
import love.yinlin.ui.screen.music.loader.ScreenPlatformMusic
import love.yinlin.ui.screen.settings.*
import love.yinlin.ui.screen.world.game.*

fun ScreenRouteScope.screens() {
    // 主页
    screen(::ScreenMain)

    // 通用
    screen(::ScreenTest)
    screen(::ScreenWebpage)
    screen(::ScreenImagePreview, type<List<Picture>>())
    screen(::ScreenVideo)

    // 微博
    screen(::ScreenWeibo)
    screen(::ScreenChaohua)
    screen(::ScreenWeiboDetails)
    screen(::ScreenWeiboUser)
    screen(::ScreenWeiboFollows)
    screen(::ScreenWeiboAlbum)

    // 美图
    screen(::ScreenPictures)

    // 活动
    screen(::ScreenActivityDetails)
    screen(::ScreenAddActivity)
    screen(::ScreenModifyActivity)
    screen(::ScreenActivityLink)

    // 世界
    screen(::ScreenGame1Hall)
    screen(::ScreenGame1Creating)
    screen(::ScreenGame1Playing)
    screen(::ScreenGame1Ranking)
    screen(::ScreenGame2Hall)
    screen(::ScreenGame2Creating)
    screen(::ScreenGame2Playing)
    screen(::ScreenGame2Ranking)
    screen(::ScreenGame3Hall)
    screen(::ScreenGame3Creating)
    screen(::ScreenGame3Playing)
    screen(::ScreenGame3Ranking)
    screen(::ScreenGame4Hall)
    screen(::ScreenGame4Creating)
    screen(::ScreenGame4Playing)
    screen(::ScreenGame4Ranking)

	// 设置
    screen(::ScreenSettings)

	// 听歌
    screen(::ScreenMusicLibrary)
    screen(::ScreenPlaylistLibrary)
    screen(::ScreenFloatingLyrics)
    screen(::ScreenMusicDetails)

    screen(::ScreenMusicModFactory)
    screen(::ScreenSongDetails, type<Song>())

    screen(::ScreenImportMusic)
    screen(::ScreenCreateMusic)
    screen(::ScreenPlatformMusic, type<PlatformMusicType>())

	// 社区
    screen(::ScreenLogin)
    screen(::ScreenUserCard)
    screen(::ScreenTopic, type<Topic>())
    screen(::ScreenMail)
    screen(::ScreenAddTopic)
    screen(::ScreenFollows)
}
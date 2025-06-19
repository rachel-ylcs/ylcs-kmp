package love.yinlin.ui.screen

import love.yinlin.data.common.Picture
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.ui.screen.common.*
import love.yinlin.ui.screen.msg.weibo.*
import love.yinlin.ui.screen.music.*
import love.yinlin.ui.screen.community.*
import love.yinlin.ui.screen.msg.activity.*
import love.yinlin.ui.screen.msg.douyin.ScreenDouyin
import love.yinlin.ui.screen.msg.pictures.ScreenPictures
import love.yinlin.ui.screen.music.loader.*
import love.yinlin.ui.screen.settings.*
import love.yinlin.ui.screen.world.*
import love.yinlin.ui.screen.world.online.ScreenGuessLyrics

fun ScreenRouteScope.screens() {
    // 主页
    screen(::ScreenMain)

    // 通用
    screen(::ScreenTest)
    screen(::ScreenWebpage)
    screen(::ScreenImagePreview, type<List<Picture>>())
    screen(::ScreenVideo)

    // 资讯
    screen(::ScreenPictures)

    screen(::ScreenWeibo)
    screen(::ScreenChaohua)
    screen(::ScreenWeiboDetails)
    screen(::ScreenWeiboUser)
    screen(::ScreenWeiboFollows)
    screen(::ScreenWeiboAlbum)

    screen(::ScreenDouyin)

    // 活动
    screen(::ScreenActivityDetails)
    screen(::ScreenAddActivity)
    screen(::ScreenModifyActivity)
    screen(::ScreenActivityLink)

    // 世界
    screen(::ScreenGameHall, type<Game>())
    screen(::ScreenGameRanking, type<Game>())
    screen(::ScreenCreateGame, type<Game>())
    screen(::ScreenPlayGame)
    screen(::ScreenGameHistory)
    screen(::ScreenGameRecordHistory)

    screen(::ScreenGuessLyrics)

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
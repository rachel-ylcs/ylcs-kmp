package love.yinlin.ui.screen

import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.buildNavTypeMap
import love.yinlin.ui.screen.common.*
import love.yinlin.ui.screen.world.*
import love.yinlin.ui.screen.msg.weibo.*
import love.yinlin.ui.screen.music.*
import love.yinlin.ui.screen.community.*
import love.yinlin.ui.screen.settings.*

fun ScreenRouteScope.screens() {
    // 主页
    screen(::ScreenMain)
    // 通用
    screen { model, args: ScreenWebpage.Args -> ScreenWebpage(model, args) }
    screen(typeMap = buildNavTypeMap<List<Picture>>()) { model, args: ScreenImagePreview.Args -> ScreenImagePreview(model, args) }
    screen { model, args: ScreenVideo.Args -> ScreenVideo(model, args) }
    // 世界
    screen { model, args: ScreenActivityDetails.Args -> ScreenActivityDetails(model, args) }
    screen(::ScreenAddActivity)
    screen { model, args: ScreenModifyActivity.Args -> ScreenModifyActivity(model, args) }
	// 设置
    screen(::ScreenSettings)
	// 微博
    screen(::ScreenWeiboDetails)
    screen { model, args: ScreenWeiboUser.Args -> ScreenWeiboUser(model, args) }
    screen(::ScreenWeiboFollows)
    screen { model, args: ScreenWeiboAlbum.Args -> ScreenWeiboAlbum(model, args) }
	// 听歌
    screen(::ScreenMusicLibrary)
    screen(::ScreenPlaylistLibrary)
    screen { model, args: ScreenImportMusic.Args -> ScreenImportMusic(model, args) }
    screen { model, args: ScreenMusicDetails.Args -> ScreenMusicDetails(model, args) }
	// 社区
    screen(::ScreenLogin)
    screen { model, args: ScreenUserCard.Args -> ScreenUserCard(model, args) }
    screen(typeMap = buildNavTypeMap<Topic>()) { model, args: ScreenTopic.Args -> ScreenTopic(model, args) }
    screen(::ScreenMail)
    screen(::ScreenAddTopic)
}
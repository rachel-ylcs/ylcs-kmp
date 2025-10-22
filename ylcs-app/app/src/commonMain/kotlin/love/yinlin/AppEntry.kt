package love.yinlin

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.compose.*
import love.yinlin.compose.screen.AppScreen
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk
import love.yinlin.screen.common.ScreenMain

// ScreenMain

// val deeplink = DeepLink(this)

//	DisposableEffect(Unit) {
//		DeepLinkHandler.listener = { uri ->
//			appModel.deeplink.process(uri)
//		}
//		onDispose {
//			DeepLinkHandler.listener = null
//		}
//	}

@Composable
fun AppEntry(
	fill: Boolean = true,
	modifier: Modifier = Modifier.fillMaxSize(),
	content: @Composable BoxWithConstraintsScope.() -> Unit
) {
	App(
		deviceFactory = { maxWidth, maxHeight -> if (fill) Device(maxWidth, maxHeight) else Device(maxWidth) },
		themeMode = app.config.themeMode,
		fontScale = app.config.fontScale,
		mainFontResource = Res.font.xwwk,
		modifier = modifier,
		localProvider = arrayOf(
			LocalAnimationSpeed provides app.config.animationSpeed,
			localBalloonTipEnabled provides app.config.enabledTip
		),
		content = content
	)
}

@Composable
fun ScreenEntry(modifier: Modifier = Modifier.fillMaxSize()) {
	AppScreen<ScreenMain>(modifier = modifier) {
		// 主页
		screen(::ScreenMain)
	}
}

//fun ScreenRouteScope.screens() {
//
//    // 通用
//    screen(::ScreenTest)
//    screen(::ScreenWebpage)
//    screen(::ScreenImagePreview, type<List<Picture>>())
//    screen(::ScreenVideo)
//
//    // 资讯
//    screen(::ScreenPictures)
//
//    screen(::ScreenWeibo)
//    screen(::ScreenChaohua)
//    screen(::ScreenWeiboDetails)
//    screen(::ScreenWeiboUser)
//    screen(::ScreenWeiboFollows)
//    screen(::ScreenWeiboAlbum)
//
//    screen(::ScreenDouyin)
//
//    // 活动
//    screen(::ScreenActivityDetails)
//    screen(::ScreenAddActivity)
//    screen(::ScreenModifyActivity)
//    screen(::ScreenActivityLink)
//
//    // 世界
//    screen(::ScreenGameHall, type<Game>())
//    screen(::ScreenGameRanking, type<Game>())
//    screen(::ScreenCreateGame, type<Game>())
//    screen(::ScreenPlayGame)
//    screen(::ScreenGameHistory)
//    screen(::ScreenGameRecordHistory)
//
//    screen(::ScreenGuessLyrics)
//
//    screen(::ScreenRhyme)
//
//	// 设置
//    screen(::ScreenSettings)
//
//	// 听歌
//    screen(::ScreenMusicLibrary)
//    screen(::ScreenPlaylistLibrary)
//    screen(::ScreenFloatingLyrics)
//    screen(::ScreenMusicDetails)
//
//    screen(::ScreenMusicModFactory)
//    screen(::ScreenSongDetails, type<Song>())
//
//    screen(::ScreenImportMusic)
//    screen(::ScreenCreateMusic)
//    screen(::ScreenPlatformMusic, type<PlatformMusicType>())
//
//	// 社区
//    screen(::ScreenLogin)
//    screen(::ScreenUserCard)
//    screen(::ScreenTopic, type<Topic>())
//    screen(::ScreenMail)
//    screen(::ScreenAddTopic)
//    screen(::ScreenFollows)
//}
package love.yinlin

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.uri.Scheme
import love.yinlin.common.uri.Uri
import love.yinlin.compose.*
import love.yinlin.compose.screen.AppScreen
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk
import love.yinlin.screen.account.*
import love.yinlin.screen.account.settings.*
import love.yinlin.screen.common.*
import love.yinlin.screen.community.*
import love.yinlin.screen.msg.activity.*
import love.yinlin.screen.msg.douyin.*
import love.yinlin.screen.msg.pictures.*
import love.yinlin.screen.msg.weibo.*
import love.yinlin.screen.music.*
import love.yinlin.screen.music.loader.*
import love.yinlin.screen.world.*
import love.yinlin.screen.world.battle.ScreenGuessLyrics
import love.yinlin.screen.world.single.rhyme.ScreenRhyme

data object AppDeepLink : DeepLink {
	private fun schemeContent(manager: ScreenManager, uri: Uri) {
		if (!app.musicFactory.isReady) manager.navigate(ScreenImportMusic.Args(uri.toString()))
		else manager.top.slot.tip.warning("请先停止播放器")
	}

	private fun schemeRachel(manager: ScreenManager, uri: Uri) {
		when (uri.path) {
			"/openProfile" -> {
				uri.params["uid"]?.toIntOrNull()?.let { uid ->
					manager.navigate(ScreenUserCard.Args(uid))
				}
			}
			"/openSong" -> {
				uri.params["id"]?.let { id ->
					manager.top.launch {
						val result = ClientAPI.request(
							route = API.User.Song.GetSong,
							data = id
						)
						if (result is Data.Success) manager.navigate(ScreenSongDetails.Args(result.data))
					}
				}
			}
		}
	}

	override fun process(manager: ScreenManager, uri: Uri) {
		when (uri.scheme) {
			Scheme.File -> schemeContent(manager, uri)
			Scheme.Content -> schemeContent(manager, uri)
			Scheme.Rachel -> schemeRachel(manager, uri)
			Scheme.QQMusic -> {
				if (!app.musicFactory.isReady) manager.navigate(ScreenPlatformMusic.Args(
					deeplink = uri.copy(scheme = Scheme.Https).toString(),
					type = PlatformMusicType.QQMusic
				))
				else manager.top.slot.tip.warning("请先停止播放器")
			}
			Scheme.NetEaseCloud -> {
				if (!app.musicFactory.isReady) manager.navigate(ScreenPlatformMusic.Args(
					deeplink = uri.copy(scheme = Scheme.Https).toString(),
					type = PlatformMusicType.NetEaseCloud
				))
				else manager.top.slot.tip.warning("请先停止播放器")
			}
		}
	}
}

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
	AppScreen<ScreenMain>(modifier = modifier, deeplink = AppDeepLink) {
		// 通用
		screen(::ScreenMain)
		screen(::ScreenImagePreview, listType<Picture>())
		screen(::ScreenWebpage)
		screen(::ScreenVideo)
		screen(::ScreenTest)

		// 美图
		screen(::ScreenPictures)

		// 资讯
		screen(::ScreenWeibo)
		screen(::ScreenChaohua)
		screen(::ScreenWeiboDetails)
		screen(::ScreenWeiboUser)
		screen(::ScreenWeiboFollows)
		screen(::ScreenWeiboAlbum)

		// 抖音
		screen(::ScreenDouyin)

		// 活动
		screen(::ScreenActivityDetails)
		screen(::ScreenAddActivity)
		screen(::ScreenModifyActivity)
		screen(::ScreenActivityLink)

		// 社区
	    screen(::ScreenUserCard)
	    screen(::ScreenTopic, type<Topic>())
		screen(::ScreenAddTopic)
		screen(::ScreenFollows)

		// 账户
		screen(::ScreenLogin)
		screen(::ScreenMail)
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


		// 世界
		screen(::ScreenGameHall, type<Game>())
		screen(::ScreenGameRanking, type<Game>())
		screen(::ScreenCreateGame, type<Game>())
		screen(::ScreenPlayGame)
		screen(::ScreenGameHistory)
		screen(::ScreenGameRecordHistory)

		screen(::ScreenGuessLyrics)

		screen(::ScreenRhyme)
	}
}
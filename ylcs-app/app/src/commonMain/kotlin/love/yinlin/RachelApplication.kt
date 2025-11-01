package love.yinlin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Paths
import love.yinlin.compose.DefaultAnimationSpeed
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.ThemeMode
import love.yinlin.data.compose.ImageQuality
import love.yinlin.data.compose.Picture
import love.yinlin.compose.screen.AppScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.data.NativeLibrary
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.Reference
import love.yinlin.platform.Platform
import love.yinlin.platform.lyrics.LyricsEngineConfig
import love.yinlin.platform.lyrics.LyricsEngineType
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
import love.yinlin.screen.world.*
import love.yinlin.screen.world.battle.*
import love.yinlin.startup.*
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import org.jetbrains.compose.resources.FontResource

@StartupFetcher(index = 0, name = "kv", returnType = StartupKV::class)
class AppConfig : StartupConfig() {
    /* ------------------  系统  ------------------ */

    // 主题模式
    var themeMode by enumState(ThemeMode.SYSTEM)
    // 动画速度
    var animationSpeed by intState(DefaultAnimationSpeed)
    // 字体大小
    var fontScale by floatState(1f, "20250518")
    // 悬浮提示
    var enabledTip by booleanState(true)

    /* ------------------  微博  ------------------ */

    // 微博用户
    val weiboUsers by listState("weiboUsers") { WeiboUserInfo.DEFAULT }

    /* ------------------  听歌  ------------------ */

    // 音频焦点
    var audioFocus by booleanState(true)

    // 歌单
    val playlistLibrary by mapState<String, MusicPlaylist>("playlistLibrary")
    // 上次播放列表
    var lastPlaylist by stringState("")
    // 上次播放歌曲
    var lastMusic by stringState("")
    // 播放模式
    var musicPlayMode by enumState(MusicPlayMode.ORDER)
    // 开启悬浮歌词
    var enabledFloatingLyrics by booleanState(true)
    // 歌词引擎配置
    var lyricsEngineConfig by jsonState { LyricsEngineConfig() }
    // 歌词引擎类型
    var lyricsEngineType by enumState(LyricsEngineType.Line)

    /* ------------------  社区  ------------------ */

    // 用户短 Token 时间
    var userShortToken by longState(0L)
    // 用户 Token
    var userToken by stringState("")
    // 用户 信息
    var userProfile: UserProfile? by jsonState { null }
    // 用户 头像缓存键
    var cacheUserAvatar by cacheState()
    // 用户 背景墙缓存键
    var cacheUserWall by cacheState()

    // 待编辑主题
    var editedTopic: EditedTopic? by jsonState { null }
}

@Stable
abstract class RachelApplication(delegate: PlatformContextDelegate) : PlatformApplication<RachelApplication>(appReference, delegate) {
    private val loadNativeLibrary by system(
        NativeLibrary("ylcs_native", *Platform.Desktop),
        factory = ::StartupNativeLibrary
    )

    val os by service(
        Local.info.appName,
        factory = ::StartupOS
    )

    private val createDirectories by sync {
        Platform.useNot(Platform.WebWasm) {
            SystemFileSystem.createDirectories(os.storage.dataPath)
            SystemFileSystem.createDirectories(os.storage.cachePath)
            SystemFileSystem.createDirectories(Paths.modPath)
        }
    }

    val picker by service(
        factory = ::StartupPicker
    )

    private val setupUrlImage by service(
        StartupLazyFetcher {
            Platform.use(Platform.WebWasm,
                ifTrue = { null },
                ifFalse = { os.storage.cachePath }
            )
        },
        Platform.use(*Platform.Phone, ifTrue = 400, ifFalse = 1024),
        ImageQuality.Medium,
        factory = ::StartupUrlImage
    )

    val kv by service(
        StartupLazyFetcher {
            Platform.use(*Platform.Desktop,
                ifTrue = { Path(os.storage.dataPath, "config") },
                ifFalse = { null }
            )
        },
        factory = ::StartupKV
    )

    val exceptionHandler by service(
        "crash_key",
        StartupExceptionHandler.Handler { key, e, error ->
            kv.set(key, "${DateEx.CurrentString}\n$error")
            println(e.stackTraceToString())
        },
        factory = ::StartupExceptionHandler
    )

    val config by service(
        StartupLazyFetcher { kv },
        factory = ::AppConfig,
    )

    val mp by service(
        StartupLazyFetcher { Paths.modPath },
        factory = ::buildMusicPlayer
    )

    override val themeMode: ThemeMode get() = config.themeMode
    override val fontScale: Float get() = config.fontScale
    override val mainFontResource: FontResource = Res.font.xwwk
    override val localProvider: Array<ProvidedValue<*>> get() = arrayOf(
        LocalAnimationSpeed provides config.animationSpeed,
        localBalloonTipEnabled provides config.enabledTip
    )

    @Composable
    override fun Content() {
        AppScreen<ScreenMain>(modifier = Modifier.fillMaxSize(), deeplink = this) {
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
//            screen(::ScreenMusicDetails)
//
//            screen(::ScreenMusicModFactory)
//            screen(::ScreenSongDetails, type<Song>())
//
//            screen(::ScreenImportMusic)
//            screen(::ScreenCreateMusic)
//            screen(::ScreenPlatformMusic, type<PlatformMusicType>())


            // 世界
            screen(::ScreenGameHall, type<Game>())
            screen(::ScreenGameRanking, type<Game>())
            screen(::ScreenCreateGame, type<Game>())
            screen(::ScreenPlayGame)
            screen(::ScreenGameHistory)
            screen(::ScreenGameRecordHistory)

            screen(::ScreenGuessLyrics)

            // screen(::ScreenRhyme)
        }
    }

    override fun onDeepLink(manager: ScreenManager, uri: Uri) {
        when (uri.scheme) {
            Scheme.File, Scheme.Content -> {
//                if (!mp.isReady) manager.navigate(ScreenImportMusic.Args(uri.toString()))
//                else manager.top.slot.tip.warning("请先停止播放器")
            }
            Scheme.Rachel -> {
                when (uri.path) {
                    "/openProfile" -> {
                        uri.params["uid"]?.toIntOrNull()?.let { uid ->
                            manager.navigate(ScreenUserCard.Args(uid))
                        }
                    }
                    "/openSong" -> {
//                        uri.params["id"]?.let { id ->
//                            manager.top.launch {
//                                val result = ClientAPI.request(
//                                    route = API.User.Song.GetSong,
//                                    data = id
//                                )
//                                if (result is Data.Success) manager.navigate(ScreenSongDetails.Args(result.data))
//                            }
//                        }
                    }
                }
            }
            Scheme.QQMusic -> {
//                if (!musicFactory.instance.isReady) manager.navigate(ScreenPlatformMusic.Args(
//                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
//                    type = PlatformMusicType.QQMusic
//                ))
//                else manager.top.slot.tip.warning("请先停止播放器")
            }
            Scheme.NetEaseCloud -> {
//                if (!musicFactory.instance.isReady) manager.navigate(ScreenPlatformMusic.Args(
//                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
//                    type = PlatformMusicType.NetEaseCloud
//                ))
//                else manager.top.slot.tip.warning("请先停止播放器")
            }
        }
    }
}

private val appReference = Reference<RachelApplication?>(null)
val app: RachelApplication get() = appReference.value!!
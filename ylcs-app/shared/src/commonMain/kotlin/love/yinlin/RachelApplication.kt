package love.yinlin

import androidx.compose.runtime.*
import kotlinx.io.files.Path
import love.yinlin.api.ClientEngine
import love.yinlin.common.Paths
import love.yinlin.compose.DefaultAnimationSpeed
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.ThemeMode
import love.yinlin.data.compose.ImageQuality
import love.yinlin.compose.screen.AppScreen
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.config.Patches
import love.yinlin.config.patches
import love.yinlin.data.NativeLibrary
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.LazyReference
import love.yinlin.extension.mkdir
import love.yinlin.platform.Platform
import love.yinlin.platform.lyrics.LyricsEngineConfig
import love.yinlin.platform.lyrics.LyricsEngineType
import love.yinlin.shared.resources.Res
import love.yinlin.shared.resources.xwwk
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
import love.yinlin.screen.world.battle.*
import love.yinlin.screen.world.single.rhyme.ScreenRhyme
import love.yinlin.startup.*
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import org.jetbrains.compose.resources.FontResource

@StartupFetcher(index = 0, name = "kv", returnType = StartupKV::class)
@StartupArg(index = 1, name = "version", type = Int::class)
@StartupArg(index = 2, name = "patches", type = Patches::class)
@Stable
class StartupAppConfig : StartupConfig() {
    /* ------------------  系统  ------------------ */

    // 主题模式
    var themeMode by enumState(ThemeMode.SYSTEM)
    // 动画速度
    var animationSpeed by intState(DefaultAnimationSpeed)
    // 字体大小
    var fontScale by floatState(1f, version = "20250518")
    // 悬浮提示
    var enabledTip by booleanState(true)

    /* ------------------  微博  ------------------ */

    // 微博用户
    val weiboUsers by listState { WeiboUserInfo.DEFAULT }

    /* ------------------  听歌  ------------------ */

    // 音频焦点
    var audioFocus by booleanState(true)

    // 歌单
    val playlistLibrary by mapState<String, MusicPlaylist>(version = "20251112")
    // 上次播放列表
    var lastPlaylist by stringState("")
    // 上次播放歌曲
    var lastMusic by stringState("")
    // 播放模式
    var musicPlayMode by enumState(MusicPlayMode.ORDER)
    // 开启悬浮歌词
    var enabledFloatingLyrics by booleanState(false)
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
abstract class RachelApplication(delegate: PlatformContextDelegate) : PlatformApplication<RachelApplication>(mApp, delegate), DeepLink {
    private val loadNativeLibrary by service(
        NativeLibrary("ylcs_native", *Platform.Desktop),
        priority = StartupDelegate.HIGH9,
        factory = ::StartupNativeLibrary
    )

    val os by service(
        Local.info.appName,
        priority = StartupDelegate.HIGH8,
        factory = ::StartupOS
    )

    private val createDirectories by sync {
        Platform.useNot(Platform.WebWasm) {
            os.storage.dataPath.mkdir()
            os.storage.cachePath.mkdir()
            Paths.modPath.mkdir()
        }
    }

    private val initClientBaseUrl by sync {
        ClientEngine.init(Local.API_BASE_URL)
    }

    @StartupNative
    val picker by service(
        factory = ::StartupPicker
    )

    val urlImage by service(
        useNotPlatformStartupLazyFetcher(Platform.WebWasm) { os.storage.cachePath.parent!! },
        Platform.use(*Platform.Phone, ifTrue = 400, ifFalse = 1024),
        ImageQuality.Medium,
        factory = ::StartupUrlImage
    )

    @StartupNative
    val kv by service(
        usePlatformStartupLazyFetcher(*Platform.Desktop) { Path(os.storage.dataPath, "config") },
        factory = ::StartupKV
    )

    val config by service(
        StartupLazyFetcher { kv },
        Local.info.version,
        patches(),
        factory = ::StartupAppConfig,
    )

    val exceptionHandler by service(
        "crash_key",
        StartupExceptionHandler.Handler { key, e, error ->
            kv.set(key, "${DateEx.CurrentString}\n$error")
            println(e.stackTraceToString())
        },
        factory = ::StartupExceptionHandler
    )

    val mp by service(
        useNotPlatformStartupLazyFetcher(Platform.WebWasm) { Paths.modPath },
        factory = ::buildMusicPlayer
    )

    override val themeMode: ThemeMode by derivedStateOf { config.themeMode }
    override val fontScale: Float by derivedStateOf { config.fontScale }
    override val mainFontResource: FontResource = Res.font.xwwk
    override val localProvider: Array<ProvidedValue<*>> by derivedStateOf { arrayOf(
        LocalAnimationSpeed provides config.animationSpeed,
        localBalloonTipEnabled provides config.enabledTip
    ) }

    @Composable
    override fun Content() {
        AppScreen<ScreenMain>(deeplink = this) {
            // 通用
            screen(::ScreenMain)
            screen(::ScreenImagePreview)
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
            screen(::ScreenModifyActivity)
            screen(::ScreenActivityLink)

            // 社区
            screen(::ScreenUserCard)
            screen(::ScreenTopic)
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
            screen(::ScreenModCenter)
            screen(::ScreenImportMusic)
            screen(::ScreenCreateMusic)
            screen(::ScreenPlatformMusic)


            // 世界
            screen(::ScreenGameHall)
            screen(::ScreenGameRanking)
            screen(::ScreenCreateGame)
            screen(::ScreenPlayGame)
            screen(::ScreenGameHistory)
            screen(::ScreenGameRecordHistory)

            screen(::ScreenGuessLyrics)

            screen(::ScreenRhyme)
        }
    }

    override fun onDeepLink(manager: ScreenManager, uri: Uri) {
        when (uri.scheme) {
            Scheme.File, Scheme.Content -> {
                if (!mp.isReady) manager.navigate(::ScreenImportMusic, uri)
                else manager.top.slot.tip.warning("请先停止播放器")
            }
            Scheme.Rachel -> {
                when (uri.path) {
                    "/openProfile" -> {
                        uri.params["uid"]?.toIntOrNull()?.let { uid ->
                            manager.navigate(::ScreenUserCard, uid)
                        }
                    }
                    "/openSong" -> {
                        uri.params["id"]?.let { id ->
                            manager.navigate(::ScreenMusicDetails, id)
                        }
                    }
                }
            }
            Scheme.QQMusic -> {
                if (mp.isReady) manager.top.slot.tip.warning("请先停止播放器")
                else manager.navigate(::ScreenPlatformMusic, uri.copy(scheme = Scheme.Https), PlatformMusicType.QQMusic)
            }
            Scheme.NetEaseCloud -> {
                if (mp.isReady) manager.top.slot.tip.warning("请先停止播放器")
                else manager.navigate(::ScreenPlatformMusic, uri.copy(scheme = Scheme.Https), PlatformMusicType.NetEaseCloud)
            }
        }
    }
}

@Stable
private val mApp = LazyReference<RachelApplication>()

@Stable
val app by mApp
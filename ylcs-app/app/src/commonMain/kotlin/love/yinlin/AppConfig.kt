package love.yinlin

import love.yinlin.compose.DefaultAnimationSpeed
import love.yinlin.compose.ThemeMode
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.platform.FloatingLyrics
import love.yinlin.service.StartupFetcher
import love.yinlin.startup.StartupConfig
import love.yinlin.startup.StartupKV

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
    // Android悬浮歌词配置
    var floatingLyricsAndroidConfig by jsonState { FloatingLyrics.AndroidConfig() }
    // iOS悬浮歌词配置
    var floatingLyricsIOSConfig by jsonState { FloatingLyrics.IOSConfig() }
    // 桌面悬浮歌词配置
    var floatingLyricsDesktopConfig by jsonState { FloatingLyrics.DesktopConfig() }

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
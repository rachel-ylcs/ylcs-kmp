package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.config.Patches
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.data.config.AnimationSpeedConfig
import love.yinlin.data.config.FontScaleConfig
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupFetcher
import love.yinlin.media.lyrics.LyricsEngineConfig
import love.yinlin.media.lyrics.LyricsEngineType
import love.yinlin.startup.StartupConfig
import love.yinlin.startup.StartupKV

@StartupFetcher(index = 0, name = "kv", returnType = StartupKV::class)
@StartupArg(index = 1, name = "version", type = Int::class)
@StartupArg(index = 2, name = "patches", type = Patches::class)
@Stable
class StartupAppConfig : StartupConfig() {
    /* ------------------  系统  ------------------ */

    // 主题模式
    var themeMode by enumState(ThemeMode.SYSTEM)
    // 动画速度
    var animationSpeed by enumState(AnimationSpeedConfig.STANDARD, version = "20260217")
    // 字体大小
    var fontScale by enumState(FontScaleConfig.STANDARD, version = "20260217")
    // 悬浮提示
    var enabledTip by booleanState(true)

    /* ------------------  微博  ------------------ */

    // 微博用户
    val weiboUsers by listState { WeiboUserInfo.Default }

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
    var musicPlayMode by enumState(MediaPlayMode.Default, version = "20260224")
    // 开启悬浮歌词
    var enabledFloatingLyrics by booleanState(false)
    // 歌词引擎配置
    var lyricsEngineConfig by jsonState { LyricsEngineConfig() }
    // 歌词引擎类型
    var lyricsEngineOrder by jsonState { LyricsEngineType.DefaultOrder }

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
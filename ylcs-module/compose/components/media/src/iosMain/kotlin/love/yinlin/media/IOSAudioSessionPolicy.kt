@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import platform.AVFAudio.*
import platform.Foundation.*

internal object IOSAudioSessionPolicy {
    enum class Kind { Media, Effect }
    data class Token(val kind: Kind, val mixWithOthers: Boolean = false)

    private val users = mutableSetOf<Token>()
    private var active = false
    private var unavailable = false
    private var category: String? = null
    private var options: ULong? = null

    fun acquire(token: Token) {
        check(NSThread.isMainThread)
        val added = users.add(token)
        unavailable = false
        catchingError(::configure)?.let { error ->
            if (added) {
                users.remove(token)
                // 新输出激活失败时恢复其余输出需要的配置。
                catching(::configure)
            }
            throw error
        }
    }

    fun release(token: Token) {
        check(NSThread.isMainThread)
        if (!users.remove(token) || unavailable) return

        // 清理不抛异常。失败时保留 active，后续登记/释放可以再次尝试。
        catching(::configure)
    }

    fun markUnavailable() {
        check(NSThread.isMainThread)
        active = false
        unavailable = true
        category = null
        options = null
    }

    private fun configure() {
        if (users.isEmpty()) {
            if (active) {
                iosCheck("停用音频会话失败") {
                    AVAudioSession.sharedInstance().setActive(
                        active = false,
                        withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                        error = it,
                    )
                }
                active = false
            }
            return
        }

        val media = users.filter { it.kind == Kind.Media }
        val desiredCategory = if (media.isEmpty()) AVAudioSessionCategoryAmbient else AVAudioSessionCategoryPlayback
        val desiredOptions = if (media.isNotEmpty() && media.all { it.mixWithOthers }) AVAudioSessionCategoryOptionMixWithOthers else 0UL

        // 播放音效不会把正在播放音乐/视频的 Playback 改为 Ambient。
        if (category != desiredCategory || options != desiredOptions) {
            iosCheck("配置音频会话失败") {
                AVAudioSession.sharedInstance().setCategory(
                    category = desiredCategory,
                    mode = AVAudioSessionModeDefault,
                    options = desiredOptions,
                    error = it,
                )
            }
            category = desiredCategory
            options = desiredOptions
        }
        if (!active) {
            iosCheck("激活音频会话失败") {
                AVAudioSession.sharedInstance().setActive(true, error = it)
            }
            active = true
        }
    }
}
package love.yinlin

import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import love.yinlin.media.MusicService
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.startup.StartupMusicPlayer
import kotlin.reflect.KClass

class RachelMusicService : MusicService(), LifecycleOwner {
    override val audioFocus: Boolean get() = app.config.audioFocus
    override val activityClass: KClass<out ComponentActivity> = MainActivity::class
    override val lifecycle = LifecycleRegistry(this)
    private var floatingLyrics: FloatingLyrics? = null

    override fun onCreate() {
        super.onCreate()
        lifecycle.currentState = Lifecycle.State.RESUMED
        lifecycleScope.launch {
            val player = app.requireClassOrNull<StartupMusicPlayer>() ?: awaitMusicPlayer()
            floatingLyrics = player.floatingLyrics
            player.floatingLyrics.attachService(this@RachelMusicService)
        }
    }

    private suspend fun awaitMusicPlayer(): StartupMusicPlayer = coroutineScope {
        // Service 冷启动时可能还没有 Activity，不能依赖 Activity 启动 Compose 的全局快照通知。
        val changes = Channel<Unit>(Channel.CONFLATED)
        val observer = Snapshot.registerGlobalWriteObserver { changes.trySend(Unit) }
        val notifications = launch { for (change in changes) Snapshot.sendApplyNotifications() }
        try { snapshotFlow { app.requireClassOrNull<StartupMusicPlayer>() }.filterNotNull().first() }
        finally {
            observer.dispose()
            changes.close()
            notifications.cancel()
        }
    }

    override fun onDestroy() {
        try { floatingLyrics?.detachService(this) }
        finally {
            floatingLyrics = null
            try { lifecycle.currentState = Lifecycle.State.DESTROYED }
            finally { super.onDestroy() }
        }
    }
}

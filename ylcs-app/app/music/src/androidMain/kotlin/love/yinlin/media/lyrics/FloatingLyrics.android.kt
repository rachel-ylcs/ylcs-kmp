package love.yinlin.media.lyrics

import android.view.Gravity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import love.yinlin.app
import love.yinlin.compose.ApplicationWindow
import love.yinlin.compose.Colors
import love.yinlin.compose.OverlayWindow
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.window.FloatingView
import love.yinlin.coroutines.Coroutines
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics actual constructor(val mp: StartupMusicPlayer) {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private var serviceOwner: LifecycleOwner? = null
    private var currentWindow: ApplicationWindow? = null

    fun attachService(owner: LifecycleOwner) {
        if (serviceOwner !== owner) detach()
        serviceOwner = owner
        if (app.config.enabledFloatingLyrics) attach()
    }

    fun detachService(owner: LifecycleOwner) {
        if (serviceOwner === owner) {
            serviceOwner = null
            detach()
        }
    }

    private fun createWindow(): ApplicationWindow = object : ApplicationWindow() {
        override val background = Colors.Transparent
        override val contentModifier = Modifier.fillMaxWidth()
        private var view: FloatingView? = null

        override fun uninitialize() {
            view = null
            if (currentWindow === this) {
                currentWindow = null
                isAttached = false
            }
        }

        @Composable
        override fun Window(content: @Composable () -> Unit) {
            OverlayWindow(
                touchable = false,
                onPermissionFailed = { if (currentWindow === this) app.config.enabledFloatingLyrics = false },
                onOpen = {
                    view = it
                    if (currentWindow === this) isAttached = true
                },
                onClose = { view = null },
                content = content,
            )
        }

        @Composable
        override fun Content() {
            if (isAttached && mp.isInit) {
                val screenHeight = LocalWindowInfo.current.containerSize.height
                val lyricsTopOffset by rememberDerivedState {
                    val config = app.config.lyricsEngineConfig
                    config.android.placeTop to config.android.top
                }

                LaunchedEffect(screenHeight, lyricsTopOffset) {
                    val topOffset = screenHeight * 0.2f * lyricsTopOffset.second.fastCoerceIn(0f, 1f)
                    val gravity = (if (lyricsTopOffset.first) Gravity.TOP else Gravity.BOTTOM) or Gravity.START
                    view?.updateLayoutParams(gravity, Offset(0f, topOffset))
                }

                Layout(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        if (mp.isPlaying) {
                            mp.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxWidth(), config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v6.bold)
                        }
                    }
                ) { measurables, constraints ->
                    val config = app.config.lyricsEngineConfig
                    val maxWidth = constraints.maxWidth
                    val start = (maxWidth * config.android.left.fastCoerceIn(0f, 1f)).toInt()
                    val end = (maxWidth * (1 - config.android.right).fastCoerceIn(0f, 1f)).toInt()

                    val childWidth = (maxWidth - start - end).fastCoerceAtLeast(0)
                    val placeable = measurables.firstOrNull()?.measure(constraints.copy(
                        minWidth = childWidth,
                        maxWidth = childWidth,
                        minHeight = 0
                    ))
                    layout(maxWidth, placeable?.height ?: 0) {
                        placeable?.placeRelative(start, 0)
                    }
                }
            }
        }
    }

    actual fun attach() = Coroutines.dispatchUI {
        val owner = serviceOwner
        if (owner != null && owner.lifecycle.currentState != Lifecycle.State.DESTROYED && currentWindow == null) {
            val window = createWindow()
            currentWindow = window
            app.openWindow(window, owner)
        }
    }

    actual fun detach() = Coroutines.dispatchUI {
        val window = currentWindow
        currentWindow = null
        isAttached = false
        window?.close()
    }

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }
}

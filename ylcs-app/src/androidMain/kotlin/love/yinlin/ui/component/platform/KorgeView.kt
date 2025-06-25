package love.yinlin.ui.component.platform

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import korlibs.graphics.gl.AGOpengl
import korlibs.io.Korio
import korlibs.io.android.withAndroidContext
import korlibs.kgl.KmlGlAndroid
import korlibs.korge.Korge
import korlibs.render.AndroidGameWindowNoActivity
import korlibs.render.GameWindowCreationConfig
import korlibs.render.KorgwSurfaceView
import kotlinx.coroutines.withContext
import love.yinlin.extension.catching
import love.yinlin.ui.CustomUI

@Stable
actual class KorgeState actual constructor(
    val config: GameWindowCreationConfig,
    val korge: Korge
) {
    var agOpenGl: AGOpengl? = null
    var gameWindow: AndroidGameWindowNoActivity? = null
    val glView = mutableStateOf<KorgwSurfaceView?>(null)

    fun queueEvent(runnable: Runnable) {
        glView.value?.queueEvent(runnable)
    }
}

@Composable
actual fun KorgeView(
    state: KorgeState,
    modifier: Modifier
) {
    val activity = LocalActivity.current
    CustomUI(
        view = state.glView,
        factory = { context ->
            val ag = AGOpengl(KmlGlAndroid { 3 })
            state.agOpenGl = ag
            val gameWindow = AndroidGameWindowNoActivity(
                width = state.korge.windowSize.width.toInt(),
                height = state.korge.windowSize.height.toInt(),
                ag = ag,
                androidContext = context,
                config = state.config,
                getView = { state.glView.value!! }
            )
            state.gameWindow = gameWindow
            KorgwSurfaceView(activity, context, gameWindow, state.config).apply {
                Korio(context) {
                    catching {
                        withAndroidContext(context) {
                            withContext(coroutineContext + gameWindow) {
                                state.korge.start()
                            }
                        }
                    }
                }
            }
        },
        release = { _, onRelease ->
            state.gameWindow?.apply {
                dispatchDestroyEvent()
                coroutineContext = null
                close()
                exit()
            }
            state.agOpenGl = null
            state.gameWindow = null
            onRelease()
        },
        modifier = modifier
    )
}
package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import korlibs.korge.GLCanvasWithKorge
import korlibs.korge.Korge
import korlibs.korge.glCanvas
import korlibs.render.GameWindowCreationConfig
import love.yinlin.ui.component.CustomUI

@Stable
actual class KorgeState actual constructor(
    val config: GameWindowCreationConfig,
    val korge: Korge
) {
    val glView = mutableStateOf<GLCanvasWithKorge?>(null)
}

@Composable
actual fun KorgeView(
    state: KorgeState,
    modifier: Modifier
) {
    CustomUI(
        view = state.glView,
        factory = {
            GLCanvasWithKorge(state.korge, state.korge.main)
        },
        release = { _, onRelease ->
            state.glView.value?.close()
            onRelease()
        },
        modifier = modifier
    )
}
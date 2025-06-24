package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import korlibs.korge.Korge
import korlibs.render.GameWindowCreationConfig
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

@Stable
actual class KorgeState actual constructor(
    val config: GameWindowCreationConfig,
    val korge: Korge
) {

}

@Composable
actual fun KorgeView(
    state: KorgeState,
    modifier: Modifier
) {
    LaunchedEffect(Unit) {
        thread {
            runBlocking {
                state.korge.start()
            }
        }
    }

//    CustomUI(
//        view = ,
//        factory = {
//        },
//        release = { _, onRelease ->
//            onRelease()
//        },
//        modifier = modifier,
//    )
}
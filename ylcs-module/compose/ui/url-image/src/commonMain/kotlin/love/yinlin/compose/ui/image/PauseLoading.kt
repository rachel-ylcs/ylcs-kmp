package love.yinlin.compose.ui.image

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling

@Composable
fun PauseLoading(scrollableState: ScrollableState) = bindPauseLoadWhenScrolling(scrollableState)
package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import korlibs.korge.Korge
import korlibs.render.GameWindowCreationConfig

@Stable
expect class KorgeState(
    config: GameWindowCreationConfig,
    korge: Korge
)

@Composable
expect fun KorgeView(
    state: KorgeState,
    modifier: Modifier = Modifier
)
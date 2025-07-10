package love.yinlin.ui.screen.world.single

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.extension.rememberDerivedState
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
private enum class GameState {
    Loading, // 加载中
    Start, // 开始
    PortraitLock, // 竖屏锁
    Pause, // 暂停
    Resume, // 恢复准备
    Playing, // 游戏中
    Settling, // 结算
}

@Stable
class ScreenRhyme(model: AppModel) : CommonSubScreen(model) {
    override val title: String? = null

    private var state by mutableStateOf(GameState.Loading)

    private fun resumePauseTimer() {
        state = GameState.Resume
    }

    @Composable
    private fun Stage(modifier: Modifier, constraints: Constraints) {
        Canvas(modifier = modifier) {
            drawRect(Colors.Black)
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        LaunchedEffect(device.type) {
            if (device.type == Device.Type.LANDSCAPE) {
                if (state == GameState.PortraitLock) resumePauseTimer()
            }
            else state = GameState.PortraitLock
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 状态层
            when (state) {
                GameState.Loading -> {

                }
                GameState.Start -> {

                }
                GameState.PortraitLock -> {

                }
                GameState.Pause -> {

                }
                GameState.Resume -> {

                }
                GameState.Playing -> {

                }
                GameState.Settling -> {

                }
            }
            // 遮罩层
            if (state != GameState.Playing) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickableNoRipple { }
                    .zIndex(3f)
                )
            }
            // 游戏画布层
            val widthFirstly by rememberDerivedState { constraints.maxWidth * 9 <= constraints.maxHeight * 16 }
            Stage(
                modifier = Modifier.then(if (widthFirstly) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
                    .aspectRatio(16 / 9f).zIndex(2f),
                constraints = constraints
            )
            // 背景层
            Box(modifier = Modifier.fillMaxSize().zIndex(1f))
        }
    }
}
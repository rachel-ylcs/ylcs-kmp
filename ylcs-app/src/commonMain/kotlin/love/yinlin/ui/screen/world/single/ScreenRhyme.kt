package love.yinlin.ui.screen.world.single

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
private enum class GameState {
    Loading, // 加载中
    Start, // 开始
    Playing, // 游戏中
    Settling, // 结算
}

@Stable
private sealed interface GameLockState {
    @Stable
    data object Normal : GameLockState // 正常
    @Stable
    data object PortraitLock : GameLockState // 竖屏锁
    @Stable
    data object Pause : GameLockState // 暂停
    @Stable
    data class Resume(val time: Int) : GameLockState // 恢复准备
}

@Stable
class ScreenRhyme(model: AppModel) : CommonSubScreen(model) {
    private var state: GameState by mutableStateOf(Loading)
    private var lockState: GameLockState by mutableStateOf(Normal)

    private fun pauseGame() {

    }

    private fun resumePauseTimer() {
        lockState = GameLockState.Resume(3)
    }

    @Composable
    private fun GameOverlayLoading() {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LoadingBox()
        }
    }

    @Composable
    private fun GameOverlayStart() {

    }

    @Composable
    private fun GameOverlayPlaying() {

    }

    @Composable
    private fun GameOverlaySettling() {

    }

    @Composable
    private fun GameOverlay(modifier: Modifier) {
        Box(modifier = modifier) {
            when (state) {
                Loading -> GameOverlayLoading()
                Start -> GameOverlayStart()
                Playing -> GameOverlayPlaying()
                Settling -> GameOverlaySettling()
            }
        }
    }

    @Composable
    private fun GameScrimMask(modifier: Modifier) {
        Box(modifier = modifier) {
            if (lockState !is Normal) {
                Box(modifier = modifier
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickableNoRipple { }
                )
            }
        }
    }

    @Composable
    private fun GameStage(modifier: Modifier) {
        Canvas(modifier = modifier) {
            val scale = 1920 / size.width
        }
    }

    @Composable
    private fun GameBackground(modifier: Modifier) {
        Box(modifier = modifier)
    }

    override val title: String? = null

    override suspend fun initialize() {
        // 加载游戏
        delay(3000L)
        state = Start
    }

    @Composable
    override fun SubContent(device: Device) {
        LaunchedEffect(device.type) {
            if (device.type == LANDSCAPE) {
                if (lockState is PortraitLock) resumePauseTimer()
            }
            else {
                lockState = PortraitLock
                pauseGame()
            }
        }

        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                // 状态层
                GameOverlay(modifier = Modifier.fillMaxSize().zIndex(4f))
                // 遮罩层
                GameScrimMask(modifier = Modifier.fillMaxSize().zIndex(3f))
                // 画布层
                GameStage(modifier = Modifier.fillMaxSize().zIndex(2f))
                // 背景层
                GameBackground(modifier = Modifier.fillMaxSize().zIndex(1f))
            },
            measurePolicy = { measurables, constraints ->
                val maxWidth = constraints.maxWidth
                val maxHeight = constraints.maxHeight
                val useWidthFirst = maxWidth * 9 <= maxHeight * 16
                val childWidth = if (useWidthFirst) maxWidth else maxHeight * 16 / 9
                val childHeight = if (useWidthFirst) maxWidth * 9 / 16 else maxHeight
                val childConstraints = Constraints.fixed(childWidth, childHeight)
                val placeables = measurables.map { it.measure(childConstraints) }
                layout(childWidth, childHeight) {
                    placeables.forEach { it.placeRelative(0, 0) }
                }
            }
        )
    }
}
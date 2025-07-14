package love.yinlin.ui.screen.world.single

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.game.Game
import love.yinlin.platform.Coroutines
import love.yinlin.platform.app
import love.yinlin.ui.component.animation.AnimationLayout
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.SplitLayout
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.screen.music.recordPath
import love.yinlin.ui.screen.music.rhymePath
import kotlin.random.Random

@Stable
private enum class GameState {
    Loading, // 加载中
    Start, // 开始
    MusicLibrary, // 音乐库
    MusicDetails, // 音乐详情
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

@Composable
private fun GameButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val acrylicColor = Colors.from(0x99F2F2F2)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    acrylicColor.copy(alpha = 0.6f),
                                    acrylicColor.copy(alpha = 0.3f)
                                )
                            ),
                            blendMode = BlendMode.Overlay
                        )
                    }
                }
                .border(
                    width = ThemeValue.Border.Small,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Colors.White.copy(alpha = 0.5f),
                            Colors.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .background(acrylicColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.padding(ThemeValue.Padding.InnerIcon * 2f).size(ThemeValue.Size.Icon * 1.5f),
                imageVector = icon,
                contentDescription = null,
                tint = Colors.Ghost,
            )
        }
    }
}

@Composable
private fun GameMusicCard(
    musicInfo: MusicInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        shadowElevation = ThemeValue.Shadow.MiniSurface,
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.aspectRatio(1f).fillMaxHeight()) {
                LocalFileImage(
                    path = { musicInfo.recordPath },
                    musicInfo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(ThemeValue.Padding.ExtraValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
            ) {
                Text(
                    text = musicInfo.name,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = musicInfo.singer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Stable
class ScreenRhyme(model: AppModel) : CommonSubScreen(model) {
    private var state: GameState by mutableStateOf(GameState.Loading)
    private var lockState: GameLockState by mutableStateOf(GameLockState.Normal)

    private var library = emptyList<MusicInfo>()

    private var resumePauseJob: Job? = null

    private fun onScreenOrientationChanged(type: Device.Type) {
        if (type == Device.Type.LANDSCAPE) {
            // 如果处于竖屏锁, 当转回横屏后启动恢复协程
            if (lockState is GameLockState.PortraitLock) resumePauseTimer()
        }
        else {
            // 转回竖屏后, 如果处于恢复状态则取消恢复协程
            resumePauseJob?.cancel()
            lockState = GameLockState.PortraitLock
            pauseGame()
        }
    }

    private fun pauseGame() {

    }

    private fun resumePauseTimer() {
        if (resumePauseJob == null) {
            resumePauseJob = launch {
                try {
                    // 倒计时 3 秒后解除暂停状态
                    repeat(3) {
                        lockState = GameLockState.Resume(3 - it)
                        delay(1000L)
                    }
                    lockState = GameLockState.Normal
                }
                finally {
                    resumePauseJob = null
                }
            }
        }
    }

    @Composable
    private fun GameOverlayLoading() {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingBox(
                text = "加载曲库中...",
                color = Colors.White
            )
        }
    }

    @Composable
    private fun GameOverlayStart() {
        Box(modifier = Modifier.fillMaxSize()) {
            WebImage(
                uri = Game.Rhyme.zPath,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue).zIndex(2f),
                horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace,
                verticalAlignment = Alignment.CenterVertically,
                left = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            onClick = { onBack() }
                        )
                    }
                },
                right = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameButton(
                            icon = Icons.Outlined.MusicNote,
                            onClick = { state = GameState.MusicLibrary }
                        )
                    }
                }
            )
        }
    }

    @Composable
    private fun GameMusicLibrary() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = { onBack() }
                )
                Text(
                    text = "曲库",
                    style = MaterialTheme.typography.titleLarge,
                    color = Colors.White
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (library.isEmpty()) {
                    EmptyBox(
                        text = "曲库中无支持的音乐MOD",
                        color = Colors.White
                    )
                }
                else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(ThemeValue.Size.CellWidth * 1.25f),
                        contentPadding = PaddingValues(horizontal = ThemeValue.Padding.EqualSpace),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = library,
                            key = { it.id }
                        ) {
                            GameMusicCard(
                                musicInfo = it,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {

                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GameMusicDetails() {

    }

    @Composable
    private fun GameOverlayPlaying() {

    }

    @Composable
    private fun GameOverlaySettling() {

    }

    @Composable
    private fun GameScrimMask(modifier: Modifier) {
        Box(modifier = modifier) {
            if (lockState !is GameLockState.Normal) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                        .clickableNoRipple { },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(ThemeValue.Padding.HorizontalExtraSpace * 2)
                            .shadow(ThemeValue.Shadow.Surface, MaterialTheme.shapes.extraLarge)
                            .width(ThemeValue.Size.PanelWidth)
                            .heightIn(max = ThemeValue.Size.PanelWidth)
                            .background(Colors.Gray8, MaterialTheme.shapes.extraLarge)
                            .padding(ThemeValue.Padding.HorizontalExtraSpace * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val currentLockState = lockState) {
                            GameLockState.Normal -> {}
                            GameLockState.PortraitLock -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
                                ) {
                                    MiniIcon(
                                        icon = Icons.Outlined.Lock,
                                        color = Colors.White,
                                        size = ThemeValue.Size.Icon * 2
                                    )
                                    Text(
                                        text = "已锁定, 请保持设备横屏",
                                        color = Colors.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                            GameLockState.Pause -> {

                            }
                            is GameLockState.Resume -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
                                ) {
                                    Text(
                                        text = "请准备好",
                                        color = Colors.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = currentLockState.time.toString(),
                                        color = Colors.White,
                                        style = MaterialTheme.typography.displayMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GameOverlay(modifier: Modifier) {
        Box(modifier = modifier) {
            AnimationLayout(state = state) {
                when (it) {
                    GameState.Loading -> GameOverlayLoading()
                    GameState.Start -> GameOverlayStart()
                    GameState.MusicLibrary -> GameMusicLibrary()
                    GameState.MusicDetails -> GameMusicDetails()
                    GameState.Playing -> GameOverlayPlaying()
                    GameState.Settling -> GameOverlaySettling()
                }
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
        if (app.musicFactory.isInit) {
            // 加载游戏
            Coroutines.io {
                // TODO: 结束测试后去除条件
                val testOption = Random.nextInt(1, 2) > 0
                // 检查包含游戏配置文件的 MOD
                library = app.musicFactory.musicLibrary.values.filter { info ->
                    SystemFileSystem.metadataOrNull(info.rhymePath)?.isRegularFile == true || testOption
                }
            }
            delay(1000)
        }
        // 切换到首页
        state = GameState.Start
    }

    override fun onBack() {
        if (lockState is GameLockState.Normal) {
            when (state) {
                GameState.Loading, GameState.Start -> pop()
                GameState.MusicLibrary -> state = GameState.Start
                GameState.MusicDetails -> state = GameState.MusicLibrary
                GameState.Playing -> pauseGame()
                GameState.Settling -> state = GameState.Start
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        LaunchedEffect(device.type) {
            onScreenOrientationChanged(device.type)
        }

        Layout(
            modifier = Modifier.background(Colors.Black).fillMaxSize(),
            content = {
                // 遮罩层
                GameScrimMask(modifier = Modifier.fillMaxSize().zIndex(4f))
                // 状态层
                GameOverlay(modifier = Modifier.fillMaxSize().zIndex(3f))
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
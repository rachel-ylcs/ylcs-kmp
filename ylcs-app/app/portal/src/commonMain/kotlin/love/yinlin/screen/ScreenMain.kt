package love.yinlin.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.app.portal.resources.*
import love.yinlin.compose.*
import love.yinlin.compose.ds.DataSourceAccount
import love.yinlin.compose.ds.DataSourceActivity
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.PlayControlUI
import love.yinlin.compose.ui.common.PortalCardItem
import love.yinlin.compose.ui.container.Banner
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.widget.Calendar
import love.yinlin.compose.ui.widget.CalendarState
import love.yinlin.cs.url
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.then
import love.yinlin.isAppInitialized
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenMain : BasicScreen() {
    override fun onBack() {
        if (isAppInitialized) app.backHome()
    }

    override fun initialize() {
        launch {
            // 更新用户信息
            if (!DataSourceAccount.updateUserToken()) navigate(::ScreenLogin)

            supervisorScope {
                this.launch { DataSourceActivity.requestNewActivity() } // 加载轮播图
            }
        }
    }

    private val musicPlayer by derivedStateOf { app.requireClassOrNull<StartupMusicPlayer>() }

    private val headerLayout = movableComposable { modifier: Modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val username = app.config.userProfile?.name
            val helloText = remember(username) {
                val time = when (DateEx.Current.hour) {
                    in 6 .. 10 -> "早上"
                    in 11 .. 13 -> "中午"
                    in 14 .. 17 -> "下午"
                    in 18 .. 23 -> "晚上"
                    else -> "凌晨"
                }
                val name = if (username != null) "，$username" else ""
                "${time}好$name"
            }

            SimpleEllipsisText(
                text = helloText,
                modifier = Modifier.weight(1f),
                style = Theme.typography.v5.bold
            )

            val userProfile = app.config.userProfile

            if (userProfile != null) {
                WebImage(
                    source = userProfile.avatarPath.url,
                    version = app.config.cacheUserAvatar.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image9),
                    options = ImageOptions.Circular,
                    onClick = {
                        navigate(::ScreenUser)
                    }
                )
            }
            else {
                PrimaryButton(
                    text = "登录",
                    icon = Icons.VerifiedUser,
                    onClick = {
                        navigate(::ScreenLogin)
                    }
                )
            }
        }
    }

    private val bannerLayout = movableComposable { modifier: Modifier ->
        val pics by rememberDerivedState { DataSourceActivity.activities.fastFilter { it.photo.coverPath != null } }

        Banner(
            size = pics.size,
            modifier = modifier,
            key = { pics[it].aid },
            interval = 5000L
        ) { index ->
            val activity = pics[index]
            WebImage(
                source = activity.photo.coverPath?.url,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onClick = {
                    navigate(::ScreenActivityDetails, activity.aid)
                }
            )
        }
    }

    private val calendarState = CalendarState()

    private val activityLayout = movableComposable { modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.eValue,
            shape = Theme.shape.v1,
            shadowElevation = Theme.shadow.v3
        ) {
            Calendar(
                state = calendarState,
                events = DataSourceActivity.activityEvents,
                onEventClick = { date ->
                    DateEx.Formatter.standardDate.format(date)
                        ?.findSelf(DataSourceActivity.activities) { it.ts }
                        ?.then { navigate(::ScreenActivityDetails, it.aid) }
                }
            )
        }
    }

    private val musicInactivePaint = Paint().apply { alpha = 0.3f }
    private val musicOverlayBrush = Brush.horizontalGradient(
        0f to Colors.White.copy(alpha = 0.3f),
        0.25f to Colors.White.copy(alpha = 0.5f),
        1f to Colors.White.copy(alpha = 0.85f)
    )

    private val musicLayout = movableComposable { modifier: Modifier ->
        Theme.ThemeModeWrapper(true) {
            Surface(
                modifier = modifier,
                shape = Theme.shape.v7,
                shadowElevation = Theme.shadow.v7,
                border = BorderStroke(Theme.border.v7, Theme.color.outline)
            ) {
                val player = musicPlayer
                val musicInfo = player?.currentMusic

                if (musicInfo != null) {
                    LocalFileImage(
                        source = musicInfo.path(app.modPath, ModResourceType.Background),
                        version = musicInfo.id,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }.drawWithCache {
                            val position = player.position
                            val duration = player.duration
                            val progress = if (duration == 0L) 0f else position.toFloat() / duration
                            onDrawWithContent {
                                val w = size.width
                                val splitX = w * progress
                                clipRect(right = splitX) {
                                    this@onDrawWithContent.drawContent()
                                    drawRect(brush = musicOverlayBrush, blendMode = BlendMode.DstIn)
                                }
                                clipRect(left = splitX, right = w) {
                                    drawIntoCanvas { canvas ->
                                        canvas.withSaveLayer(Rect(splitX, 0f, w, size.height), musicInactivePaint) {
                                            this@onDrawWithContent.drawContent()
                                        }
                                    }
                                }
                            }
                        }.zIndex(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { navigate(::ScreenMusic) }
                        .padding(Theme.padding.eValue)
                        .zIndex(2f),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shape = Theme.shape.v8
                    if (musicInfo != null) {
                        LocalFileImage(
                            source = musicInfo.path(app.modPath, ModResourceType.Record),
                            version = musicInfo.id,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(Theme.size.image8).clip(shape).border(Theme.border.v9, Theme.color.outline.copy(alpha = 0.5f), shape),
                        )
                    }
                    else {
                        Box(modifier = Modifier.size(Theme.size.image8).clip(shape).background(Theme.color.backgroundVariant))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        SimpleEllipsisText(
                            text = musicInfo?.name ?: "未知歌曲",
                            style = Theme.typography.v6.bold
                        )
                        SimpleEllipsisText(
                            text = musicInfo?.singer ?: "未知歌手",
                            style = Theme.typography.v7,
                            color = Theme.color.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayControlUI(
                            size = Theme.size.smallIcon,
                            isPlaying = player?.isPlaying ?: false,
                            onPrevious = {
                                if (player != null && player.isReady) {
                                    launch { player.gotoPrevious() }
                                }
                            },
                            onNext = {
                                if (player != null && player.isReady) {
                                    launch { player.gotoNext() }
                                }
                            },
                            onPlay = {
                                if (player != null && player.isReady) {
                                    launch {
                                        if (player.isPlaying) player.pause()
                                        else player.play()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private val cardList = [
        PortalCardItem(
            eyebrow = "INFORMATION",
            title = "资讯",
            subtitle = "漫游与新鲜现场",
            drawable = Res.drawable.card_information,
            lightColors = [Color(0xFF9FBFC5), Color(0xFFF3FAFB), Color(0xFFB7E2EA)],
            darkColors = [Color(0xFF52D9E8), Color(0xFF17323A), Color(0xFF151B20)],
            onClick = {
                navigate(::ScreenInformation)
            }
        ),
        PortalCardItem(
            eyebrow = "PHOTO",
            title = "图集",
            subtitle = "定格与闪耀瞬间",
            drawable = Res.drawable.card_photo,
            lightColors = [Color(0xFFB8AFD2), Color(0xFFF8F6FC), Color(0xFFD5C8F0)],
            darkColors = [Color(0xFFA593FF), Color(0xFF2A2340), Color(0xFF181B23)],
            onClick = {
                navigate(::ScreenPhotoAlbum)
            }
        ),
        PortalCardItem(
            eyebrow = "COMMUNITY",
            title = "社区",
            subtitle = "分享与热爱共鸣",
            drawable = Res.drawable.card_community,
            lightColors = [Color(0xFFCFB1A9), Color(0xFFFCF7F4), Color(0xFFF1C7B6)],
            darkColors = [Color(0xFFFF7C68), Color(0xFF3A231F), Color(0xFF1D191A)],
            onClick = {
                navigate(::ScreenCommunity)
            }
        ),
        PortalCardItem(
            eyebrow = "WORLD",
            title = "世界",
            subtitle = "探索与无限可能",
            drawable = Res.drawable.card_world,
            lightColors = [Color(0xFFB8C394), Color(0xFFF9FBF3), Color(0xFFD4E3AA)],
            darkColors = [Color(0xFFD9FF63), Color(0xFF29301A), Color(0xFF181C16)],
            onClick = {
                navigate(::ScreenWorld)
            }
        ),
        PortalCardItem(
            eyebrow = "RADIO",
            title = "电台",
            subtitle = "聆听与声浪共鸣",
            drawable = Res.drawable.card_radio,
            lightColors = [Color(0xFFCCB78F), Color(0xFFFCF9F1), Color(0xFFEED5A4)],
            darkColors = [Color(0xFFFFB84D), Color(0xFF3B2C19), Color(0xFF1D1A17)],
            onClick = {
                navigate(::ScreenRadio)
            }
        ),
        PortalCardItem(
            eyebrow = "GAME",
            title = "娱乐",
            subtitle = "曲谱与音律狂欢",
            drawable = Res.drawable.card_game,
            lightColors = [Color(0xFFCCA8BF), Color(0xFFFCF5F9), Color(0xFFEDBDD9)],
            darkColors = [Color(0xFFFF70C8), Color(0xFF3A2032), Color(0xFF1D1820)],
            onClick = {
                navigate(::ScreenGame)
            }
        ),
    ]

    private val cardLayout = movableComposable { modifier: Modifier ->
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
            maxItemsInEachRow = 2
        ) {
            for (card in cardList) {
                card.Content(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.value9)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            headerLayout(Modifier.fillMaxWidth())
            bannerLayout(Modifier.fillMaxWidth().aspectRatio(1.77778f).clip(Theme.shape.v7))
            musicLayout(Modifier.fillMaxWidth())
            cardLayout(Modifier.fillMaxWidth())
            activityLayout(Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.value9)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            headerLayout(Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7)
            ) {
                bannerLayout(Modifier.weight(1f).aspectRatio(1.77778f).clip(Theme.shape.v7))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                ) {
                    activityLayout(Modifier.fillMaxWidth())
                    cardLayout(Modifier.fillMaxWidth())
                }
            }

            Box(modifier = Modifier.weight(1f))
            musicLayout(Modifier.fillMaxWidth())
        }
    }

    @Composable
    override fun BasicContent() {
        val deviceType by rememberDeviceType()
        when (deviceType) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }
}
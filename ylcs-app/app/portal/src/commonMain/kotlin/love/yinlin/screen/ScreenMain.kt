package love.yinlin.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.number
import love.yinlin.app
import love.yinlin.app.portal.resources.*
import love.yinlin.compose.*
import love.yinlin.compose.ds.DataSourceAccount
import love.yinlin.compose.ds.DataSourceActivity
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.PlayControlUI
import love.yinlin.compose.ui.common.PortalCardItem
import love.yinlin.compose.ui.container.Banner
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogConfirm
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.layout.Grid
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.widget.Calendar
import love.yinlin.compose.ui.widget.CalendarState
import love.yinlin.cs.url
import love.yinlin.data.common.PWAInstallation
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.then
import love.yinlin.isAppInitialized
import love.yinlin.platform.Platform
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.uri.UriGenerator

@Stable
class ScreenMain : BasicScreen() {
    private suspend fun installPWA() {
        Platform.use(*Platform.Web) {
            if (PWAInstallation.shouldSuggest) {
                val install = PWAInstallation.installAction
                if (install != null) {
                    val result = webAppInstallDialog.open(
                        title = "安装银临茶舍 PWA",
                        content = """
渐进式网页应用(PWA) 将网页安装成设备上的APP
- 从桌面图标或开始菜单直接打开
- 脱离浏览器在独立窗口使用
- 支持更多银临茶舍网页版特有功能
关闭后仍可在浏览器菜单「安装应用」或「添加到主屏幕」找到
                    """.trimIndent())

                    if (result) install()
                }
            }
        }
    }

    override fun onBack() {
        if (isAppInitialized) app.backHome()
    }

    override fun initialize() {
        launch {
            val token = app.config.userToken

            supervisorScope {
                this.launch { installPWA() } // 安装 PWA

                this.launch {
                    if (!DataSourceAccount.updateUserToken(token)) navigate(::ScreenLogin) // 更新用户信息
                }

                this.launch { DataSourceActivity.requestNewActivity(token) } // 加载轮播图
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
            val userProfile = app.config.userProfile

            if (userProfile == null) {
                SimpleEllipsisText(
                    text = "登录体验更多内容",
                    modifier = Modifier.weight(1f),
                    style = Theme.typography.v5.bold
                )

                PrimaryButton(
                    text = "登录",
                    icon = Icons.VerifiedUser,
                    onClick = { navigate(::ScreenLogin) }
                )
            }
            else {
                val helloText = remember(userProfile) {
                    val time = when (DateEx.Current.hour) {
                        in 6 .. 10 -> "早上"
                        in 11 .. 13 -> "中午"
                        in 14 .. 17 -> "下午"
                        in 18 .. 23 -> "晚上"
                        else -> "凌晨"
                    }
                    "${time}好，${userProfile.name}"
                }

                WebImage(
                    source = userProfile.avatarPath.url,
                    version = app.config.cacheUserAvatar.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image9),
                    options = ImageOptions.Circular,
                    onClick = { navigate(::ScreenUser) }
                )

                SimpleEllipsisText(
                    text = helloText,
                    modifier = Modifier.weight(1f).silentClick { navigate(::ScreenUser) },
                    style = Theme.typography.v5.bold
                )
            }

            Icon(
                icon = Icons.Settings,
                onClick = { navigate(::ScreenSettings) }
            )
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
            (val aid, val photo) = pics[index]
            WebImage(
                source = photo.coverPath?.url,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onClick = { navigate(::ScreenActivityDetails, aid) }
            )
        }
    }

    private val activityLayout = movableComposable { modifier: Modifier ->
        Surface(
            modifier = modifier,
            shape = Theme.shape.v7,
            shadowElevation = Theme.shadow.v7,
            border = BorderStroke(Theme.border.v7, Theme.color.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showCalendar by rememberFalse()
                val [futureActivity, pastActivity] = DataSourceActivity.nearestActivity

                Column(modifier = Modifier.weight(1f)) {
                    if (futureActivity == null) SimpleEllipsisText(text = ">> 暂无最新活动...", modifier = Modifier.padding(Theme.padding.value9))
                    else {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navigate(::ScreenActivityDetails, futureActivity.activity.aid)
                            }.padding(Theme.padding.value9),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                        ) {
                            val dayText = if (futureActivity.day == 0) ">> 今天" else ">> 还有${(futureActivity.day)}天"

                            SimpleEllipsisText(text = dayText, color = Theme.color.secondary, style = Theme.typography.v7.bold)
                            SimpleEllipsisText(text = futureActivity.title, style = Theme.typography.v7.bold)
                        }
                    }

                    if (pastActivity != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navigate(::ScreenActivityDetails, pastActivity.activity.aid)
                            }.padding(Theme.padding.value9),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                        ) {
                            val dayText = if (pastActivity.day == 0) "<< 今天" else "<< 已过${pastActivity.day}天"

                            SimpleEllipsisText(text = dayText, color = Theme.color.tertiary, style = Theme.typography.v7.bold)
                            SimpleEllipsisText(text = pastActivity.title, style = Theme.typography.v7.bold)
                        }
                    }
                }

                Flyout(
                    visible = showCalendar,
                    onClickOutside = { showCalendar = false },
                    flyout = {
                        Surface(
                            contentPadding = Theme.padding.eValue,
                            shape = Theme.shape.v1,
                            shadowElevation = Theme.shadow.v3,
                            border = BorderStroke(Theme.border.v7, Theme.color.outline)
                        ) {
                            val calendarState = remember { CalendarState() }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                            ) {
                                val currentDate = calendarState.currentDate

                                SimpleEllipsisText(
                                    text = "${currentDate.year} 年 ${currentDate.month.number} 月",
                                    style = Theme.typography.v6.bold
                                )

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
                    }
                ) {
                    PrimaryTextButton(
                        text = "活动日历",
                        icon = Icons.Calendar,
                        onClick = { showCalendar = true }
                    )
                }
            }
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
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
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
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                maxItemsInEachRow = 2
            ) {
                for (card in cardList) {
                    card.Content(modifier = Modifier.weight(1f))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = Theme.padding.eValue9,
                shape = Theme.shape.v7,
                shadowElevation = Theme.shadow.v7
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Link, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = "友情链接", style = Theme.typography.v6.bold, modifier = Modifier.idText())
                    }

                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        PrimaryTextButton(
                            text = "茶舍水群",
                            icon = Icons2.QQ,
                            onClick = {
                                launch {
                                    val uri = Platform.use(*Platform.Phone, ifTrue = UriGenerator.qqGroup("828049503"), ifFalse = UriGenerator.qqGroupLink("eAli22ljj4"))
                                    if (!app.openUri(uri.toString())) slot.tip.warning("未安装QQ")
                                }
                            }
                        )

                        SecondaryTextButton(
                            text = "茶舍小店",
                            icon = Icons2.Taobao,
                            onClick = {
                                launch {
                                    if (!app.openUri(UriGenerator.taobao("280201975").toString())) slot.tip.warning("未安装淘宝")
                                }
                            }
                        )
                    }
                }
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
            activityLayout(Modifier.fillMaxWidth())
            musicLayout(Modifier.fillMaxWidth())
            cardLayout(Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape() {
        val space = Theme.padding.e9

        Grid(
            builder = {
                x(minmax(), minmax())
                y(auto, auto, 1, auto)
                s(space)
            },
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.value9)
        ) {
            headerLayout(Modifier.fillMaxWidth().item(xs = 2))
            bannerLayout(Modifier.fillMaxWidth().aspectRatio(1.77778f).clip(Theme.shape.v7))
            cardLayout(Modifier.fillMaxWidth().item(ys = 2))
            activityLayout(Modifier.fillMaxWidth())
            musicLayout(Modifier.fillMaxWidth().item(xs = 2))
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

    private val webAppInstallDialog = this land DialogConfirm()
}

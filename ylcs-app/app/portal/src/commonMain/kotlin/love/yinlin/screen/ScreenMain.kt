package love.yinlin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.zIndex
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.number
import love.yinlin.app
import love.yinlin.app.portal.resources.*
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceAccount
import love.yinlin.compose.ds.DataSourceActivity
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.PlayControlUI
import love.yinlin.compose.ui.common.PortalCardItem
import love.yinlin.compose.ui.container.Banner
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.url
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.DateEx
import love.yinlin.isAppInitialized
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenMain : BasicScreen() {
    override fun onBack() {
        if (isAppInitialized) app.backHome()
    }

    override suspend fun initialize() {
        // 更新用户信息
        if (!DataSourceAccount.updateUserToken()) navigate(::ScreenLogin)

        supervisorScope {
            launch { DataSourceActivity.requestNewActivity() } // 加载轮播图
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

            WebImage(
                uri = app.config.userProfile?.avatarPath?.url ?: "",
                key = app.config.cacheUserAvatar,
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.size(Theme.size.image9),
                onClick = {
                    navigate(::ScreenUser)
                }
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
            val activity = pics[index]
            WebImage(
                uri = activity.photo.coverPath?.url ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onClick = {
                    navigate(::ScreenActivityDetails, activity.aid)
                }
            )
        }
    }

    private val activityLayout = movableComposable { modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v7,
            shadowElevation = Theme.shadow.v7
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                val today = remember { DateEx.Today }

                val todayTitle by rememberDerivedState {
                    val activity = DataSourceActivity.spanActivities[5].second
                    activity?.shortTitle ?: activity?.title
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monthText = remember {
                        val table = ["", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"]
                        "${table[today.month.number]}月"
                    }

                    SimpleClipText(
                        text = monthText,
                        style = Theme.typography.v6.bold
                    )

                    todayTitle?.let {
                        SimpleEllipsisText(
                            text = it,
                            color = Theme.color.primary,
                            style = Theme.typography.v6.bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val state = rememberLazyListState(initialFirstVisibleItemIndex = 4)

                HorizontalScrollContainer(state, modifier = Modifier.fillMaxWidth()) {
                    val shape = Theme.shape.v7
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.e)
                    ) {
                        items(DataSourceActivity.spanActivities) { [date, activity] ->
                            val itemWidth = if (activity != null) Theme.size.cell7 else Theme.size.cell10
                            val itemBackground = when {
                                date == today -> Theme.color.secondaryContainer.copy(alpha = 0.75f)
                                activity != null -> Theme.color.primaryContainer.copy(alpha = 0.5f)
                                else -> Colors.Transparent
                            }
                            val dateStyle = if (date == today || activity == null) Theme.typography.v6.bold else Theme.typography.v7.bold
                            val itemColor = when {
                                date == today || activity != null -> Theme.color.onContainer
                                date < today -> Theme.color.onSurfaceVariant.copy(alpha = 0.5f)
                                else -> Theme.color.onSurface
                            }
                            val borderAlpha = if (date < today && activity == null) 0.5f else 1f

                            Box(
                                modifier = Modifier.width(itemWidth)
                                    .height(Theme.size.cell10)
                                    .clip(shape)
                                    .background(itemBackground)
                                    .border(Theme.border.v8, Theme.color.outline.copy(alpha = borderAlpha), shape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activity == null) {
                                    SimpleClipText(
                                        text = date.day.toString(),
                                        style = dateStyle,
                                        color = itemColor
                                    )
                                }
                                else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().clickable {
                                            navigate(::ScreenActivityDetails, activity.aid)
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.CenterVertically)
                                    ) {
                                        SimpleEllipsisText(
                                            text = activity.shortTitle ?: activity.title ?: "未知活动",
                                            style = dateStyle,
                                            color = itemColor
                                        )
                                        SimpleEllipsisText(
                                            text = date.day.toString(),
                                            style = dateStyle,
                                            color = itemColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val musicLayout = movableComposable { modifier: Modifier ->
        Surface(
            modifier = modifier,
            shape = Theme.shape.v7,
            shadowElevation = Theme.shadow.v7,
            border = BorderStroke(Theme.border.v7, Theme.color.outline)
        ) {
            val player = musicPlayer
            val musicInfo = player?.currentMusic

            if (musicInfo != null) {
                val path = musicInfo.path(app.modPath, ModResourceType.Background).path
                LocalFileImage(
                    uri = path,
                    musicInfo,
                    contentScale = ContentScale.Crop,
                    alpha = 0.2f,
                    modifier = Modifier.matchParentSize().zIndex(1f)
                )
                LocalFileImage(
                    uri = path,
                    musicInfo,
                    contentScale = ContentScale.Crop,
                    alpha = 0.75f,
                    modifier = Modifier.matchParentSize().zIndex(2f).drawWithContent {
                        val position = player.position
                        val duration = player.duration
                        val progress = if (duration == 0L) 0f else position.toFloat() / duration
                        clipRect(right = size.width * progress) {
                            this@drawWithContent.drawContent()
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { navigate(::ScreenMusic) }
                    .padding(Theme.padding.eValue)
                    .zIndex(3f),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shape = Theme.shape.v8
                if (musicInfo != null) {
                    LocalFileImage(
                        uri = musicInfo.path(app.modPath, ModResourceType.Record).path,
                        musicInfo,
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

    private val cardList = [
        PortalCardItem(
            eyebrow = "INFORMATION",
            title = "资讯",
            subtitle = "漫游与新鲜现场",
            drawable = Res.drawable.card_information,
            lightColors = [Color(0xFF52D9E8), Color(0xFF80E1EC), Color(0xFFADEBF2)],
            darkColors = [Color(0xFF52D9E8), Color(0xFF17323A), Color(0xFF151B20)],
            onClick = {

            }
        ),
        PortalCardItem(
            eyebrow = "PHOTO",
            title = "图集",
            subtitle = "定格与闪耀瞬间",
            drawable = Res.drawable.card_photo,
            lightColors = [Color(0xFFA593FF), Color(0xFFB8A9FF), Color(0xFFCFC4FF)],
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
            lightColors = [Color(0xFFFF7C68), Color(0xFFFF9B88), Color(0xFFFFBBAA)],
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
            lightColors = [Color(0xFFD9FF63), Color(0xFFE1FF83), Color(0xFFE9FFA6)],
            darkColors = [Color(0xFFD9FF63), Color(0xFF29301A), Color(0xFF181C16)],
            onClick = {
                navigate(::ScreenWorld)
            }
        )
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
            activityLayout(Modifier.fillMaxWidth())
            musicLayout(Modifier.fillMaxWidth())
            cardLayout(Modifier.fillMaxWidth())
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
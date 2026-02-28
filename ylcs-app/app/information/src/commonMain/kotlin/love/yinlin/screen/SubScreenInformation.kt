package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastFilter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import love.yinlin.app
import love.yinlin.common.DataSourceInformation
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Banner
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.widget.Calendar
import love.yinlin.compose.ui.widget.CalendarState
import love.yinlin.cs.ApiActivityAddActivity
import love.yinlin.cs.ApiActivityGetActivities
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.replaceAll
import kotlin.math.abs

@Stable
class SubScreenInformation(parent: NavigationScreen) : SubScreen(parent) {
    private val activities = DataSourceInformation.activities

    private val activityEvents by derivedStateOf {
        activities.asSequence().mapNotNull { activity ->
            val date = activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }
            val title = activity.shortTitle ?: activity.title
            if (date != null && title != null) date.toEpochDays() to title
            else null
        }.toMap()
    }

    private val calendarState = CalendarState()

    private suspend fun requestActivity() {
        ApiActivityGetActivities.request(app.config.userToken.ifEmpty { null }) { activities.replaceAll(it.sorted()) }
    }

    private suspend fun addActivity(hide: Boolean) {
        ApiActivityAddActivity.request(app.config.userToken, hide) { requestActivity() }.errorTip
    }

    override suspend fun initialize() {
        requestActivity()
    }

    @Composable
    private fun ToolBarLayout(modifier: Modifier = Modifier) {
        ActionScope.Right.Container(modifier = modifier) {
            if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
                LoadingIcon(icon = Icons.Add, tip = "添加", onClick = {
                    when (addActivityDialog.open()) {
                        0 -> addActivity(false)
                        1 -> addActivity(true)
                    }
                })
            }
            LoadingIcon(icon = Icons.Refresh, tip = "刷新", onClick = {
                requestActivity()
            })
        }
    }

    @Composable
    private fun SectionIcon(icon: ImageVector, text: String, onClick: () -> Unit) {
        Column(
            modifier = Modifier.clip(Theme.shape.v7).clickable(onClick = onClick).padding(Theme.padding.eValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
        ) {
            Icon(icon = icon, color = Colors.Unspecified, modifier = Modifier.size(Theme.size.image9))
            SimpleClipText(text = text)
        }
    }

    @Composable
    private fun SectionLayout(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v3,
        ) {
            ActionScope.Left.Container(modifier = Modifier.fillMaxWidth()) {
                SectionIcon(icon = Icons2.Album, text = "美图", onClick = {
                    navigate(::ScreenAlbum)
                })
                SectionIcon(icon = Icons2.Weibo, text = "微博", onClick = {
                    navigate(::ScreenWeibo)
                })
                SectionIcon(icon = Icons2.Chaohua, text = "超话", onClick = {
                    navigate(::ScreenChaohua)
                })
                SectionIcon(icon = Icons2.Douyin, text = "抖音", onClick = {

                })
            }
        }
    }

    @Composable
    private fun CalendarLayout(modifier: Modifier = Modifier, actions: @Composable (RowScope.() -> Unit)) {
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.eValue,
            shape = Theme.shape.v1,
            shadowElevation = Theme.shadow.v3
        ) {
            Calendar(
                state = calendarState,
                events = activityEvents,
                actions = actions,
                onEventClick = { date ->
                    DateEx.Formatter.standardDate.format(date)
                        ?.findSelf(activities) { it.ts }
                        ?.let { navigate(::ScreenActivityDetails, it.aid) }
                }
            )
        }
    }

    @Composable
    private fun BannerLayout(modifier: Modifier = Modifier) {
        val pics by rememberDerivedState { activities.fastFilter { it.photo.coverPath != null } }

        Banner(
            size = pics.size,
            modifier = modifier,
            interval = 5000L
        ) { index ->
            val activity = pics[index]
            WebImage(
                uri = activity.photo.coverPath?.url ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onClick = { navigate(::ScreenActivityDetails, activity.aid) }
            )
        }
    }

    @Composable
    private fun CalendarBarItem(activity: Activity) {
        val date = remember(activity) { activity.ts?.let { DateEx.Formatter.standardDate.parse(it) } }
        val interval = if (date != null) DateEx.Today.until(date, DateTimeUnit.DAY) else null
        val intervalString =  when {
            interval == null -> "未知时间"
            interval == 0L -> "进行中"
            interval > 0L -> ">> ${interval}天"
            interval < 0L -> "<< ${abs(interval)}天"
            else -> "未知时间"
        }
        val intervalColor = when {
            interval == null -> LocalColor.current
            interval == 0L -> Theme.color.primary
            interval > 0L -> Theme.color.secondary
            interval < 0L -> Theme.color.tertiary
            else -> LocalColorVariant.current
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = Theme.padding.eValue9,
            shadowElevation = Theme.shadow.v3,
            onClick = { navigate(::ScreenActivityDetails, activity.aid) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleEllipsisText(
                    text = intervalString,
                    style = Theme.typography.v6.bold,
                    color = intervalColor
                )
                SimpleEllipsisText(
                    text = "${activity.title ?: "未知活动"} / ${activity.ts ?: "?"}",
                    overflow = TextOverflow.StartEllipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalImmersivePadding.current.withoutTop)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BannerLayout(modifier = Modifier.fillMaxWidth().aspectRatio(1.77778f))
            SectionLayout(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value8))
            CalendarLayout(modifier = Modifier.padding(Theme.padding.value8)) {
                ToolBarLayout()
            }
            for (activity in activities) CalendarBarItem(activity)
        }
    }

    @Composable
    private fun Square() {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = LocalImmersivePadding.current.withoutBottom + Theme.padding.value,
                shadowElevation = Theme.shadow.v3
            ) {
                ToolBarLayout(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(LocalImmersivePadding.current.withoutTop)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BannerLayout(modifier = Modifier.fillMaxWidth().aspectRatio(1.77778f))
                SectionLayout(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value8))
                CalendarLayout(modifier = Modifier.padding(Theme.padding.value8)) { }
                for (activity in activities) CalendarBarItem(activity)
            }
        }
    }

    @Composable
    private fun Landscape() {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = LocalImmersivePadding.current.withoutBottom + Theme.padding.value,
                shadowElevation = Theme.shadow.v3
            ) {
                ToolBarLayout(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(LocalImmersivePadding.current.withoutTop)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.width(Theme.size.cell1 * 1.5f)) {
                        BannerLayout(modifier = Modifier.padding(Theme.padding.value8).fillMaxWidth().aspectRatio(1.77778f))
                        SectionLayout(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value8))
                    }
                    CalendarLayout(modifier = Modifier.padding(Theme.padding.value8)) { }
                }
                for (activity in activities) CalendarBarItem(activity)
            }
        }
    }

    @Composable
    override fun Content() {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> Portrait()
            Device.Type.SQUARE -> Square()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }

    private val addActivityDialog = this land object : DialogChoice.ByList() {
        override val num: Int = 2
        override fun nameFactory(index: Int): String = if (index == 0) "公开活动" else "私密活动"
        override fun iconFactory(index: Int): ImageVector = if (index == 0) Icons.Public else Icons.Lock
    }
}
package love.yinlin.screen.msg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastFilter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.app
import love.yinlin.common.ExtraIcons
import love.yinlin.compose.*
import love.yinlin.data.compose.ItemKey
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.image.IconText
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.data.Data
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.findSelf
import love.yinlin.extension.replaceAll
import love.yinlin.screen.common.ScreenImagePreview
import love.yinlin.screen.common.ScreenVideo
import love.yinlin.screen.common.ScreenWebpage
import love.yinlin.screen.msg.activity.ScreenActivityDetails
import love.yinlin.screen.msg.activity.ScreenAddActivity
import love.yinlin.screen.msg.douyin.ScreenDouyin
import love.yinlin.screen.msg.pictures.ScreenPictures
import love.yinlin.screen.msg.weibo.*
import love.yinlin.compose.ui.container.Calendar
import love.yinlin.compose.ui.container.CalendarState
import love.yinlin.compose.ui.image.Banner
import kotlin.math.abs

@Stable
class SubScreenMsg(parent: BasicScreen) : SubScreen(parent) {
    // 当前微博
    var currentWeibo: Weibo? = null
    // 微博处理器
    val processor = object : WeiboProcessor {
        override fun onWeiboClick(weibo: Weibo) {
            currentWeibo = weibo
            navigate(::ScreenWeiboDetails)
        }

        override fun onWeiboAvatarClick(info: WeiboUserInfo) {
            navigate(::ScreenWeiboUser, info.id)
        }

        override fun onWeiboLinkClick(arg: String) = ScreenWebpage.gotoWebPage(arg) { navigate(::ScreenWebpage, it) }

        override fun onWeiboTopicClick(arg: String) = ScreenWebpage.gotoWebPage(arg) { navigate(::ScreenWebpage, it) }

        override fun onWeiboAtClick(arg: String) = ScreenWebpage.gotoWebPage(arg) { navigate(::ScreenWebpage, it) }

        override fun onWeiboPicClick(pics: List<Picture>, current: Int) {
            navigate(::ScreenImagePreview, pics, current)
        }

        override fun onWeiboVideoClick(pic: Picture) {
            navigate(::ScreenVideo, pic.video)
        }
    }

    // 活动日历
    val activities = mutableStateListOf<Activity>()
    private val calendarState = CalendarState()

    private suspend fun requestActivity() {
        // TODO:
//        val result = ClientAPI.request(
//            route = API.User.Activity.GetActivities
//        )
//        if (result is Data.Success) activities.replaceAll(result.data.sorted())
    }

    private fun showActivityDetails(aid: Int) {
        navigate(::ScreenActivityDetails, aid)
    }

    private fun onDateClick(date: LocalDate) {
        DateEx.Formatter.standardDate.format(date)
            ?.findSelf(activities) { it.ts }
            ?.let { showActivityDetails(it.aid) }
    }

    @Composable
    private fun BannerLayout(
        gap: Float,
        shape: Shape,
        modifier: Modifier = Modifier
    ) {
        // TODO:
//        val pics by rememberDerivedState { activities.fastFilter { it.pic != null } }
//        BoxWithConstraints(modifier = modifier) {
//            Banner(
//                pics = pics,
//                interval = 5000L,
//                gap = gap,
//                modifier = Modifier.fillMaxWidth().heightIn(min = maxWidth * (0.5f - gap))
//            ) { pic, _, scale ->
//                Surface(
//                    modifier = Modifier.fillMaxWidth().aspectRatio(2f).scale(scale),
//                    shape = shape,
//                    shadowElevation = CustomTheme.shadow.surface
//                ) {
//                    WebImage(
//                        uri = pic.picPath ?: "",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize(),
//                        onClick = { showActivityDetails(pic.aid) }
//                    )
//                }
//            }
//        }
    }

    @Composable
    private fun SectionLayout(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(CustomTheme.padding.value)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconText(
                    icon = ExtraIcons.Pictures,
                    text = "美图",
                    onClick = { navigate(::ScreenPictures) }
                )
                IconText(
                    icon = ExtraIcons.Weibo,
                    text = "微博",
                    onClick = { navigate(::ScreenWeibo) }
                )
                IconText(
                    icon = ExtraIcons.Chaohua,
                    text = "超话",
                    onClick = { navigate(::ScreenChaohua) }
                )
                IconText(
                    icon = ExtraIcons.Douyin,
                    text = "抖音",
                    onClick = { navigate(::ScreenDouyin) }
                )
            }
        }
    }

    @Composable
    private fun CalendarLayout(
        modifier: Modifier = Modifier,
        actions: @Composable (ActionScope.() -> Unit)
    ) {
        val events: Map<LocalDate, String> by rememberDerivedState {
            val events = mutableMapOf<LocalDate, String>()
            for (activity in activities) {
                val ts = activity.ts
                val title = activity.title
                if (ts != null && title != null) {
                    DateEx.Formatter.standardDate.parse(ts)?.let { date ->
                        events.put(date, title)
                    }
                }
            }
            events
        }

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Calendar(
                state = calendarState,
                events = events,
                actions = actions,
                modifier = Modifier.fillMaxWidth(),
                onEventClick = { onDateClick(it) }
            )
        }
    }

    @Composable
    private fun CalendarBarItem(
        modifier: Modifier = Modifier,
        activity: Activity,
    ) {
        val date = remember(activity) { activity.ts?.let { DateEx.Formatter.standardDate.parse(it) } }
        val interval = remember(date) {
            if (date != null) DateEx.Today.until(date, DateTimeUnit.DAY) else null
        }
        val intervalString = remember(interval) { when {
            interval == null -> ""
            interval == 0L -> "进行中"
            interval > 0L -> ">> ${interval}天"
            interval < 0L -> "<< ${abs(interval)}天"
            else -> ""
        } }
        val intervalColor = when {
            interval == null -> MaterialTheme.colorScheme.onSurface
            interval == 0L -> MaterialTheme.colorScheme.primary
            interval > 0L -> MaterialTheme.colorScheme.secondary
            interval < 0L -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurface
        }

        Surface(
            modifier = modifier,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraSpace),
                aspectRatio = 0.5f,
                left = {
                    Text(
                        text = intervalString,
                        style = if (LocalDevice.current.type == Device.Type.PORTRAIT) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        color = intervalColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                right = {
                    Text(
                        text = remember(activity) { "${activity.title} / ${activity.ts}" },
                        style = if (LocalDevice.current.type == Device.Type.PORTRAIT) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }

    @Composable
    private fun ActionScope.ToolBarLayout() {
        if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
            Action(Icons.Outlined.Add, "添加") {
                navigate(::ScreenAddActivity)
            }
        }
        ActionSuspend(Icons.Outlined.Refresh, "刷新") {
            requestActivity()
        }
    }

    @Composable
    private fun Portrait() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = ItemKey("Banner")) {
                BannerLayout(
                    gap = 0f,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().padding(bottom = CustomTheme.padding.verticalSpace)
                )
            }
            item(key = ItemKey("Section")) {
                SectionLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
            }
            item(key = ItemKey("Calendar")) {
                CalendarLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value)) {
                    ToolBarLayout()
                }
            }
            items(
                items = activities,
                key = { it.aid }
            ) { activity ->
                CalendarBarItem(
                    modifier = Modifier.fillMaxWidth().clickable {
                        activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.let {
                            onDateClick(it)
                        }
                    },
                    activity = activity
                )
            }
        }
    }

    @Composable
    private fun Landscape() {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                Row(
                    modifier = Modifier
                        .padding(LocalImmersivePadding.current.withoutBottom)
                        .fillMaxWidth()
                        .padding(vertical = CustomTheme.padding.verticalSpace),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ActionScope.Right.Actions {
                        ToolBarLayout()
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = ItemKey("Banner")) {
                    BannerLayout(
                        gap = 0.3f,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalExtraSpace)
                    )
                }
                item(key = ItemKey("Section")) {
                    SectionLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
                }
                items(
                    items = activities,
                    key = { it.aid }
                ) { activity ->
                    CalendarBarItem(
                        modifier = Modifier.fillMaxWidth().clickable {
                            activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.let {
                                onDateClick(it)
                            }
                        },
                        activity = activity
                    )
                }
            }
        }
    }

    override suspend fun initialize() {
        requestActivity()
    }

    @Composable
    override fun Content(device: Device) {
        when (device.type) {
            Device.Type.PORTRAIT -> Portrait()
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
        }
    }
}
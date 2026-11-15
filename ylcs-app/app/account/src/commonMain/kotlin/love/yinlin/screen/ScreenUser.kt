package love.yinlin.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.io.readByteArray
import love.yinlin.app
import love.yinlin.app.account.resources.img_not_login
import love.yinlin.app.global.resources.img_logo
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceAccount
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.graphics.qrcode.QrcodeShape
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.ui.common.PortraitValue
import love.yinlin.compose.ui.common.UserLabel
import love.yinlin.compose.ui.common.UserProfileCard
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.container.itemKey
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryOutlinedButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.layout.Grid
import love.yinlin.compose.ui.layout.VerticalDivider
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.status.LinearProgress
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.widget.QrcodeBox
import love.yinlin.compose.ui.widget.QrcodeScanner
import love.yinlin.cs.ApiProfileSignin
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.rachel.follows.FollowTabItem
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

@Stable
class ScreenUser : BasicScreen() {
    private fun onLevelClick(profile: UserProfile) {
        levelSheet.open(profile)
    }

    override fun initialize() {
        launch {
            DataSourceAccount.updateUserProfile()
        }
    }

    private val userProfileCard = movableComposable { profile: UserProfile, innerPadding: PaddingValues, modifier: Modifier ->
        UserProfileCard(
            profile = profile.publicProfile,
            modifier = modifier,
            onLevelClick = { onLevelClick(profile) }
        ) {
            ActionScope.SplitContainer(
                modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(Theme.padding.eValue9),
                left = {
                    Icon(icon = Icons.ArrowBack, onClick = ::onBack)
                },
                right = {
                    Icon(icon = Icons.CropFree, onClick = { scanSheet.open() })
                    Icon(icon = Icons.AccountBox, onClick = { userCardSheet.open(profile) })
                }
            )
        }
    }

    private val userLevelCard = movableComposable { profile: UserProfile, modifier: Modifier ->
        Box(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = Theme.padding.h9),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).silentClick { onLevelClick(profile) },
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    val level = profile.level
                    val totalExp = UserLevel.levelTable[level - 1].second

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        SimpleEllipsisText(
                            text = "Lv.${level}",
                            style = Theme.typography.v5.bold
                        )
                        SimpleEllipsisText(
                            text = "${profile.exp} / $totalExp"
                        )
                    }

                    LinearProgress(
                        value = (profile.exp.toFloat() / totalExp).coerceIn(0f, 1f),
                        trackColor = Theme.color.primary.copy(alpha = 0.5f),
                        activeColor = Theme.color.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                VerticalDivider()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    SimpleEllipsisText(text = "银币", style = Theme.typography.v6.bold)
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Paid, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = profile.coin.toString(), style = Theme.typography.v6, modifier = Modifier.idText())
                    }
                }
            }
        }
    }

    private val userTopicCard = movableComposable { profile: UserProfile, modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v7,
            onClick = { navigate(::ScreenUserCard, profile.uid) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    Icon(icon = Icons.Article)
                    SimpleEllipsisText(text = "主题", style = Theme.typography.v7.bold)
                }

                VerticalDivider()

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    TextIconAdapter(modifier = Modifier.silentClick {
                        navigate(::ScreenFollows, FollowTabItem.Follows)
                    }) { idIcon, idText ->
                        Icon(icon = Icons.FavoriteBorder, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = "关注 ${profile.follows}", modifier = Modifier.idText())
                    }

                    TextIconAdapter(modifier = Modifier.silentClick {
                        navigate(::ScreenFollows, FollowTabItem.Followers)
                    }) { idIcon, idText ->
                        Icon(icon = Icons.AccountCircle, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = "粉丝 ${profile.followers}", modifier = Modifier.idText())
                    }
                }
            }
        }
    }

    private val mailCard = movableComposable { profile: UserProfile, modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v7,
            onClick = { navigate(::ScreenMail) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    Icon(icon = Icons.Mail)
                    SimpleEllipsisText(text = "邮件", style = Theme.typography.v7.bold)
                }

                VerticalDivider()

                val mailCount = profile.notification.mailCount

                SimpleEllipsisText(
                    text = if (mailCount > 0) "未读 +$mailCount" else "无新邮件",
                    color = if (mailCount > 0) Theme.color.error else LocalColorVariant.current,
                    style = Theme.typography.v7.bold
                )
            }
        }
    }

    private suspend fun signin(profile: UserProfile) {
        ApiProfileSignin.request(app.config.userToken) { status, newData ->
            if (status) {
                app.config.userProfile = profile.copy(
                    coin = profile.coin + 1,
                    exp = profile.exp + 1,
                    notification = profile.notification.copy(signinData = newData)
                )
                slot.tip.success("签到成功! 经验+1, 银币+1")
            }
            else slot.tip.warning("今日已签到过")
        }
    }

    private val signinCard = movableComposable { profile: UserProfile, modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v7
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                val signinData = profile.notification.signinData
                val currentDate = signinData.currentDate

                signinData.check { isSignin, byteValue, _, dayIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon = Icons.EventAvailable)
                        SimpleEllipsisText(text = "${currentDate.month.number}月签到", style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))

                        if (!isSignin) {
                            SecondaryTextButton(
                                text = "立即打卡",
                                icon = Icons.Upload,
                                onClick = {
                                    launch { signin(profile) }
                                }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(8) { index ->
                            val isToday = index == dayIndex
                            val daySignin = ((byteValue shr index) and 1) == 1
                            val date = signinData.currentDate.minus(dayIndex - index, DateTimeUnit.DAY)

                            val icon = when {
                                index > dayIndex -> Icons.QuestionMark
                                daySignin -> Icons.Check
                                isToday -> Icons.IndeterminateCheckBox
                                else -> Icons.Clear
                            }

                            val color = if (index > dayIndex) LocalColor.current else {
                                if (daySignin) {
                                    if (isToday) Theme.color.primary else LocalColor.current
                                }
                                else {
                                    if (isToday) Theme.color.warning else Theme.color.error
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                            ) {
                                SimpleEllipsisText(text = date.day.toString())
                                Icon(icon = icon, color = color)
                            }
                        }
                    }

                    val [signinDays, totalDays] = remember(profile) { signinData.signinDayOfYear }

                    SimpleEllipsisText(text = "今年累计签到 $signinDays / $totalDays 天", color = Theme.color.tertiary)
                }
            }
        }
    }

    private val adminCard = movableComposable { _: UserProfile, modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v7,
            onClick = { navigate(::ScreenAdmin) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon = Icons.Lock)
                SimpleEllipsisText(text = "超管空间", style = Theme.typography.v7.bold)
            }
        }
    }

    private val testCard = movableComposable { _: UserProfile, modifier: Modifier ->
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value9,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v7,
            onClick = { navigate(::ScreenTest) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon = Icons.BugReport)
                SimpleEllipsisText(text = "测试", style = Theme.typography.v7.bold)
            }
        }
    }

    private val cardGroup = movableComposable { profile: UserProfile, modifier: Modifier ->
        val padding = Theme.padding.e

        Grid(
            builder = {
                x(0.5f, 0.5f)
                y(auto, auto, auto)
                s(padding)
            },
            modifier = modifier,
        ) {
            userTopicCard(profile, Modifier.fillMaxWidth())
            mailCard(profile, Modifier.fillMaxWidth())
            signinCard(profile, Modifier.fillMaxWidth().item(xs = 2))
            if (profile.hasPrivilegeVIPAccount) {
                adminCard(profile, Modifier.fillMaxWidth())
                testCard(profile, Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun Portrait(profile: UserProfile) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            userProfileCard(profile, LocalImmersivePadding.current.withoutBottom, Modifier.fillMaxWidth())
            userLevelCard(profile, Modifier.fillMaxWidth())
            cardGroup(profile, Modifier.fillMaxWidth().padding(Theme.padding.eValue))
        }
    }

    @Composable
    private fun Landscape(profile: UserProfile) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Column(modifier = Modifier.width(Theme.size.cell1 * 1.5f).fillMaxHeight()) {
                userProfileCard(profile, PaddingValues.Zero, Modifier.fillMaxWidth())
                userLevelCard(profile, Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                cardGroup(profile, Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            }
        }
    }

    @Composable
    override fun BasicContent() {
        val profile = app.config.userProfile

        if (profile != null) {
            val deviceType by rememberDeviceType()
            when (deviceType) {
                Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(profile = profile)
                Device.Type.LANDSCAPE -> Landscape(profile = profile)
            }
        }
        else {
            Column(
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v5, Alignment.CenterVertically),
            ) {
                Image(
                    res = love.yinlin.app.account.resources.Res.drawable.img_not_login,
                    modifier = Modifier.size(Theme.size.image2)
                )
                PrimaryButton(text = "登录", icon = Icons.AccountCircle, onClick = {
                    navigate(::ScreenLogin)
                })
                SecondaryOutlinedButton(text = "返回", icon = Icons.ArrowBack, onClick = ::onBack)
            }
        }
    }

    private val scanSheet = this land object : Sheet() {
        override val minPortraitRatio: Float = 0.9f
        override val maxPortraitRatio: Float = 0.9f
        override val scrollable: Boolean = false

        @Composable
        override fun Content() {
            QrcodeScanner(
                modifier = Modifier.fillMaxSize(),
                onData = { app.picker.pickPicture()?.use { it.readByteArray() } },
                onResult = { result ->
                    close()
                    val uri = Uri.parse(result)
                    if (uri?.scheme == Scheme.Rachel) DeepLink.navigate(uri)
                    else slot.tip.warning("不能识别此信息")
                }
            )
        }
    }

    private val userCardSheet = this land object : SheetContent<UserProfile>() {
        @Composable
        override fun Content(args: UserProfile) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue3),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v4)
            ) {
                WebImage(
                    source = args.avatarPath.url,
                    version = app.config.cacheUserAvatar.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image4).shadow(Theme.shape.circle, Theme.shadow.v4),
                    options = ImageOptions.Circular
                )
                SimpleEllipsisText(
                    text = args.name,
                    style = Theme.typography.v4.bold,
                    color = Theme.color.primary
                )

                QrcodeBox(
                    text = "rachel://yinlin.love/openProfile?uid=${args.uid}",
                    logo = love.yinlin.app.global.resources.Res.drawable.img_logo,
                    shape = QrcodeShape.Rounded,
                    modifier = Modifier.size(Theme.size.image2)
                )
            }
        }
    }

    private val levelSheet = this land object : SheetContent<UserProfile>() {
        override val scrollable: Boolean = false

        @Composable
        private fun LevelItem(level: Int, item: Pair<Int, Int>, modifier: Modifier = Modifier) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(level.toString())
                TextIconAdapter(modifier = Modifier.weight(1f)) { idIcon, idText ->
                    val text = if (item.second != Int.MAX_VALUE) "${item.first} ~ ${item.second}" else "> ${item.first}"
                    Icon(icon = Icons.Explicit, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = text, modifier = Modifier.idText())
                }
                UserLabel(label = "", level = level)
            }
        }

        @Composable
        override fun Content(args: UserProfile) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemKey("Profile") {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WebImage(
                            source = args.avatarPath.url,
                            version = app.config.cacheUserAvatar.toString(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            options = ImageOptions.Circular
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            SimpleEllipsisText(text = args.name, style = Theme.typography.v6.bold, modifier = Modifier.fillMaxWidth())
                            UserLabel(label = args.label, level = args.level)
                        }
                        PortraitValue(value = args.level.toString(), title = "等级")
                        PortraitValue(value = args.exp.toString(), title = "经验")
                    }
                }
                itemsIndexed(
                    items = UserLevel.levelTable,
                    key = { _, v -> v }
                ) { index, item ->
                    val level = index + 1
                    val isCurrentLevel = level == app.config.userProfile?.level
                    val color = if (isCurrentLevel) Theme.color.primary else LocalColor.current

                    ThemeContainer(color) {
                        LevelItem(
                            level = level,
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .condition(isCurrentLevel) {
                                    border(width = Theme.border.v5, color = Theme.color.primary)
                                }.clickable { }.padding(horizontal = Theme.padding.h9, vertical = Theme.padding.v)
                        )
                    }
                }
            }
        }
    }
}

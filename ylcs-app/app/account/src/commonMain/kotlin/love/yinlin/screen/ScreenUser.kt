package love.yinlin.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceAccount
import love.yinlin.compose.ds.DataSourceActivity
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.BoardButton
import love.yinlin.compose.ui.common.BoardButtonContainer
import love.yinlin.compose.ui.common.PortraitValue
import love.yinlin.compose.ui.common.UserLabel
import love.yinlin.compose.ui.common.UserProfileCard
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.container.itemKey
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.text.TextIconBinder
import love.yinlin.compose.ui.widget.QrcodeBox
import love.yinlin.compose.ui.widget.QrcodeScanner
import love.yinlin.compose.window.DeepLink
import love.yinlin.cs.ApiProfileSignin
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.Data
import love.yinlin.data.rachel.follows.FollowTabItem
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.DateEx
import love.yinlin.platform.Platform
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.UriGenerator

@Stable
class ScreenUser : BasicScreen() {
    private fun onLevelClick(profile: UserProfile) {
        levelSheet.open(profile)
    }

    override suspend fun initialize() {
        DataSourceAccount.updateUserProfile()
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
                    Icon(icon = Icons.Settings, onClick = { navigate(::ScreenSettings) })
                },
                right = {
                    Icon(icon = Icons.CropFree, onClick = { scanSheet.open() })
                    Icon(icon = Icons.AccountBox, onClick = {
                        app.config.userProfile?.let(userCardSheet::open) ?: slot.tip.warning("请先登录")
                    })
                }
            )
        }
    }

    private val userPropertyCard = movableComposable { profile: UserProfile, modifier: Modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortraitValue(value = profile.level.toString(), title = "等级", onClick = { onLevelClick(profile) })
            PortraitValue(value = profile.exp.toString(), title = "经验", onClick = { onLevelClick(profile) })
            PortraitValue(value = profile.coin.toString(), title = "银币")
            PortraitValue(value = profile.follows.toString(), title = "关注", onClick = {
                navigate(::ScreenFollows, FollowTabItem.Follows)
            })
            PortraitValue(value = profile.followers.toString(), title = "粉丝", onClick = {
                navigate(::ScreenFollows, FollowTabItem.Followers)
            })
        }
    }

    private val buttonContainer = movableComposable { ->
        BoardButtonContainer("个人空间") {
            val notification = app.config.userProfile?.notification

            BoardButton("签到", Icons.EventAvailable, if (notification?.isSignin == false) 1 else 0) {
                app.config.userProfile?.let(signinSheet::open) ?: slot.tip.warning("请先登录")
            }
            BoardButton("主题", Icons.Article) {
                app.config.userProfile?.let {
                    navigate(::ScreenUserCard, it.uid)
                } ?: slot.tip.warning("请先登录")
            }
            BoardButton("邮箱", Icons.Mail) {
                app.config.userProfile?.let {
                    navigate(::ScreenMail)
                } ?: slot.tip.warning("请先登录")
            }
            BoardButton("徽章", Icons.MilitaryTech) {

            }
        }
        BoardButtonContainer("推广") {
            BoardButton("抽奖", Icons.Gift) {
                app.config.userProfile?.let {
                    navigate(::ScreenPrice)
                } ?: slot.tip.warning("请先登录")
            }
            BoardButton("招新", Icons.Campaign) {

            }
            BoardButton("水群", Icons2.QQ) {
                launch {
                    val uri = Platform.use(*Platform.Phone, ifTrue = UriGenerator.qqGroup("828049503"), ifFalse = UriGenerator.qqGroupLink("eAli22ljj4"))
                    if (!app.openUri(uri)) slot.tip.warning("未安装QQ")
                }
            }
            BoardButton("店铺", Icons2.Taobao) {
                if (!app.openUri(UriGenerator.taobao("280201975"))) slot.tip.warning("未安装淘宝")
            }
        }
        BoardButtonContainer("超管空间") {
            if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
                BoardButton("添加活动", Icons.Gift) {
                    launch {
                        val index = addActivityDialog.open() ?: return@launch
                        when (val result = DataSourceActivity.addActivity(index == 1)) {
                            is Data.Success -> navigate(::ScreenModifyActivity, result.data)
                            is Data.Failure -> result.throwable.errorTip
                        }
                    }
                }
                BoardButton("秀动提取", Icons.Link) {
                    navigate(::ScreenActivityLink)
                }
                BoardButton("测试", Icons.BugReport) {
                    navigate(::ScreenTest)
                }
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
            userPropertyCard(profile, Modifier.fillMaxWidth())
            buttonContainer()
        }
    }

    @Composable
    private fun Landscape(profile: UserProfile) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Column(modifier = Modifier.width(Theme.size.cell1 * 1.25f).fillMaxHeight()) {
                userProfileCard(profile, PaddingValues.Zero, Modifier.fillMaxWidth())
                userPropertyCard(profile, Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                buttonContainer()
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
                SecondaryButton(text = "设置", icon = Icons.Settings, onClick = {
                    navigate(::ScreenSettings)
                })
            }
        }
    }

    private val addActivityDialog = this land object : DialogChoice.ByList() {
        override val num: Int = 2
        override fun nameFactory(index: Int): String = if (index == 0) "公开活动" else "私密活动"
        override fun iconFactory(index: Int): ImageVector = if (index == 0) Icons.Public else Icons.Lock
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
                    if (uri?.scheme == Scheme.Rachel) DeepLink.openUri(uri)
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
                    uri = args.avatarPath.url,
                    key = app.config.cacheUserAvatar,
                    contentScale = ContentScale.Crop,
                    circle = true,
                    modifier = Modifier.size(Theme.size.image4).shadow(Theme.shape.circle, Theme.shadow.v4)
                )
                SimpleEllipsisText(
                    text = args.name,
                    style = Theme.typography.v4.bold,
                    color = Theme.color.primary
                )

                QrcodeBox(
                    text = "rachel://yinlin.love/openProfile?uid=${args.uid}",
                    logo = love.yinlin.app.global.resources.Res.drawable.img_logo,
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
                            uri = args.avatarPath.url,
                            key = app.config.cacheUserAvatar,
                            contentScale = ContentScale.Crop,
                            circle = true,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
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

    private val signinSheet = this land object : SheetContent<UserProfile>() {
        var signinData by mutableRefStateOf(BooleanArray(8) { false })
        var todayIndex by mutableIntStateOf(-1)
        var todaySignin by mutableStateOf(true)

        override suspend fun initialize(args: UserProfile) {
            signinData = BooleanArray(8) { false }
            todayIndex = -1
            todaySignin = true
            ApiProfileSignin.request(app.config.userToken) { status, value, index ->
                todaySignin = status
                todayIndex = index
                signinData = BooleanArray(8) { ((value shr it) and 1) == 1 }
                if (!todaySignin) app.config.userProfile = args.copy(
                    coin = args.coin + 1,
                    exp = args.exp + 1,
                    notification = args.notification.copy(isSignin = true)
                )
            }
        }

        @Composable
        override fun Content(args: UserProfile) {
            val today = remember { DateEx.Today }

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                SimpleEllipsisText(text = "签到记录", style = Theme.typography.v6.bold)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                ) {
                    repeat(8) { index ->
                        val isToday = index == todayIndex
                        val checked = signinData[index]
                        val icon = when {
                            index > todayIndex -> Icons.QuestionMark
                            checked -> Icons.Check
                            isToday -> Icons.IndeterminateCheckBox
                            else -> Icons.Clear
                        }
                        val color = if (index > todayIndex) LocalColor.current else {
                            if (checked) {
                                if (isToday) Theme.color.primary else LocalColor.current
                            }
                            else {
                                if (isToday) Theme.color.warning else Theme.color.error
                            }
                        }
                        val border = if (isToday) BorderStroke(Theme.border.v7, color) else null

                        Surface(
                            shape = Theme.shape.v7,
                            contentPadding = Theme.padding.eValue,
                            tonalLevel = 5,
                            shadowElevation = Theme.shadow.v7,
                            border = border
                        ) {
                            TextIconBinder(modifier = Modifier) { idIcon, idText ->
                                val date = today.minus(todayIndex - index, DateTimeUnit.DAY)
                                Icon(
                                    icon = icon,
                                    color = color,
                                    modifier = Modifier.idIcon()
                                )
                                SimpleEllipsisText(
                                    text = "${date.month.number}月${date.day}日",
                                    color = color,
                                    modifier = Modifier.idText()
                                )
                            }
                        }
                    }
                }

                SimpleEllipsisText(text = if (todaySignin) "今日已签到" else "签到成功! 经验+1, 银币+1")
            }
        }
    }
}
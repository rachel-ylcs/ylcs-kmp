package love.yinlin.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.atomicfu.atomic
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.number
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.*
import love.yinlin.common.uri.Scheme
import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.UriGenerator
import love.yinlin.compose.*
import love.yinlin.data.Data
import love.yinlin.data.ItemKey
import love.yinlin.data.RequestError
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.*
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.MiniImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.ui.component.layout.Space
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.node.clickableNoRipple
import love.yinlin.compose.ui.node.condition
import love.yinlin.ui.component.platform.QrcodeScanner
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.ui.screen.common.ScreenTest
import love.yinlin.ui.screen.settings.ScreenSettings
import love.yinlin.ui.screen.msg.activity.ScreenActivityLink
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun LevelItem(
    index: Int,
    item: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    val level = index + 1
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(level.toString())
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            NormalText(
                text = remember(item) {
                    if (item.second != Int.MAX_VALUE) "${item.first} ~ ${item.second}"
                    else "> ${item.first}"
                },
                icon = Icons.Outlined.Explicit
            )
        }
        Box(
            modifier = Modifier.offset(y = -CustomTheme.padding.littleSpace),
            contentAlignment = Alignment.Center
        ) {
            UserLabel(
                label = "",
                level = level
            )
        }
    }
}

@Stable
class ScreenPartMe(model: AppModel) : ScreenPart(model) {
    private val isUpdateToken = atomic(false)

    fun cleanUserToken() {
        app.config.userShortToken = 0L
        app.config.userToken = ""
        app.config.userProfile = null
        app.config.cacheUserAvatar = KVConfig.UPDATE
        app.config.cacheUserWall = KVConfig.UPDATE
    }

    suspend fun updateUserToken() {
        val token = app.config.userToken
        val currentTime = DateEx.CurrentLong
        val duration = currentTime - app.config.userShortToken
        if (token.isNotEmpty() && duration > 7 * 24 * 3600 * 1000L &&
            isUpdateToken.compareAndSet(expect = false, update = true)) {
            val result = ClientAPI.request(
                route = API.User.Account.UpdateToken,
                data = token
            )
            isUpdateToken.value = false
            when (result) {
                is Data.Success -> {
                    app.config.userShortToken = currentTime
                    app.config.userToken = result.data
                }
                is Data.Failure if result.type == RequestError.Unauthorized -> {
                    cleanUserToken()
                    navigate<ScreenLogin>()
                }
                else -> {}
            }
        }
    }

    suspend fun updateUserProfile() {
        val token = app.config.userToken
        if (token.isNotEmpty() && !isUpdateToken.value) {
            val result = ClientAPI.request(
                route = API.User.Profile.GetProfile,
                data = token
            )
            if (result is Data.Success) app.config.userProfile = result.data
        }
    }

    @Composable
    private fun ToolContainer(
        modifier: Modifier = Modifier,
        shape: Shape = RectangleShape
    ) {
        TipButtonContainer(
            modifier = modifier,
            shape = shape,
            title = "功能栏"
        ) {
            Item("扫码", Icons.Filled.CropFree) { scanSheet.open() }
            Item("名片", Icons.Filled.AccountBox) {
                app.config.userProfile?.let {
                    userCardSheet.open(it)
                } ?: slot.tip.warning("请先登录")
            }
            Item("设置", Icons.Filled.Settings) { navigate<ScreenSettings>() }
        }
    }

    @Composable
    private fun UserSpaceContainer(
        modifier: Modifier = Modifier,
        shape: Shape = RectangleShape
    ) {
        TipButtonContainer(
            modifier = modifier,
            shape = shape,
            title = "个人空间"
        ) {
            val notification = app.config.userProfile?.notification

            Item(
                text = "签到",
                icon = Icons.Filled.EventAvailable,
                label = if (notification?.isSignin == false) 1 else 0
            ) {
                app.config.userProfile?.let {
                    signinSheet.open(it)
                } ?: slot.tip.warning("请先登录")
            }
            Item("主题", Icons.AutoMirrored.Filled.Article) {
                app.config.userProfile?.let {
                    navigate(ScreenUserCard.Args(it.uid))
                } ?: slot.tip.warning("请先登录")
            }
            Item(
                text = "邮箱",
                icon = Icons.Filled.Mail,
                label = notification?.mailCount ?: 0
            ) {
                app.config.userProfile?.let {
                    navigate<ScreenMail>()
                } ?: slot.tip.warning("请先登录")
            }
            Item("徽章", Icons.Filled.MilitaryTech) { }
        }
    }

    @Composable
    private fun PromotionContainer(
        modifier: Modifier = Modifier,
        shape: Shape = RectangleShape
    ) {
        TipButtonContainer(
            modifier = modifier,
            shape = shape,
            title = "推广"
        ) {
            Item("水群", ExtraIcons.QQ) {
                launch {
                    Platform.use(*Platform.Phone,
                        ifTrue = { if (!OS.Application.startAppIntent(UriGenerator.qqGroup("828049503"))) slot.tip.warning("未安装QQ") },
                        ifFalse = { OS.Application.startAppIntent(UriGenerator.qqGroup("0tJOqsYAaonMEq6dFqmg8Zb0cfXYzk8E", "%2BchwTB02SMM8pDjJVgLN4hZysG0%2BXRWT4GAIGs6RqGazJ2NCqdkYETWvtTPrd69R")) }
                    )
                }
            }
            Item("店铺", ExtraIcons.Taobao) {
                launch {
                    if (!OS.Application.startAppIntent(UriGenerator.taobao("280201975"))) slot.tip.warning("未安装淘宝")
                }
            }
        }
    }

    @Composable
    private fun AdminContainer(
        modifier: Modifier = Modifier,
        shape: Shape = RectangleShape
    ) {
        TipButtonContainer(
            modifier = modifier,
            shape = shape,
            title = "超管空间"
        ) {
            Item("活动", Icons.Filled.Link) {
                navigate<ScreenActivityLink>()
            }
            Item("测试", Icons.Filled.BugReport) {
                navigate<ScreenTest>()
            }
        }
    }

    @Composable
    private fun LoginBox(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalSpace),
                horizontalArrangement = Arrangement.End
            ) {
                ActionScope.Right.Actions {
                    Action(Icons.Filled.Settings) {
                        navigate<ScreenSettings>()
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
                ) {
                    MiniIcon(
                        res = Res.drawable.img_not_login,
                        size = CustomTheme.size.extraImage
                    )
                    ClickText(
                        text = stringResource(Res.string.login),
                        onClick = { navigate<ScreenLogin>() }
                    )
                }
            }
        }
    }

    @Composable
    private fun Portrait(userProfile: UserProfile) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            UserProfileCard(
                modifier = Modifier.fillMaxWidth(),
                profile = userProfile,
                onLevelClick = { levelSheet.open(userProfile) },
                onFollowClick = { navigate(ScreenFollows.Args(it)) }
            )
            ToolContainer(modifier = Modifier.fillMaxWidth())
            UserSpaceContainer(modifier = Modifier.fillMaxWidth())
            PromotionContainer(modifier = Modifier.fillMaxWidth())
            if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
                AdminContainer(modifier = Modifier.fillMaxWidth())
            }
            Space()
        }
    }

    @Composable
    private fun Landscape(userProfile: UserProfile) {
        Row(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            UserProfileCard(
                profile = userProfile,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f).padding(CustomTheme.padding.equalExtraValue),
                onLevelClick = { levelSheet.open(userProfile) },
                onFollowClick = { navigate(ScreenFollows.Args(it)) }
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                ToolContainer(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraValue),
                    shape = MaterialTheme.shapes.large
                )
                UserSpaceContainer(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraValue),
                    shape = MaterialTheme.shapes.large
                )
                PromotionContainer(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraValue),
                    shape = MaterialTheme.shapes.large
                )
                if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
                    AdminContainer(
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraValue),
                        shape = MaterialTheme.shapes.large
                    )
                }
                Space()
            }
        }
    }

    override suspend fun update() {
        updateUserProfile()
    }

    @Composable
    override fun Content() {
        app.config.userProfile?.let { userProfile ->
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait(userProfile = userProfile)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(userProfile = userProfile)
            }
        } ?: LoginBox(Modifier.fillMaxSize().padding(LocalImmersivePadding.current))
    }

    private val scanSheet = object : FloatingSheet() {
        override val maxHeightRatio: Float = 0.9f
        override val initFullScreen: Boolean = true

        @Composable
        override fun Content() {
            QrcodeScanner(
                modifier = Modifier.fillMaxWidth(),
                onResult = { result ->
                    try {
                        val uri = Uri.parse(result)!!
                        if (uri.scheme == Scheme.Rachel) deeplink(uri)
                    }
                    catch (_: Throwable) {
                        slot.tip.warning("不能识别此信息")
                    }
                    close()
                }
            )
        }
    }

    private val userCardSheet = object : FloatingArgsSheet<UserProfile>() {
        @Composable
        override fun Content(args: UserProfile) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                WebImage(
                    uri = args.avatarPath,
                    key = app.config.cacheUserAvatar,
                    contentScale = ContentScale.Crop,
                    circle = true,
                    modifier = Modifier.size(CustomTheme.size.largeImage)
                        .shadow(CustomTheme.shadow.icon, CircleShape)
                )
                Text(
                    text = args.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val logoPainter = painterResource(Res.drawable.img_logo)
                val qrcodePainter = rememberQrCodePainter(data = "rachel://yinlin.love/openProfile?uid=${args.uid}") {
                    logo {
                        painter = logoPainter
                        padding = QrLogoPadding.Natural(1f)
                        shape = QrLogoShape.circle()
                        size = 0.2f
                    }
                    shapes {
                        ball = QrBallShape.circle()
                        darkPixel = QrPixelShape.roundCorners()
                        frame = QrFrameShape.roundCorners(0.25f)
                    }
                    colors {
                        dark = QrBrush.brush {
                            Brush.linearGradient(
                                0f to primaryColor.copy(alpha = 0.9f),
                                1f to secondaryColor.copy(0.9f),
                                end = Offset(it, it)
                            )
                        }
                        frame = QrBrush.solid(primaryColor)
                    }
                }
                MiniImage(
                    painter = qrcodePainter,
                    modifier = Modifier.size(CustomTheme.size.extraImage)
                )
                Space()
            }
        }
    }

    private val signinSheet = object : FloatingArgsSheet<UserProfile>() {
        var signinData by mutableRefStateOf(BooleanArray(8) { false })
        var todayIndex by mutableIntStateOf(-1)
        var todaySignin by mutableStateOf(true)

        override suspend fun initialize(args: UserProfile) {
            signinData = BooleanArray(8) { false }
            todayIndex = -1
            todaySignin = true
            val result = ClientAPI.request(
                route = API.User.Profile.Signin,
                data = app.config.userToken
            )
            if (result is Data.Success) {
                with(result.data) {
                    todaySignin = status
                    todayIndex = index
                    signinData = BooleanArray(8) { ((value shr it) and 1) == 1 }
                }
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
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                Text(
                    text = "签到记录",
                    style = MaterialTheme.typography.titleLarge
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    repeat(2) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
                        ) {
                            repeat(4) { col ->
                                val index = row * 4 + col
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = CustomTheme.shadow.tonal,
                                    shadowElevation = CustomTheme.shadow.item,
                                    border = if (index != todayIndex) null else BorderStroke(CustomTheme.border.small, MaterialTheme.colorScheme.primary)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace, Alignment.CenterVertically)
                                    ) {
                                        val date = today.minus(todayIndex - index, DateTimeUnit.DAY)

                                        MiniIcon(
                                            icon = if (signinData[index]) Icons.Outlined.Check else Icons.Outlined.IndeterminateCheckBox,
                                            color = if (index != todayIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${date.month.number}月${date.day}日",
                                            color = if (index != todayIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(text = if (todaySignin) "今日已签到" else "签到成功! 经验+1, 银币+1")
            }
        }
    }

    private val levelSheet = object : FloatingArgsSheet<UserProfile>() {
        @Composable
        override fun Content(args: UserProfile) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = ItemKey("Profile")) {
                    UserProfileInfo(
                        profile = remember(args) { args.publicProfile },
                        owner = true,
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue)
                    ) { onLevelClick ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PortraitValue(
                                value = args.level.toString(),
                                title = "等级",
                                modifier = Modifier.clickableNoRipple(onClick = onLevelClick)
                            )
                            PortraitValue(
                                value = args.exp.toString(),
                                title = "经验"
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = UserLevel.levelTable,
                    key = { index, _ -> index }
                ) { index, item ->
                    LevelItem(
                        index = index,
                        item = item,
                        modifier = Modifier.fillMaxWidth()
                            .condition(index + 1 == app.config.userProfile?.level) {
                                border(
                                    width = CustomTheme.border.small,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }.clickable {}.padding(CustomTheme.padding.value)
                    )
                }
            }
        }
    }

    @Composable
    override fun Floating() {
        scanSheet.Land()
        userCardSheet.Land()
        signinSheet.Land()
        levelSheet.Land()
    }
}
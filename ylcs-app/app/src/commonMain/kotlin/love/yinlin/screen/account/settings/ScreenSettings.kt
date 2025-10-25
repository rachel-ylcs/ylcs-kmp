package love.yinlin.screen.account.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.common.*
import love.yinlin.common.uri.Uri
import love.yinlin.compose.*
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.MiniImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.image.colorfulImageVector
import love.yinlin.compose.ui.text.InputType
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.rememberTextInputState
import love.yinlin.data.Data
import love.yinlin.data.ItemKey
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.extension.fileSizeString
import love.yinlin.platform.*
import love.yinlin.resources.*
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.ui.component.input.SingleSelector
import love.yinlin.compose.ui.input.TertiaryButton
import love.yinlin.ui.component.layout.Space
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.screen.account.ScreenLogin
import love.yinlin.screen.account.SubScreenMe
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.community.ScreenUserCard
import love.yinlin.service
import love.yinlin.ui.component.screen.dialog.FloatingDialogCrop
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Stable
class ScreenSettings(manager: ScreenManager) : CommonScreen(manager) {
    private val subScreenMe = manager.get<ScreenMain>().get<SubScreenMe>()

    private suspend fun pickPicture(aspectRatio: Float): Path? {
        return Picker.pickPicture()?.use { source ->
            service.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }?.let { path ->
            cropDialog.openSuspend(url = path.toString(), aspectRatio = aspectRatio)?.let { rect ->
                service.os.storage.createTempFile { sink ->
                    SystemFileSystem.source(path).buffered().use { source ->
                        ImageProcessor(ImageCrop(rect), ImageCompress, quality = ImageQuality.High).process(source, sink)
                    }
                }
            }
        }
    }

    private suspend fun modifyUserAvatar() {
        pickPicture(1f)?.let { path ->
            val result = ClientAPI.request(
                route = API.User.Profile.UpdateAvatar,
                data = app.config.userToken,
                files = {
                    API.User.Profile.UpdateAvatar.Files(
                        avatar = file(SystemFileSystem.source(path))
                    )
                }
            )
            when (result) {
                is Data.Success -> app.config.cacheUserAvatar = KVConfig.UPDATE
                is Data.Failure -> slot.tip.error(result.message)
            }
        }
    }

    private suspend fun modifyUserWall() {
        pickPicture(1.77777f)?.let { path ->
            val result = ClientAPI.request(
                route = API.User.Profile.UpdateWall,
                data = app.config.userToken,
                files = {
                    API.User.Profile.UpdateWall.Files(
                        wall = file(SystemFileSystem.source(path))
                    )
                }
            )
            when (result) {
                is Data.Success -> app.config.cacheUserWall = KVConfig.UPDATE
                is Data.Failure -> slot.tip.error(result.message)
            }
        }
    }

    private suspend fun modifyUserId(initText: String) {
        idModifyDialog.openSuspend(initText)?.let { text ->
            val profile = app.config.userProfile
            if (profile != null && profile.coin >= UserConstraint.RENAME_COIN_COST) launch {
                val result = ClientAPI.request(
                    route = API.User.Profile.UpdateName,
                    data = API.User.Profile.UpdateName.Request(
                        token = app.config.userToken,
                        name = text
                    )
                )
                when (result) {
                    is Data.Success -> {
                        app.config.userProfile = profile.copy(
                            name = text,
                            coin = profile.coin - UserConstraint.RENAME_COIN_COST
                        )
                    }
                    is Data.Failure -> slot.tip.error(result.message)
                }
            }
        }
    }

    private suspend fun modifyUserSignature(initText: String) {
        signatureModifyDialog.openSuspend(initText)?.let { text ->
            val profile = app.config.userProfile
            if (profile != null) launch {
                val result = ClientAPI.request(
                    route = API.User.Profile.UpdateSignature,
                    data = API.User.Profile.UpdateSignature.Request(
                        token = app.config.userToken,
                        signature = text
                    )
                )
                when (result) {
                    is Data.Success -> {
                        app.config.userProfile = profile.copy(signature = text)
                    }
                    is Data.Failure -> slot.tip.error(result.message)
                }
            }
        }
    }

    private suspend fun modifyPassword(oldPwd: String, newPwd: String) {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            val result = ClientAPI.request(
                route = API.User.Account.ChangePassword,
                data = API.User.Account.ChangePassword.Request(
                    token = token,
                    oldPwd = oldPwd,
                    newPwd = newPwd
                )
            )
            when (result) {
                is Data.Success -> {
                    slot.tip.success("修改密码成功, 请重新登录")
                    subScreenMe.cleanUserToken()
                }
                is Data.Failure -> slot.tip.error(result.message)
            }
        }
    }

    private suspend fun resetPicture() {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            if (slot.confirm.openSuspend(content = "重置默认头像与背景墙")) {
                val result = ClientAPI.request(
                    route = API.User.Profile.ResetPicture,
                    data = token
                )
                when (result) {
                    is Data.Success -> {
                        app.config.cacheUserAvatar = KVConfig.UPDATE
                        app.config.cacheUserWall = KVConfig.UPDATE
                    }
                    is Data.Failure-> slot.tip.error(result.message)
                }
            }
        }
    }

    private suspend fun logoff() {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            if (slot.confirm.openSuspend(content = "退出登录")) {
                ClientAPI.request(
                    route = API.User.Account.Logoff,
                    data = token
                )
                // 不论是否成功均从本地设备退出登录
                subScreenMe.cleanUserToken()
            }
        }
    }

    private suspend fun clearCache(): String = Coroutines.io {
        service.os.storage.clearCache()
        service.os.storage.cacheSize.fileSizeString
    }

    private suspend fun sendFeedback(content: String) {
        val result = ClientAPI.request(
            route = API.User.Info.SendFeedback,
            data = API.User.Info.SendFeedback.Request(
                token = app.config.userToken,
                content = content
            )
        )
        when (result) {
            is Data.Success -> feedbackSheet.close()
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    private suspend fun checkUpdate() {
        when (val result = ClientAPI.request<ServerStatus>(route = ServerRes.Server)) {
            is Data.Success -> {
                val data = result.data
                if (data.targetVersion > Local.info.version) slot.tip.warning("新版本${data.targetVersion}可用")
                else if (data.minVersion > Local.info.version) slot.tip.error("当前版本不满足服务器最低兼容版本${data.minVersion}")
                else slot.tip.success("当前已是最新版本")
            }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    @Composable
    private fun AccountSettings(
        userProfile: UserProfile?,
        modifier: Modifier = Modifier
    ) {
        SettingsLayout(
            modifier = modifier,
            title = "账号",
            icon = Icons.Outlined.AccountCircle
        ) {
            if (userProfile == null) {
                ItemExpander(
                    title = "登录",
                    icon = colorfulImageVector(
                        icon = Icons.AutoMirrored.Outlined.Login,
                        background = CustomTheme.colorScheme.warning
                    ),
                    color = CustomTheme.colorScheme.warning,
                    hasDivider = false,
                    onClick = {
                        pop()
                        navigate<ScreenLogin>()
                    }
                )
            }
            else {
                Item(
                    title = "头像",
                    onClick = { launch { modifyUserAvatar() } }
                ) {
                    WebImage(
                        uri = userProfile.avatarPath,
                        key = app.config.cacheUserAvatar,
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.size(CustomTheme.size.image)
                            .shadow(CustomTheme.shadow.icon, CircleShape)
                    )
                }
                ItemText(
                    title = "ID",
                    text = userProfile.name,
                    onClick = { launch { modifyUserId(userProfile.name) } }
                )
                ItemText(
                    title = "个性签名",
                    text = userProfile.signature,
                    maxLines = 2,
                    onClick = { launch { modifyUserSignature(userProfile.signature) } }
                )
                Item(
                    title = "背景墙",
                    onClick = { launch { modifyUserWall() } }
                ) {
                    WebImage(
                        uri = userProfile.wallPath,
                        key = app.config.cacheUserWall,
                        modifier = Modifier.width(CustomTheme.size.largeImage).aspectRatio(1.77778f)
                            .shadow(CustomTheme.shadow.icon)
                    )
                }
                ItemText(
                    title = "邀请人",
                    text = userProfile.inviterName ?: ""
                )
                ItemExpander(
                    title = "修改密码",
                    icon = colorfulImageVector(icon = Icons.Outlined.Password, background = Colors.Orange4),
                    onClick = { passwordModifySheet.open() }
                )
                ItemExpanderSuspend(
                    title = "重置默认图片",
                    icon = colorfulImageVector(
                        icon = ExtraIcons.ResetPicture,
                        background = CustomTheme.colorScheme.warning
                    ),
                    onClick = { resetPicture() }
                )
                ItemExpanderSuspend(
                    title = "退出登录",
                    icon = colorfulImageVector(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        background = CustomTheme.colorScheme.warning
                    ),
                    color = CustomTheme.colorScheme.warning,
                    hasDivider = false,
                    onClick = { logoff() }
                )
            }
        }
    }

    @Composable
    private fun PreferencesSettings(modifier: Modifier = Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "偏好",
            icon = Icons.Outlined.Construction
        ) {
            Item(
                title = "主题",
                icon = colorfulImageVector(
                    icon = when (app.config.themeMode) {
                        ThemeMode.SYSTEM -> Icons.Outlined.Contrast
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                    },
                    background = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                SingleSelector(
                    current = app.config.themeMode,
                    onSelected = { app.config.themeMode = it },
                    style = MaterialTheme.typography.bodySmall,
                    hasIcon = false,
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace)
                ) {
                    for (themeMode in ThemeMode.entries) {
                        this.Item(themeMode, themeMode.toString())
                    }
                }
            }

            Item(
                title = "动画速度",
                icon = colorfulImageVector(
                    icon = Icons.Outlined.Animation,
                    background = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                val animationSpeedValue = remember { intArrayOf(600, 400, 200) }
                val animationSpeedString = remember { arrayOf("慢", "标准", "快") }
                SingleSelector(
                    current = LocalAnimationSpeed.current,
                    onSelected = { app.config.animationSpeed = it },
                    style = MaterialTheme.typography.bodySmall,
                    hasIcon = false,
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace)
                ) {
                    repeat(animationSpeedValue.size) {
                        this.Item(animationSpeedValue[it], animationSpeedString[it])
                    }
                }
            }

            Item(
                title = "字体大小",
                icon = colorfulImageVector(
                    icon = Icons.Outlined.FormatSize,
                    background = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                val fontScaleValue = remember { floatArrayOf(0.83333f, 1f, 1.2f) }
                val fontScaleString = remember { arrayOf("小", "标准", "大") }
                SingleSelector(
                    current = app.config.fontScale,
                    onSelected = { app.config.fontScale = it },
                    style = MaterialTheme.typography.bodySmall,
                    hasIcon = false,
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace)
                ) {
                    repeat(fontScaleValue.size) {
                        this.Item(fontScaleValue[it], fontScaleString[it])
                    }
                }
            }

            ItemSwitch(
                title = "音频焦点",
                icon = colorfulImageVector(
                    icon = Icons.Outlined.MusicNote,
                    background = MaterialTheme.colorScheme.primaryContainer
                ),
                checked = app.config.audioFocus,
                onCheckedChange = {
                    app.config.audioFocus = it
                    slot.tip.success("重启APP后生效")
                }
            )

            ItemSwitch(
                title = "悬浮提示",
                icon = colorfulImageVector(
                    icon = Icons.Outlined.Lightbulb,
                    background = MaterialTheme.colorScheme.primaryContainer
                ),
                hasDivider = false,
                checked = app.config.enabledTip,
                onCheckedChange = {
                    app.config.enabledTip = it
                }
            )
        }
    }

    @Composable
    private fun ApplicationSettings(modifier: Modifier = Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "应用",
            icon = Icons.Outlined.Info
        ) {
            var cacheSizeText by rememberState { service.os.storage.cacheSize.fileSizeString }
            ItemExpanderSuspend(
                title = "清理缓存",
                icon = colorfulImageVector(icon = Icons.Outlined.DeleteSweep, background = Colors.Red4),
                text = cacheSizeText,
                onClick = { cacheSizeText = clearCache() }
            )

            ItemExpander(
                title = "崩溃日志",
                icon = colorfulImageVector(icon = Icons.Outlined.Description, background = Colors.Yellow4),
                onClick = { crashLogSheet.open() }
            )

            ItemExpanderSuspend(
                title = "检查更新",
                icon = colorfulImageVector(icon = Icons.Outlined.RocketLaunch, background = Colors.Yellow4),
                text = Local.info.versionName,
                onClick = { checkUpdate() }
            )

            ItemExpander(
                title = "反馈与建议",
                icon = colorfulImageVector(icon = Icons.Outlined.Draw, background = Colors.Green4),
                onClick = { feedbackSheet.open() }
            )

            ItemExpander(
                title = "关于茶舍",
                icon = colorfulImageVector(icon = Icons.Outlined.Face, background = Colors.Green4),
                onClick = { aboutSheet.open() }
            )

            ItemExpander(
                title = "隐私政策",
                icon = colorfulImageVector(icon = Icons.Outlined.VerifiedUser, background = Colors.Yellow4),
                hasDivider = false,
                onClick = { privacyPolicySheet.open() }
            )
        }
    }

    @Composable
    private fun Portrait(userProfile: UserProfile?) {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AccountSettings(
                userProfile = userProfile,
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
            )
            PreferencesSettings(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
            )
            ApplicationSettings(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
            )
        }
    }

    @Composable
    private fun Landscape(userProfile: UserProfile?) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(CustomTheme.size.panelWidth),
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            item(key = ItemKey("AccountSettings")) {
                AccountSettings(
                    userProfile = userProfile,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
            }
            item(key = ItemKey("PreferencesSettings")) {
                PreferencesSettings(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
            }
            item(key = ItemKey("ApplicationSettings")) {
                ApplicationSettings(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
            }
        }
    }

    override val title: String = "设置"

    @Composable
    override fun Content(device: Device) = when (device.type) {
        Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(app.config.userProfile)
        Device.Type.LANDSCAPE -> Landscape(app.config.userProfile)
    }

    private val crashLogSheet = object : FloatingSheet() {
        @Composable
        override fun Content() {
            val text = remember { app.kv.get(AppContext.CRASH_KEY, "无崩溃日志") }
            Box(modifier = Modifier.fillMaxWidth()
                .padding(CustomTheme.padding.sheetValue)
                .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private val feedbackSheet = object : FloatingSheet() {
        @Composable
        override fun Content() {
            val state = rememberTextInputState()

            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                LoadingIcon(
                    icon = Icons.Outlined.Check,
                    enabled = state.ok,
                    onClick = { sendFeedback(state.text) }
                )
                TextInput(
                    state = state,
                    hint = "您的建议",
                    maxLength = 512,
                    maxLines = 5,
                    clearButton = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val privacyPolicySheet = object : FloatingSheet() {
        @Composable
        override fun Content() {
            Box(modifier = Modifier.fillMaxWidth()
                .padding(CustomTheme.padding.sheetValue)
                .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = stringResource(Res.string.app_privacy_policy),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private val aboutSheet = object : FloatingSheet() {
        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(CustomTheme.padding.equalExtraValue)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 2f)
            ) {
                MiniImage(
                    res = Res.drawable.img_logo,
                    modifier = Modifier.size(CustomTheme.size.mediumImage).clip(CircleShape)
                )
                Text(
                    text = Local.info.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = Local.info.versionName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(Res.string.app_description),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
                Space()
                Text(
                    text = "支持平台",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    MiniIcon(icon = Icons.Outlined.Android)
                    MiniIcon(icon = ExtraIcons.IOS)
                    MiniIcon(icon = ExtraIcons.Windows)
                    MiniIcon(icon = ExtraIcons.Linux)
                    MiniIcon(icon = ExtraIcons.MacOS)
                    MiniIcon(icon = ExtraIcons.WasmJs)
                    MiniIcon(icon = Icons.Outlined.Dns)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryButton(
                        text = "银临茶舍官网",
                        icon = Icons.Outlined.Home,
                        onClick = {
                            launch {
                                Uri.parse(getString(Res.string.app_website))?.let {
                                    service.os.net.openUri(it)
                                }
                            }
                        }
                    )
                    TertiaryButton(
                        text = "Github开源",
                        icon = ExtraIcons.Github,
                        onClick = {
                            launch {
                                Uri.parse(getString(Res.string.app_repository))?.let {
                                    service.os.net.openUri(it)
                                }
                            }
                        }
                    )
                }
                Space()
                Text(
                    text = "项目组",
                    style = MaterialTheme.typography.labelLarge,
                )
                ContributorList(
                    contributors = About.contributors,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigate(ScreenUserCard.Args(it.uid)) }
                )
                UpdateInfoLayout(
                    updateInfo = About.updateInfo,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val passwordModifySheet = object : FloatingSheet() {
        private suspend fun submit(oldPwd: String, newPwd1: String, newPwd2: String) {
            if (newPwd1 != newPwd2) slot.tip.warning("两次输入密码不同")
            else if (oldPwd == newPwd1) slot.tip.warning("旧密码与新密码相同")
            else if (!UserConstraint.checkPassword(oldPwd, newPwd1)) slot.tip.warning("密码不合法")
            else modifyPassword(oldPwd, newPwd1)
        }

        @Composable
        override fun Content() {
            val oldPassword = rememberTextInputState()
            val newPassword1 = rememberTextInputState()
            val newPassword2 = rememberTextInputState()

            val canSubmit by rememberDerivedState { oldPassword.ok && newPassword1.ok && newPassword2.ok }

            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                LoadingClickText(
                    text = "提交",
                    icon = Icons.Outlined.Check,
                    enabled = canSubmit,
                    onClick = {
                        submit(oldPassword.text, newPassword1.text, newPassword2.text)
                    }
                )
                TextInput(
                    state = oldPassword,
                    hint = "旧密码",
                    inputType = InputType.PASSWORD,
                    maxLength = UserConstraint.MAX_PWD_LENGTH,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                TextInput(
                    state = newPassword1,
                    hint = "确认旧密码",
                    inputType = InputType.PASSWORD,
                    maxLength = UserConstraint.MAX_PWD_LENGTH,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                TextInput(
                    state = newPassword2,
                    hint = "新密码",
                    inputType = InputType.PASSWORD,
                    maxLength = UserConstraint.MAX_PWD_LENGTH,
                    onImeClick = {
                        if (canSubmit) launch {
                            submit(oldPassword.text, newPassword1.text, newPassword2.text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val cropDialog = FloatingDialogCrop()

    private val idModifyDialog = FloatingDialogInput(
        hint = "修改ID(消耗${UserConstraint.RENAME_COIN_COST}银币)",
        maxLength = UserConstraint.MAX_NAME_LENGTH
    )

    private val signatureModifyDialog = FloatingDialogInput(
        hint = "修改个性签名",
        maxLength = UserConstraint.MAX_SIGNATURE_LENGTH,
        maxLines = 3,
        clearButton = false
    )

    @Composable
    override fun Floating() {
        passwordModifySheet.Land()
        crashLogSheet.Land()
        feedbackSheet.Land()
        privacyPolicySheet.Land()
        aboutSheet.Land()
        cropDialog.Land()
        idModifyDialog.Land()
        signatureModifyDialog.Land()
    }
}
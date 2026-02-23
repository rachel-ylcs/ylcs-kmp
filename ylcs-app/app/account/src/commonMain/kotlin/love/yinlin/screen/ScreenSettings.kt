package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.io.files.Path
import love.yinlin.Local
import love.yinlin.app
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.img_logo
import love.yinlin.common.DataSourceAccount
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.bold
import love.yinlin.compose.config.CacheState
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.crop
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.graphics.thumbnail
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.ContributorLayout
import love.yinlin.compose.ui.common.SettingsLayout
import love.yinlin.compose.ui.common.UpdateInfoLayout
import love.yinlin.compose.ui.floating.DialogCrop
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryLoadingTextButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.TertiaryButton
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.PasswordInput
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.*
import love.yinlin.data.config.AnimationSpeedConfig
import love.yinlin.data.config.FontScaleConfig
import love.yinlin.data.rachel.literal.AppAbout
import love.yinlin.data.rachel.literal.AppDescription
import love.yinlin.data.rachel.literal.AppPrivacyPolicy
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.readByteArray
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

@Stable
class ScreenSettings : Screen() {
    private suspend fun pickPicture(aspectRatio: Float): Path? {
        return app.picker.pickPicture()?.use { source ->
            app.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }?.let { path ->
            cropDialog.open(url = path.toString(), aspectRatio = aspectRatio)?.let { region ->
                app.os.storage.createTempFile { sink ->
                    val image = PlatformImage.decode(path.readByteArray()!!)!!
                    image.crop(region)
                    image.thumbnail()
                    sink.write(image.encode(quality = ImageQuality.High)!!)
                    true
                }
            }
        }
    }

    private suspend fun modifyUserAvatar() {
        pickPicture(1f)?.let { path ->
            ApiProfileUpdateAvatar.request(app.config.userToken, apiFile(path)) {
                app.config.cacheUserAvatar = CacheState.UPDATE
            }.errorTip
        }
    }

    private suspend fun modifyUserWall() {
        pickPicture(1.77777f)?.let { path ->
            ApiProfileUpdateWall.request(app.config.userToken, apiFile(path)) {
                app.config.cacheUserWall = CacheState.UPDATE
            }.errorTip
        }
    }

    private suspend fun modifyUserId(initText: String) {
        idModifyDialog.open(initText)?.let { text ->
            val profile = app.config.userProfile
            if (profile != null && profile.coin >= UserConstraint.RENAME_COIN_COST) {
                ApiProfileUpdateName.request(app.config.userToken, text) {
                    app.config.userProfile = profile.copy(name = text, coin = profile.coin - UserConstraint.RENAME_COIN_COST)
                }.errorTip
            }
        }
    }

    private suspend fun modifyUserSignature(initText: String) {
        signatureModifyDialog.open(initText)?.let { text ->
            val profile = app.config.userProfile
            if (profile != null) {
                ApiProfileUpdateSignature.request(app.config.userToken, text) {
                    app.config.userProfile = profile.copy(signature = text)
                }.errorTip
            }
        }
    }

    private suspend fun modifyPassword(oldPwd: String, newPwd: String) {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            ApiAccountChangePassword.request(token, oldPwd, newPwd) {
                DataSourceAccount.cleanUserToken()
                slot.tip.success("修改密码成功, 请重新登录")
            }.errorTip
        }
    }

    private suspend fun resetPicture() {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            if (slot.confirm.open(content = "重置默认头像与背景墙")) {
                ApiProfileResetPicture.request(token) {
                    app.config.cacheUserAvatar = CacheState.UPDATE
                    app.config.cacheUserWall = CacheState.UPDATE
                }.errorTip
            }
        }
    }

    private suspend fun sendFeedback(content: String) {
        ApiCommonSendFeedback.request(app.config.userToken, content) {
            feedbackSheet.close()
        }.errorTip
    }

    private suspend fun checkUpdate() {
        ApiCommonGetServerStatus.request { status ->
            if (status.targetVersion > Local.info.version) slot.tip.warning("新版本${status.targetVersion}可用")
            else if (status.minVersion > Local.info.version) slot.tip.error("当前不满足最低兼容版本${status.minVersion}")
            else slot.tip.success("当前已是最新版本")
        }.errorTip
    }

    override val title: String = "设置"

    @Composable
    private fun AccountSettings(profile: UserProfile?, modifier: Modifier = Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "账号",
            icon = Icons.AccountCircle,
            color = Theme.color.secondaryContainer
        ) {
            if (profile == null) {
                ItemExpander(
                    title = "登录",
                    icon = Icons.Login,
                    hasDivider = false,
                    onClick = {
                        pop()
                        navigate(::ScreenLogin)
                    }
                )
            }
            else {
                Item(
                    title = "头像",
                    onClick = {
                        launch { modifyUserAvatar() }
                    }
                ) {
                    WebImage(
                        uri = profile.avatarPath.url,
                        key = app.config.cacheUserAvatar,
                        contentScale = ContentScale.Crop,
                        circle = true,
                        modifier = Modifier.size(Theme.size.image8).shadow(Theme.shape.circle, Theme.shadow.v7)
                    )
                }
                ItemText(
                    title = "ID",
                    text = profile.name,
                    onClick = {
                        launch { modifyUserId(profile.name) }
                    }
                )
                ItemText(
                    title = "个性签名",
                    text = profile.signature,
                    maxLines = 2,
                    onClick = {
                        launch { modifyUserSignature(profile.signature) }
                    }
                )
                Item(
                    title = "背景墙",
                    onClick = {
                        launch { modifyUserWall() }
                    }
                ) {
                    WebImage(
                        uri = profile.wallPath.url,
                        key = app.config.cacheUserWall,
                        modifier = Modifier.width(Theme.size.image5).aspectRatio(1.77778f)
                    )
                }
                ItemText(
                    title = "邀请人",
                    text = profile.inviterName ?: ""
                )
                ItemExpander(
                    title = "修改密码",
                    icon = Icons.Password,
                    onClick = { passwordModifySheet.open() }
                )
                ItemExpanderSuspend(
                    title = "重置默认图片",
                    icon = Icons.ResetPicture,
                    onClick = ::resetPicture
                )
                ItemExpanderSuspend(
                    title = "退出登录",
                    icon = Icons.Logout,
                    hasDivider = false,
                    onClick = {
                        if (slot.confirm.open(content = "退出登录")) DataSourceAccount.logoff()
                    }
                )
            }
        }
    }

    @Composable
    private fun PreferencesSettings(modifier: Modifier = Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "偏好",
            icon = Icons.Construction,
            color = Theme.color.primaryContainer
        ) {
            Item(
                title = "主题",
                icon = when (app.config.themeMode) {
                    ThemeMode.SYSTEM -> Icons.Contrast
                    ThemeMode.LIGHT -> Icons.LightMode
                    ThemeMode.DARK -> Icons.DarkMode
                },
                onClick = null
            ) {
                val themeText = arrayOf(Theme.value.systemThemeText, Theme.value.lightThemeText, Theme.value.darkThemeText)

                Filter(
                    size = ThemeMode.entries.size,
                    selectedProvider = { app.config.themeMode.ordinal == it },
                    titleProvider = { themeText[it] },
                    onClick = { index, selected -> if (selected) app.config.themeMode = ThemeMode.entries[index] },
                    style = Theme.typography.v8,
                    activeIcon = null,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                )
            }

            Item(
                title = "动画速度",
                icon = Icons.Animation,
                onClick = null
            ) {
                Filter(
                    size = AnimationSpeedConfig.entries.size,
                    selectedProvider = { app.config.animationSpeed.ordinal == it },
                    titleProvider = { AnimationSpeedConfig.entries[it].title },
                    onClick = { index, selected -> if (selected) app.config.animationSpeed = AnimationSpeedConfig.entries[index] },
                    style = Theme.typography.v8,
                    activeIcon = null,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                )
            }

            Item(
                title = "字体大小",
                icon = Icons.TextFields,
                onClick = null
            ) {
                Filter(
                    size = FontScaleConfig.entries.size,
                    selectedProvider = { app.config.fontScale.ordinal == it },
                    titleProvider = { FontScaleConfig.entries[it].title },
                    onClick = { index, selected -> if (selected) app.config.fontScale = FontScaleConfig.entries[index] },
                    style = Theme.typography.v8,
                    activeIcon = null,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                )
            }

            ItemSwitch(
                title = "音频焦点",
                icon = Icons.MusicNote,
                checked = app.config.audioFocus,
                onCheckedChange = {
                    app.config.audioFocus = it
                    slot.tip.success("重启APP后生效")
                }
            )

            ItemSwitch(
                title = "悬浮提示",
                icon = Icons.Lightbulb,
                hasDivider = false,
                checked = app.config.enabledTip,
                onCheckedChange = { app.config.enabledTip = it }
            )
        }
    }

    @Composable
    private fun ApplicationSettings(modifier: Modifier = Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "应用",
            icon = Icons.Info,
            color = Theme.color.tertiaryContainer
        ) {
            var cacheSizeText: String? by rememberState { null }

            LaunchedEffect(Unit) {
                cacheSizeText = Coroutines.io {
                    app.os.storage.calcCacheSize().fileSizeString
                }
            }

            ItemExpanderSuspend(
                title = "清理缓存",
                icon = Icons.DeleteSweep,
                text = cacheSizeText ?: "正在计算...",
                onClick = {
                    if (cacheSizeText == null) slot.tip.warning("请等待缓存计算完成")
                    else {
                        Coroutines.io {
                            app.urlImage.clearCache()
                            app.os.storage.clearCache()
                            cacheSizeText = app.os.storage.calcCacheSize().fileSizeString
                        }
                    }
                }
            )

            ItemExpander(
                title = "崩溃日志",
                icon = Icons.Description,
                onClick = { crashLogSheet.open() }
            )

            ItemExpanderSuspend(
                title = "检查更新",
                icon = Icons.RocketLaunch,
                text = Local.info.versionName,
                onClick = ::checkUpdate
            )

            ItemExpander(
                title = "反馈与建议",
                icon = Icons.Draw,
                onClick = { feedbackSheet.open() }
            )

            ItemExpander(
                title = "隐私政策",
                icon = Icons.VerifiedUser,
                onClick = { privacyPolicySheet.open() }
            )

            ItemExpander(
                title = "更新日志",
                icon = Icons.History,
                onClick = { updateLogSheet.open() }
            )

            ItemExpander(
                title = "关于茶舍",
                icon = Icons.Face,
                hasDivider = false,
                onClick = { aboutSheet.open() }
            )
        }
    }

    @Composable
    private fun Portrait(profile: UserProfile?) {
        Column(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
            AccountSettings(profile = profile, modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            PreferencesSettings(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            ApplicationSettings(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
        }
    }

    @Composable
    private fun Landscape(profile: UserProfile?) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            item(key = ItemKey("AccountSettings")) {
                AccountSettings(profile = profile, modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            }
            item(key = ItemKey("PreferencesSettings")) {
                PreferencesSettings(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            }
            item(key = ItemKey("ApplicationSettings")) {
                ApplicationSettings(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue))
            }
        }
    }

    @Composable
    override fun Content() {
        val profile = app.config.userProfile

        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(profile)
            Device.Type.LANDSCAPE -> Landscape(profile)
        }
    }

    private val cropDialog = this land DialogCrop()

    private val idModifyDialog = this land DialogInput(
        hint = "修改ID(消耗${UserConstraint.RENAME_COIN_COST}银币)",
        maxLength = UserConstraint.MAX_NAME_LENGTH,
        trailing = InputDecoration.Icon.Clear
    )

    private val signatureModifyDialog = this land DialogInput(
        hint = "修改个性签名",
        maxLength = UserConstraint.MAX_SIGNATURE_LENGTH,
        maxLines = 3
    )

    private val crashLogSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            SelectionBox {
                Text(
                    text = remember { app.kv.get(app.exceptionHandler.crashKey, "无崩溃日志") },
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9)
                )
            }
        }
    }

    private val feedbackSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            val state = rememberInputState(maxLength = 512)

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = "反馈与建议", style = Theme.typography.v6.bold)
                    PrimaryLoadingTextButton(
                        text = "提交",
                        icon = Icons.Check,
                        enabled = state.isSafe,
                        onClick = { sendFeedback(state.text) }
                    )
                }
                Input(
                    state = state,
                    hint = "您的建议",
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val privacyPolicySheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            Box(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9)) {
                SelectionBox {
                    Text(text = AppPrivacyPolicy, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    private val updateLogSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            UpdateInfoLayout(
                updateInfo = AppAbout.updateInfo,
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9)
            )
        }
    }

    private val aboutSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
            ) {
                Image(
                    res = Res.drawable.img_logo,
                    modifier = Modifier.size(Theme.size.image7).clip(Theme.shape.circle)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = Local.info.name, style = Theme.typography.v6.bold, color = Theme.color.primary)
                    SimpleEllipsisText(text = Local.info.versionName, style = Theme.typography.v6)
                }
                Text(text = AppDescription, overflow = TextOverflow.Ellipsis)
                SimpleEllipsisText(text = "支持平台", style = Theme.typography.v6.bold)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    Icon(icon = Icons.Android)
                    Icon(icon = Icons2.IOS)
                    Icon(icon = Icons2.Windows)
                    Icon(icon = Icons2.Linux)
                    Icon(icon = Icons2.MacOS)
                    Icon(icon = Icons2.WasmJs)
                    Icon(icon = Icons.Dns)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryButton(
                        text = "银临茶舍官网",
                        icon = Icons.Home,
                        onClick = { app.os.net.openUri(Uri(scheme = Scheme.Https, host = "yinlin.love")) }
                    )
                    TertiaryButton(
                        text = "Github开源",
                        icon = Icons2.Github,
                        onClick = { app.os.net.openUri(Uri(scheme = Scheme.Https, host = "github.com", path = "/rachel-ylcs/ylcs-kmp")) }
                    )
                }

                SimpleEllipsisText(text = "项目组", style = Theme.typography.v6.bold)

                ContributorLayout(
                    contributors = AppAbout.contributors,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigate(::ScreenUserCard, it.uid) }
                )
            }
        }
    }

    private val passwordModifySheet = this land object : Sheet() {
        private suspend fun submit(oldPwd: String, newPwd1: String, newPwd2: String) {
            if (newPwd1 != newPwd2) slot.tip.warning("两次输入密码不同")
            else if (oldPwd == newPwd1) slot.tip.warning("旧密码与新密码相同")
            else if (!UserConstraint.checkPassword(oldPwd, newPwd1)) slot.tip.warning("密码不合法")
            else modifyPassword(oldPwd, newPwd1)
        }

        @Composable
        override fun Content() {
            val oldPassword = rememberInputState(maxLength = UserConstraint.MAX_PWD_LENGTH)
            val newPassword1 = rememberInputState(maxLength = UserConstraint.MAX_PWD_LENGTH)
            val newPassword2 = rememberInputState(maxLength = UserConstraint.MAX_PWD_LENGTH)

            val canSubmit by rememberDerivedState { oldPassword.isSafe && newPassword1.isSafe && newPassword2.isSafe }

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = "修改密码", style = Theme.typography.v6.bold)
                    PrimaryLoadingTextButton(
                        text = "提交",
                        icon = Icons.Check,
                        enabled = canSubmit,
                        onClick = { submit(oldPassword.text, newPassword1.text, newPassword2.text) }
                    )
                }
                PasswordInput(
                    state = oldPassword,
                    hint = "旧密码",
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordInput(
                    state = newPassword1,
                    hint = "新密码",
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordInput(
                    state = newPassword2,
                    hint = "确认新密码",
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
}
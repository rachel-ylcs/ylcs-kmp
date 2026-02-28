package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import love.yinlin.app
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.img_logo
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.input.ComboBox
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.PasswordInput
import love.yinlin.cs.ApiAccountForgotPassword
import love.yinlin.cs.ApiAccountGetInviters
import love.yinlin.cs.ApiAccountLogin
import love.yinlin.cs.ApiAccountRegister
import love.yinlin.cs.request
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.extension.DateEx
import love.yinlin.platform.platform

@Stable
class ScreenLogin : Screen() {
    @Stable
    private enum class Mode(val title: String) {
        Login("登录"),
        Register("注册"),
        ForgotPassword("忘记密码");
    }

    private var inviters by mutableStateOf(emptyList<String>())

    private var mode: Mode by mutableStateOf(Mode.Login)
    private val loginId = InputState(maxLength = UserConstraint.MAX_NAME_LENGTH)
    private val loginPwd = InputState(maxLength = UserConstraint.MAX_PWD_LENGTH)
    private val registerId = InputState(maxLength = UserConstraint.MAX_NAME_LENGTH)
    private val registerPwd = InputState(maxLength = UserConstraint.MAX_PWD_LENGTH)
    private val registerPwd2 = InputState(maxLength = UserConstraint.MAX_PWD_LENGTH)
    private var registerInviter by mutableIntStateOf(-1)
    private val forgotPasswordId = InputState(maxLength = UserConstraint.MAX_NAME_LENGTH)
    private val forgotPasswordPwd = InputState(maxLength = UserConstraint.MAX_PWD_LENGTH)

    private val canLogin by derivedStateOf { loginId.isSafe && loginPwd.isSafe }
    private val canRegister by derivedStateOf { registerId.isSafe && registerPwd.isSafe && registerPwd2.isSafe }
    private val canForgotPassword by derivedStateOf { forgotPasswordId.isSafe && forgotPasswordPwd.isSafe }

    private suspend fun login() {
        val id = loginId.text
        val pwd = loginPwd.text
        if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
            slot.tip.error("昵称或密码不合规范")
            return
        }
        ApiAccountLogin.request(id, pwd, platform) { token ->
            app.config.userShortToken = DateEx.CurrentLong
            app.config.userToken = token
            pop()
        }.errorTip
    }

    private suspend fun register() {
        val id = registerId.text
        val pwd = registerPwd.text
        val pwd2 = registerPwd2.text
        val inviter = inviters.getOrNull(registerInviter) ?: ""
        if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd, pwd2)) {
            slot.tip.error("昵称或密码不合规范")
            return
        }
        if (!UserConstraint.checkName(inviter)) {
            slot.tip.error("邀请人不合规范")
            return
        }
        if (pwd != pwd2) {
            slot.tip.error("两次输入的密码不相同")
            return
        }
        ApiAccountRegister.request(id, pwd, inviter) {
            mode = Mode.Login
            loginId.text = id
            loginPwd.text = ""
            slot.tip.success("提交申请成功, 请等待管理员审核")
        }.errorTip
    }

    private suspend fun forgotPassword() {
        val id = forgotPasswordId.text
        val pwd = forgotPasswordPwd.text
        if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
            slot.tip.error("昵称或密码不合规范")
            return
        }
        ApiAccountForgotPassword.request(id, pwd) {
            mode = Mode.Login
            loginId.text = id
            loginPwd.text = ""
            slot.tip.success("提交申请成功, 请等待管理员审核")
        }.errorTip
    }

    override val title: String get() = mode.title

    override suspend fun initialize() {
        ApiAccountGetInviters.request { inviters = it }
    }

    @Composable
    private fun ContentLogin(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
        ) {
            Input(
                modifier = Modifier.fillMaxWidth(),
                state = loginId,
                hint = "昵称",
                imeAction = ImeAction.Next,
                trailing = InputDecoration.Icon.Clear
            )

            PasswordInput(
                modifier = Modifier.fillMaxWidth(),
                state = loginPwd,
                hint = "密码",
                onImeClick = {
                    if (canLogin) launch { login() }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryTextButton(text = "没有账号?", onClick = {
                    mode = Mode.Register
                    registerId.text = loginId.text
                    registerPwd.text = ""
                    registerPwd2.text = ""
                    registerInviter = -1
                })
                SecondaryTextButton(text = "忘记密码?", onClick = {
                    mode = Mode.ForgotPassword
                    forgotPasswordId.text = loginId.text
                    forgotPasswordPwd.text = ""
                })
            }

            PrimaryLoadingButton(
                modifier = Modifier.fillMaxWidth(),
                text = "登录",
                enabled = canLogin,
                onClick = ::login
            )
        }
    }

    @Composable
    private fun ContentRegister(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
        ) {
            Input(
                modifier = Modifier.fillMaxWidth(),
                state = registerId,
                hint = "注册昵称",
                imeAction = ImeAction.Next,
                trailing = InputDecoration.Icon.Clear
            )

            PasswordInput(
                modifier = Modifier.fillMaxWidth(),
                state = registerPwd,
                hint = "密码"
            )

            PasswordInput(
                modifier = Modifier.fillMaxWidth(),
                state = registerPwd2,
                hint = "确认密码",
            )

            ComboBox(
                items = inviters,
                onSelect = { registerInviter = it },
                hint = "邀请人",
                index = registerInviter,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryTextButton(
                text = "返回登录",
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    mode = Mode.Login
                    loginId.text = ""
                    loginPwd.text = ""
                },
            )

            PrimaryLoadingButton(
                modifier = Modifier.fillMaxWidth(),
                text = "注册",
                enabled = canRegister,
                onClick = ::register
            )
        }
    }

    @Composable
    private fun ContentForgotPassword(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
        ) {
            Input(
                modifier = Modifier.fillMaxWidth(),
                state = forgotPasswordId,
                hint = "昵称",
                imeAction = ImeAction.Next,
                trailing = InputDecoration.Icon.Clear
            )

            PasswordInput(
                modifier = Modifier.fillMaxWidth(),
                state = forgotPasswordPwd,
                hint = "新密码",
                onImeClick = {
                    if (canForgotPassword) launch { forgotPassword() }
                }
            )

            SecondaryTextButton(
                text = "返回登录",
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    mode = Mode.Login
                    loginId.text = ""
                    loginPwd.text = ""
                },
            )

            PrimaryLoadingButton(
                modifier = Modifier.fillMaxWidth(),
                text = "提交申请",
                enabled = canForgotPassword,
                onClick = ::forgotPassword
            )
        }
    }

    @Composable
    private fun ContentBox(modifier: Modifier = Modifier) {
        AnimationContent(
            state = mode,
            modifier = modifier,
        ) {
            when (it) {
                Mode.Login -> ContentLogin(modifier = Modifier.fillMaxWidth())
                Mode.Register -> ContentRegister(modifier = Modifier.fillMaxWidth())
                Mode.ForgotPassword -> ContentForgotPassword(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .padding(Theme.padding.eValue8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
        ) {
            Image(res = Res.drawable.img_logo, modifier = Modifier.size(Theme.size.image4))
            ContentBox(modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape() {
        Row(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .padding(Theme.padding.eValue6),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(res = Res.drawable.img_logo, modifier = Modifier.size(Theme.size.image4))
            }
            ContentBox(modifier = Modifier.weight(2f))
        }
    }

    @Composable
    override fun Content() {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> Portrait()
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
        }
    }
}
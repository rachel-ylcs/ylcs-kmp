package love.yinlin.ui.screen.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.input.LoadingButton
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.resources.Res
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.image.MiniIcon

@Stable
class ScreenLogin(model: AppModel) : CommonSubScreen(model) {
	@Stable
	@Serializable
	private enum class Mode {
		Login,
		Register,
		ForgotPassword
	}

	private var mode by mutableStateOf(Mode.Login)
	private val loginId = TextInputState()
	private val loginPwd = TextInputState()
	private val registerId = TextInputState()
	private val registerPwd = TextInputState()
	private val registerPwd2 = TextInputState()
	private val registerInviter = TextInputState()
	private val forgotPasswordId = TextInputState()
	private val forgotPasswordPwd = TextInputState()

	private val canLogin by derivedStateOf { loginId.ok && loginPwd.ok }
	private val canRegister by derivedStateOf { registerId.ok && registerPwd.ok && registerPwd2.ok }
	private val canForgotPassword by derivedStateOf { forgotPasswordId.ok && forgotPasswordPwd.ok }

	private suspend fun login() {
		val id = loginId.text
		val pwd = loginPwd.text
		if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
			slot.tip.error("昵称或密码不合规范")
			return
		}
		val result1 = ClientAPI.request(
			route = API.User.Account.Login,
			data = API.User.Account.Login.Request(
				name = id,
				pwd = pwd,
				platform = OS.platform
			)
		)
		when (result1) {
			is Data.Success -> {
				val token = result1.data
				app.config.userToken = token
				val result2 = ClientAPI.request(
					route = API.User.Profile.GetProfile,
					data = token
				)
				if (result2 is Data.Success) app.config.userProfile = result2.data
				pop()
			}
			is Data.Error -> slot.tip.error(result1.message)
		}
	}

	private suspend fun register() {
		val id = registerId.text
		val pwd = registerPwd.text
		val pwd2 = registerPwd2.text
		val inviter = registerInviter.text
		if (!UserConstraint.checkName(id, inviter) || !UserConstraint.checkPassword(pwd, pwd2)) {
			slot.tip.error("昵称或密码不合规范")
			return
		}
		if (pwd != pwd2) {
			slot.tip.error("两次输入的密码不相同")
			return
		}
		val result = ClientAPI.request(
			route = API.User.Account.Register,
			data = API.User.Account.Register.Request(
				name = id,
				pwd = pwd,
				inviterName = inviter
			)
		)
		when (result) {
			is Data.Success -> {
				mode = Mode.Login
				loginId.text = id
				loginPwd.text = ""
				slot.tip.success(result.message)
			}
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	private suspend fun forgotPassword() {
		val id = forgotPasswordId.text
		val pwd = forgotPasswordPwd.text
		if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
			slot.tip.error("昵称或密码不合规范")
			return
		}
		val result = ClientAPI.request(
			route = API.User.Account.ForgotPassword,
			data = API.User.Account.ForgotPassword.Request(
				name = id,
				pwd = pwd
			)
		)
		when (result) {
			is Data.Success -> {
				mode = Mode.Login
				loginId.text = id
				loginPwd.text = ""
				slot.tip.success(result.message)
			}
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	@Composable
	private fun ContentLogin(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = loginId,
				hint = "昵称",
				maxLength = UserConstraint.MAX_NAME_LENGTH
			)
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = loginPwd,
				hint = "密码",
				inputType = InputType.PASSWORD,
				maxLength = UserConstraint.MAX_PWD_LENGTH
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.End),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "没有账号?",
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.clickable {
						mode = Mode.Register
						registerId.text = loginId.text
						registerPwd.text = ""
						registerPwd2.text = ""
						registerInviter.text = ""
					}
				)
				Text(
					text = "忘记密码?",
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.clickable {
						mode = Mode.ForgotPassword
						forgotPasswordId.text = loginId.text
						forgotPasswordPwd.text = ""
					}
				)
			}
			LoadingButton(
				modifier = Modifier.fillMaxWidth(),
				text = "登录",
				enabled = canLogin,
				onClick = { login() }
			)
		}
	}

	@Composable
	private fun ContentRegister(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = registerId,
				hint = "注册昵称",
				maxLength = UserConstraint.MAX_NAME_LENGTH
			)
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = registerPwd,
				hint = "密码",
				inputType = InputType.PASSWORD,
				maxLength = UserConstraint.MAX_PWD_LENGTH
			)
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = registerPwd2,
				hint = "确认密码",
				inputType = InputType.PASSWORD,
				maxLength = UserConstraint.MAX_PWD_LENGTH
			)
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = registerInviter,
				hint = "邀请人昵称",
				maxLength = UserConstraint.MAX_NAME_LENGTH
			)
			Text(
				text = "返回登录",
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.align(Alignment.End).clickable {
					mode = Mode.Login
					loginId.text = ""
					loginPwd.text = ""
				}
			)
			LoadingButton(
				modifier = Modifier.fillMaxWidth(),
				text = "注册",
				enabled = canRegister,
				onClick = { register() }
			)
		}
	}

	@Composable
	private fun ContentForgotPassword(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = forgotPasswordId,
				hint = "昵称",
				maxLength = UserConstraint.MAX_NAME_LENGTH
			)
			TextInput(
				modifier = Modifier.fillMaxWidth(),
				state = forgotPasswordPwd,
				hint = "新密码",
				inputType = InputType.PASSWORD,
				maxLength = UserConstraint.MAX_PWD_LENGTH
			)
			Text(
				text = "返回登录",
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.align(Alignment.End).clickable {
					mode = Mode.Login
					loginId.text = ""
					loginPwd.text = ""
				}
			)
			LoadingButton(
				modifier = Modifier.fillMaxWidth(),
				text = "提交申请",
				enabled = canForgotPassword,
				onClick = { forgotPassword() }
			)
		}
	}

	@Composable
	private fun ContentBox(modifier: Modifier = Modifier) {
		AnimatedContent(
			targetState = mode,
			modifier = modifier
		) { animatedMode ->
			when (animatedMode) {
				Mode.Login -> ContentLogin(modifier = Modifier.fillMaxSize())
				Mode.Register -> ContentRegister(modifier = Modifier.fillMaxSize())
				Mode.ForgotPassword -> ContentForgotPassword(modifier = Modifier.fillMaxSize())
			}
		}
	}

	@Composable
	private fun Portrait() {
		Column(
			modifier = Modifier
				.padding(LocalImmersivePadding.current)
				.fillMaxSize()
				.padding(ThemeValue.Padding.EqualValue),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			MiniIcon(
				res = Res.drawable.img_logo,
				size = ThemeValue.Size.LargeImage
			)
			ContentBox(modifier = Modifier.fillMaxWidth().weight(1f))
		}
	}

	@Composable
	private fun Landscape() {
		Row(
			modifier = Modifier
				.padding(LocalImmersivePadding.current)
				.fillMaxSize()
				.padding(ThemeValue.Padding.EqualValue),
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			Box(
				modifier = Modifier.weight(1f).fillMaxHeight(),
				contentAlignment = Alignment.Center
			) {
				MiniIcon(
					res = Res.drawable.img_logo,
					size = ThemeValue.Size.LargeImage
				)
			}
			Box(
				modifier = Modifier.weight(2f).fillMaxHeight(),
				contentAlignment = Alignment.Center
			) {
				ContentBox(modifier = Modifier.fillMaxSize())
			}
		}
	}

	override val title: String by derivedStateOf {
		when (mode) {
			Mode.Login -> "登录"
			Mode.Register -> "注册"
			Mode.ForgotPassword -> "忘记密码"
		}
	}

	@Composable
	override fun SubContent(device: Device) = when (device.type) {
		Device.Type.PORTRAIT -> Portrait()
		Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
	}
}
package love.yinlin.ui.screen.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ScreenModel
import love.yinlin.common.screen
import love.yinlin.data.Data
import love.yinlin.data.rachel.UserConstraint
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import org.jetbrains.compose.resources.painterResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.img_logo

private enum class Mode {
	Login,
	Register,
	ForgotPassword
}

private class LoginModel(private val model: AppModel) : ScreenModel() {
	val tip = TipState()
	val loadingState = DialogState()

	var mode by mutableStateOf(Mode.Login)
	val loginId = TextInputState()
	val loginPwd = TextInputState()
	val registerId = TextInputState()
	val registerPwd = TextInputState()
	val registerPwd2 = TextInputState()
	val registerInviter = TextInputState()
	val forgotPasswordId = TextInputState()
	val forgotPasswordPwd = TextInputState()

	val canLogin by derivedStateOf { loginId.ok && loginPwd.ok }
	val canRegister by derivedStateOf { registerId.ok && registerPwd.ok && registerPwd2.ok }
	val canForgotPassword by derivedStateOf { forgotPasswordId.ok && forgotPasswordPwd.ok }

	fun login() {
		launch {
			val id = loginId.text
			val pwd = loginPwd.text
			if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
				tip.error("ID或密码不合规范")
				return@launch
			}
			loadingState.isOpen = true
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
					loadingState.isOpen = false
					if (result2 is Data.Success) app.config.userProfile = result2.data
					model.pop()
				}
				is Data.Error -> {
					loadingState.isOpen = false
					tip.error(result1.message)
				}
			}
		}
	}

	fun register() {
		launch {
			TODO()
		}
	}

	fun forgotPassword() {
		launch {
			TODO()
		}
	}
}

@Composable
private fun ContentLogin(
	model: LoginModel,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.loginId,
			hint = "请输入ID",
			maxLength = UserConstraint.MAX_NAME_LENGTH
		)
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.loginPwd,
			hint = "请输入密码",
			inputType = InputType.PASSWORD,
			maxLength = UserConstraint.MAX_PWD_LENGTH
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.End),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "没有账号?",
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.clickable {
					model.mode = Mode.Register
					model.registerId.text = model.loginId.text
					model.registerPwd.text = ""
					model.registerPwd2.text = ""
					model.registerInviter.text = ""
				}
			)
			Text(
				text = "忘记密码?",
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.clickable {
					model.mode = Mode.ForgotPassword
					model.forgotPasswordId.text = model.loginId.text
					model.forgotPasswordPwd.text = ""
				}
			)
		}
		Button(
			modifier = Modifier.fillMaxWidth(),
			enabled = model.canLogin,
			onClick = { model.login() }
		) {
			Text(text = "登录")
		}
	}
}

@Composable
private fun ContentRegister(
	model: LoginModel,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.registerId,
			hint = "请输入注册昵称",
			maxLength = UserConstraint.MAX_NAME_LENGTH
		)
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.registerPwd,
			hint = "请输入密码",
			inputType = InputType.PASSWORD,
			maxLength = UserConstraint.MAX_PWD_LENGTH
		)
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.registerPwd2,
			hint = "请再确认一次密码",
			inputType = InputType.PASSWORD,
			maxLength = UserConstraint.MAX_PWD_LENGTH
		)
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.registerInviter,
			hint = "请输入邀请人昵称",
			maxLength = UserConstraint.MAX_NAME_LENGTH
		)
		Text(
			text = "返回登录",
			color = MaterialTheme.colorScheme.primary,
			modifier = Modifier.align(Alignment.End).clickable {
				model.mode = Mode.Login
				model.loginId.text = ""
				model.loginPwd.text = ""
			}
		)
		Button(
			modifier = Modifier.fillMaxWidth(),
			enabled = model.canRegister,
			onClick = { model.register() }
		) {
			Text(text = "注册")
		}
	}
}

@Composable
private fun ContentForgotPassword(
	model: LoginModel,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.forgotPasswordId,
			hint = "请输入昵称",
			maxLength = UserConstraint.MAX_NAME_LENGTH
		)
		TextInput(
			modifier = Modifier.fillMaxWidth(),
			state = model.forgotPasswordPwd,
			hint = "请输入新密码",
			inputType = InputType.PASSWORD,
			maxLength = UserConstraint.MAX_PWD_LENGTH
		)
		Text(
			text = "返回登录",
			color = MaterialTheme.colorScheme.primary,
			modifier = Modifier.align(Alignment.End).clickable {
				model.mode = Mode.Login
				model.loginId.text = ""
				model.loginPwd.text = ""
			}
		)
		Button(
			modifier = Modifier.fillMaxWidth(),
			enabled = model.canForgotPassword,
			onClick = { model.forgotPassword() }
		) {
			Text(text = "提交申请")
		}
	}
}

@Composable
private fun ContentBox(
	model: LoginModel,
	modifier: Modifier = Modifier
) {
	AnimatedContent(
		targetState = model.mode,
		modifier = modifier
	) { animatedMode ->
		when (animatedMode) {
			Mode.Login -> ContentLogin(
				model = model,
				modifier = Modifier.fillMaxSize()
			)
			Mode.Register -> ContentRegister(
				model = model,
				modifier = Modifier.fillMaxSize()
			)
			Mode.ForgotPassword -> ContentForgotPassword(
				model = model,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}

@Composable
private fun Portrait(
	model: LoginModel
) {
	Column(
		modifier = Modifier.fillMaxSize().padding(10.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Image(
			modifier = Modifier.size(120.dp),
			painter = painterResource(Res.drawable.img_logo),
			contentDescription = null
		)
		ContentBox(
			model = model,
			modifier = Modifier.fillMaxWidth().weight(1f)
		)
	}
}

@Composable
private fun Landscape(
	model: LoginModel
) {
	Row(
		modifier = Modifier.fillMaxSize().padding(10.dp),
		horizontalArrangement = Arrangement.spacedBy(20.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier.weight(1f).fillMaxHeight(),
			contentAlignment = Alignment.Center
		) {
			Image(
				modifier = Modifier.size(200.dp),
				painter = painterResource(Res.drawable.img_logo),
				contentDescription = null
			)
		}
		Box(
			modifier = Modifier.weight(2f).fillMaxHeight(),
			contentAlignment = Alignment.Center
		) {
			ContentBox(
				model = model,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}

@Composable
fun ScreenLogin(model: AppModel) {
	val screenModel = screen { LoginModel(model) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = when (screenModel.mode) {
			Mode.Login -> "登录"
			Mode.Register -> "注册"
			Mode.ForgotPassword -> "忘记密码"
		},
		onBack = { model.pop() }
	) {
		if (app.isPortrait) Portrait(model = screenModel)
		else Landscape(model = screenModel)
	}

	if (screenModel.loadingState.isOpen) {
		DialogLoading(state = screenModel.loadingState)
	}

	Tip(state = screenModel.tip)
}
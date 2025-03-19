package love.yinlin.ui.screen.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.UserConstraint
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.button.LoadingButton
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import org.jetbrains.compose.resources.painterResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.img_logo

@Stable
@Serializable
data object ScreenLogin : Screen<ScreenLogin.Model> {
	enum class Mode {
		Login,
		Register,
		ForgotPassword
	}

	class Model(model: AppModel) : Screen.Model(model) {
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

		suspend fun login() {
			val id = loginId.text
			val pwd = loginPwd.text
			if (!UserConstraint.checkName(id) || !UserConstraint.checkPassword(pwd)) {
				tip.error("ID或密码不合规范")
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
				is Data.Error -> {
					tip.error(result1.message)
				}
			}
		}

		suspend fun register() {
			TODO()
		}

		suspend fun forgotPassword() {
			TODO()
		}

		@Composable
		private fun ContentLogin(modifier: Modifier = Modifier) {
			Column(
				modifier = modifier,
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = loginId,
					hint = "请输入ID",
					maxLength = UserConstraint.MAX_NAME_LENGTH
				)
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = loginPwd,
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
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = registerId,
					hint = "请输入注册昵称",
					maxLength = UserConstraint.MAX_NAME_LENGTH
				)
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = registerPwd,
					hint = "请输入密码",
					inputType = InputType.PASSWORD,
					maxLength = UserConstraint.MAX_PWD_LENGTH
				)
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = registerPwd2,
					hint = "请再确认一次密码",
					inputType = InputType.PASSWORD,
					maxLength = UserConstraint.MAX_PWD_LENGTH
				)
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = registerInviter,
					hint = "请输入邀请人昵称",
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
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = forgotPasswordId,
					hint = "请输入昵称",
					maxLength = UserConstraint.MAX_NAME_LENGTH
				)
				TextInput(
					modifier = Modifier.fillMaxWidth(),
					state = forgotPasswordPwd,
					hint = "请输入新密码",
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
		fun Portrait() {
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
				ContentBox(modifier = Modifier.fillMaxWidth().weight(1f))
			}
		}

		@Composable
		fun Landscape() {
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
					ContentBox(modifier = Modifier.fillMaxSize())
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model)

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = when (model.mode) {
				Mode.Login -> "登录"
				Mode.Register -> "注册"
				Mode.ForgotPassword -> "忘记密码"
			},
			onBack = { model.pop() },
			tip = model.tip
		) {
			if (app.isPortrait) model.Portrait()
			else model.Landscape()
		}
	}
}
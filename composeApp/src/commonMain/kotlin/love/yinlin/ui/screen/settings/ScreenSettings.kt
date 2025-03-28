package love.yinlin.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Colors
import love.yinlin.common.ThemeColor
import love.yinlin.data.Data
import love.yinlin.data.rachel.UserProfile
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.NoImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.colorfulImageVector
import love.yinlin.ui.component.screen.SubScreen
import org.jetbrains.compose.resources.stringResource
import love.yinlin.resources.*

@Stable
@Serializable
data object ScreenSettings : Screen<ScreenSettings.Model> {
	class Model(model: AppModel) : Screen.Model(model) {
		fun logoff(token: String) {
			launch {
				val result = ClientAPI.request(
					route = API.User.Account.Logoff,
					data = token
				)
				if (result is Data.Success) {
					app.config.userToken = ""
					app.config.userProfile = null
				}
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
				Item(
					title = "头像",
					onClick = {}
				) {
					if (userProfile == null) NoImage()
					else WebImage(
						uri = userProfile.avatarPath,
						key = app.config.cacheUserAvatar,
						contentScale = ContentScale.Crop,
						circle = true,
						modifier = Modifier.size(48.dp).shadow(5.dp, CircleShape)
					)
				}
				ItemText(
					title = "ID",
					text = userProfile?.name ?: stringResource(Res.string.default_name),
					onClick = {}
				)
				ItemText(
					title = "个性签名",
					text = userProfile?.signature ?: stringResource(Res.string.default_signature),
					maxLines = 2,
					onClick = {}
				)
				Item(
					title = "背景墙",
					onClick = {}
				) {
					if (userProfile == null) NoImage(width = 96.dp, height = 54.dp)
					else WebImage(
						uri = userProfile.wallPath,
						key = app.config.cacheUserWall,
						modifier = Modifier.width(96.dp).height(54.dp).shadow(5.dp)
					)
				}
				ItemText(
					title = "邀请人",
					text = userProfile?.inviterName ?: ""
				)
				ItemExpander(
					title = "退出登录",
					icon = colorfulImageVector(icon = Icons.AutoMirrored.Outlined.Logout, background = ThemeColor.warning),
					color = ThemeColor.warning,
					hasDivider = false,
					onClick = {
						val token = app.config.userToken
						if (token.isNotEmpty()) logoff(token)
					}
				)
			}
		}

		@Composable
		private fun CommonSettings(modifier: Modifier = Modifier) {
			SettingsLayout(
				modifier = modifier,
				title = "系统",
				icon = Icons.Outlined.Info
			) {
				ItemTextExpander(
					title = "清理缓存",
					icon = colorfulImageVector(icon = Icons.Outlined.DeleteSweep, background = Colors.Red4),
					text = "0KB",
					onClick = {}
				)
				ItemExpander(
					title = "崩溃日志",
					icon = colorfulImageVector(icon = Icons.Outlined.Description, background = Colors.Yellow4),
					onClick = {}
				)
				ItemTextExpander(
					title = "检查更新",
					icon = colorfulImageVector(icon = Icons.Outlined.RocketLaunch, background = Colors.Yellow4),
					text = "3.0.0",
					onClick = {}
				)
				ItemExpander(
					title = "反馈与建议",
					icon = colorfulImageVector(icon = Icons.Outlined.Draw, background = Colors.Green4),
					onClick = {}
				)
				ItemExpander(
					title = "隐私政策",
					icon = colorfulImageVector(icon = Icons.Outlined.VerifiedUser, background = Colors.Yellow4),
					onClick = {}
				)
				ItemExpander(
					title = "关于茶舍",
					icon = colorfulImageVector(icon = Icons.Outlined.Face, background = Colors.Green4),
					hasDivider = false,
					onClick = {}
				)
			}
		}

		@Composable
		fun Portrait(userProfile: UserProfile?) {
			Column(
				modifier = Modifier.fillMaxSize().padding(vertical = 5.dp)
					.verticalScroll(rememberScrollState())
			) {
				AccountSettings(
					userProfile = userProfile,
					modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
				)
				CommonSettings(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp))
			}
		}

		@Composable
		fun Landscape(userProfile: UserProfile?) {
			Row(
				modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
			) {
				AccountSettings(
					userProfile = userProfile,
					modifier = Modifier.weight(1f).fillMaxHeight()
						.padding(horizontal = 5.dp, vertical = 10.dp)
				)
				CommonSettings(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 5.dp, vertical = 10.dp))
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model)

	@Composable
	override fun content(model: Model) {
		val userProfile = app.config.userProfile

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "设置",
			onBack = { model.pop() },
			slot = model.slot
		) {
			if (app.isPortrait) model.Portrait(userProfile)
			else model.Landscape(userProfile)
		}
	}
}
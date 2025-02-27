package love.yinlin.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.data.rachel.UserProfile
import love.yinlin.platform.app
import love.yinlin.platform.config
import love.yinlin.ui.component.image.NoImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.screen.SubScreen
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.default_name
import ylcs_kmp.composeapp.generated.resources.default_signature

private class SettingsModel : ViewModel() {

}

@Composable
private fun AccountSettings(
	modifier: Modifier = Modifier,
	userProfile: UserProfile?
) {
	SettingsLayout(
		modifier = modifier,
		title = "账号",
		icon = Icons.Filled.AccountCircle
	) {
		Item(
			title = "头像",
			onClick = {}
		) {
			if (userProfile == null) NoImage()
			else WebImage(
				uri = userProfile.avatarPath,
				key = config.cacheUserAvatar,
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
				key = config.cacheUserWall,
				modifier = Modifier.width(96.dp).height(54.dp).shadow(5.dp)
			)
		}
		ItemText(
			title = "邀请人",
			text = userProfile?.inviterName ?: ""
		)
	}
}

@Composable
private fun Portrait(
	model: SettingsModel,
	userProfile: UserProfile?
) {
	Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
		AccountSettings(
			modifier = Modifier.fillMaxWidth(),
			userProfile = userProfile
		)
	}
}

@Composable
private fun Landscape(
	model: SettingsModel,
	userProfile: UserProfile?
) {

}

@Composable
fun ScreenSettings(model: AppModel) {
	val screenModel = viewModel { SettingsModel() }
	val userProfile = config.userProfile

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "设置",
		onBack = { model.pop() }
	) {
		if (app.isPortrait) Portrait(screenModel, userProfile)
		else Landscape(screenModel, userProfile)
	}
}
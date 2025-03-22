package love.yinlin.ui.screen.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.data.rachel.UserProfile
import love.yinlin.platform.app
import love.yinlin.ui.component.input.LoadingButton
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.settings.ScreenSettings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import love.yinlin.resources.*

@Composable
private fun TipButtonContainer(
	title: String,
	buttons: List<TipButtonInfo>,
	modifier: Modifier = Modifier,
	shape: Shape = RectangleShape
) {
	Surface(
		modifier = modifier,
		shape = shape,
		shadowElevation = 5.dp
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(10.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleLarge
			)
			Row(
				modifier = modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				for (button in buttons) {
					TipButton(
						info = button,
						modifier = Modifier.weight(1f)
					)
				}
			}
		}
	}
}

class ScreenPartMe(model: AppModel) : ScreenPart(model) {
	fun scanQrcode() {

	}

	fun gotoLogin() {
		navigate(ScreenLogin)
	}

	fun gotoSettings() {
		navigate(ScreenSettings)
	}

	@Composable
	private fun ToolBar(modifier: Modifier = Modifier) {
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
			verticalAlignment = Alignment.CenterVertically
		) {
			ClickIcon(
				imageVector = Icons.Filled.CropFree,
				onClick = { scanQrcode() }
			)
			ClickIcon(
				imageVector = Icons.Filled.Settings,
				onClick = { gotoSettings() }
			)
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
			title = "个人空间",
			buttons = listOf(
				TipButtonInfo("签到", Icons.Filled.EventAvailable) { },
				TipButtonInfo("好友", Icons.Filled.Group) { },
				TipButtonInfo("主题", Icons.AutoMirrored.Filled.Article) {
					app.config.userProfile?.let { navigate(ScreenUserCard(it.uid)) }
				},
				TipButtonInfo("邮箱", Icons.Filled.Mail) {
					if (app.config.userToken.isNotEmpty()) navigate(ScreenMail)
				},
				TipButtonInfo("徽章", Icons.Filled.MilitaryTech) { },
			)
		)
	}

	@Composable
	private fun LoginBox(modifier: Modifier = Modifier) {
		Column(modifier = modifier) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(10.dp),
				horizontalArrangement = Arrangement.End
			) {
				ClickIcon(
					imageVector = Icons.Filled.Settings,
					onClick = { gotoSettings() }
				)
			}
			Box(
				modifier = Modifier.fillMaxWidth().weight(1f),
				contentAlignment = Alignment.Center
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(20.dp)
				) {
					Image(
						modifier = Modifier.size(200.dp),
						painter = painterResource(Res.drawable.image_not_login),
						contentDescription = null
					)
					LoadingButton(
						text = stringResource(Res.string.login),
						onClick = { gotoLogin() }
					)
				}
			}
		}
	}

	@Composable
	private fun Portrait(userProfile: UserProfile) {
		Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
			PortraitUserProfileCard(
				profile = remember(userProfile) { userProfile.publicProfile },
				owner = true,
				toolbar = { ToolBar() }
			)
			Space(10.dp)
			UserSpaceContainer(
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	@Composable
	private fun Landscape(userProfile: UserProfile) {
		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = 5.dp
			) {
				ToolBar(modifier = Modifier.fillMaxWidth().padding(10.dp))
			}
			Row(modifier = Modifier.fillMaxWidth()) {
				LandscapeUserProfileCard(
					profile = remember(userProfile) { userProfile.publicProfile },
					owner = true,
					modifier = Modifier.width(450.dp).padding(10.dp)
				)
				Column(modifier = Modifier.weight(1f)) {
					UserSpaceContainer(
						modifier = Modifier.fillMaxWidth().padding(10.dp),
						shape = MaterialTheme.shapes.large
					)
				}
			}
		}
	}

	@Composable
	override fun content() {
		val userProfile = app.config.userProfile
		if (userProfile == null) LoginBox(Modifier.fillMaxSize())
		else {
			if (app.isPortrait) Portrait(userProfile)
			else Landscape(userProfile)
		}
	}
}
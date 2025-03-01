package love.yinlin.ui.screen.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.data.rachel.UserProfile
import love.yinlin.platform.app
import love.yinlin.platform.config
import love.yinlin.ui.Route
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.OffsetLayout
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.MainModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.image_not_login
import ylcs_kmp.composeapp.generated.resources.login

@Stable
private data class TipButtonInfo(
	val text: String,
	val icon: ImageVector,
	val onClick: () -> Unit
)

class MeModel(val mainModel: MainModel) {
	fun scanQrcode() {

	}

	fun gotoLogin() {
		mainModel.navigate(Route.Login)
	}

	fun gotoSettings() {
		mainModel.navigate(Route.Settings)
	}
}

@Composable
private fun PortraitValue(
	value: String,
	title: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(5.dp)
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.titleLarge
		)
		Text(
			text = title
		)
	}
}

@Composable
private fun ToolBar(
	model: MeModel,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
		verticalAlignment = Alignment.CenterVertically
	) {
		ClickIcon(
			imageVector = Icons.Filled.CropFree,
			onClick = { model.scanQrcode() }
		)
		ClickIcon(
			imageVector = Icons.Filled.Settings,
			onClick = { model.gotoSettings() }
		)
	}
}

@Composable
private fun TipButton(
	info: TipButtonInfo,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.clickable(onClick = info.onClick).padding(3.dp),
		verticalArrangement = Arrangement.spacedBy(3.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		MiniIcon(
			imageVector = info.icon,
			color = MaterialTheme.colorScheme.onSurface
		)
		Text(
			text = info.text,
			color = MaterialTheme.colorScheme.onSurface
		)
	}
}

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

@Composable
private fun UserSpaceContainer(
	model: MeModel,
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
			TipButtonInfo("主题", Icons.AutoMirrored.Filled.Article) { },
			TipButtonInfo("邮箱", Icons.Filled.Mail) {
				if (config.userToken.isNotEmpty()) model.mainModel.navigate(Route.Mail)
			},
			TipButtonInfo("徽章", Icons.Filled.MilitaryTech) { },
		)
	)
}

@Composable
private fun LoginBox(
	model: MeModel,
	modifier: Modifier = Modifier
) {
	Column(modifier = modifier) {
		Row(
			modifier = Modifier.fillMaxWidth().padding(10.dp),
			horizontalArrangement = Arrangement.End
		) {
			ClickIcon(
				imageVector = Icons.Filled.Settings,
				onClick = { model.gotoSettings() }
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
				Button(onClick = { model.gotoLogin() }) {
					Text(text = stringResource(Res.string.login))
				}
			}
		}
	}
}

@Composable
private fun Portrait(
	model: MeModel,
	userProfile: UserProfile
) {
	Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
		WebImage(
			uri = userProfile.wallPath,
			key = config.cacheUserWall,
			modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
		)
		Column(
			modifier = Modifier.fillMaxWidth()
				.shadow(elevation = 5.dp, clip = false)
				.background(MaterialTheme.colorScheme.surface)
				.padding(10.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				OffsetLayout(y = (-46).dp) {
					WebImage(
						uri = userProfile.avatarPath,
						key = config.cacheUserAvatar,
						contentScale = ContentScale.Crop,
						circle = true,
						modifier = Modifier.size(72.dp).shadow(5.dp, CircleShape)
					)
				}
				Text(
					text = userProfile.name,
					style = MaterialTheme.typography.titleLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
				)
				ToolBar(model = model)
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				UserLabel(
					label = userProfile.label,
					level = userProfile.level
				)
				Row(
					modifier = Modifier.weight(1f),
					horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
					verticalAlignment = Alignment.CenterVertically
				) {
					PortraitValue(
						value = userProfile.level.toString(),
						title = "等级"
					)
					PortraitValue(
						value = userProfile.coin.toString(),
						title = "银币"
					)
				}
			}
			Text(
				text = userProfile.signature,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
		}
		Space(10.dp)
		UserSpaceContainer(
			model = model,
			modifier = Modifier.fillMaxWidth()
		)
	}
}

@Composable
private fun Landscape(
	model: MeModel,
	userProfile: UserProfile
) {
	Column(modifier = Modifier.fillMaxSize()) {
		Surface(
			modifier = Modifier.fillMaxWidth(),
			shadowElevation = 5.dp
		) {
			ToolBar(
				model = model,
				modifier = Modifier.fillMaxWidth().padding(10.dp)
			)
		}
		Row(modifier = Modifier.fillMaxWidth()) {
			Surface(
				modifier = Modifier.width(450.dp).padding(10.dp),
				shape = MaterialTheme.shapes.large,
				shadowElevation = 5.dp
			) {
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					WebImage(
						uri = userProfile.wallPath,
						key = config.cacheUserWall,
						modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
					)
					Row(
						modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 10.dp),
						horizontalArrangement = Arrangement.spacedBy(15.dp)
					) {
						Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
							WebImage(
								uri = userProfile.avatarPath,
								key = config.cacheUserAvatar,
								contentScale = ContentScale.Crop,
								circle = true,
								modifier = Modifier.matchParentSize().shadow(5.dp, CircleShape)
							)
						}
						Column(
							modifier = Modifier.weight(1f),
							verticalArrangement = Arrangement.spacedBy(5.dp)
						) {
							Text(
								text = userProfile.name,
								style = MaterialTheme.typography.titleLarge,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier.fillMaxWidth()
							)
							UserLabel(
								label = userProfile.label,
								level = userProfile.level
							)
						}
						Row(
							horizontalArrangement = Arrangement.spacedBy(10.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							PortraitValue(
								value = userProfile.level.toString(),
								title = "等级"
							)
							PortraitValue(
								value = userProfile.coin.toString(),
								title = "银币"
							)
						}
					}
					Text(
						text = userProfile.signature,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
					)
				}
			}
			Column(modifier = Modifier.weight(1f)) {
				UserSpaceContainer(
					model = model,
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					shape = MaterialTheme.shapes.large
				)
			}
		}
	}
}

@Composable
fun ScreenMe(model: MeModel) {
	val userProfile = config.userProfile
	if (userProfile == null) LoginBox(model, Modifier.fillMaxSize())
	else {
		if (app.isPortrait) Portrait(model, userProfile)
		else Landscape(model, userProfile)
	}
}
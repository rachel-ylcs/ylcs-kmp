package love.yinlin.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.IndeterminateCheckBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Scheme
import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.data.Failed
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.DateEx
import love.yinlin.extension.rememberState
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_not_login
import love.yinlin.resources.login
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.screen.settings.ScreenSettings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.BarcodeFormats
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Stable
class ScreenPartMe(model: AppModel) : ScreenPart(model) {
	val signinSheet = FloatingSheet()
	val scanSheet = FloatingSheet()

	fun logoff() {
		app.config.userToken = ""
		app.config.userProfile = null
	}

	suspend fun updateUserToken() {
		val token = app.config.userToken
		if (token.isNotEmpty()) {
			val result = ClientAPI.request(
				route = API.User.Account.UpdateToken,
				data = token
			)
			when (result) {
				is Data.Success -> app.config.userToken = result.data
				is Data.Error -> {
					if (result.type == Failed.RequestError.Unauthorized) {
						logoff()
						navigate(ScreenLogin.Args)
					}
				}
			}
		}
	}

	@Composable
	private fun ToolBar(modifier: Modifier = Modifier) {
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
			verticalAlignment = Alignment.CenterVertically
		) {
			ClickIcon(
				icon = Icons.Filled.CropFree,
				onClick = {
					scanSheet.open()
				}
			)
			ClickIcon(
				icon = Icons.Filled.Settings,
				onClick = { navigate(ScreenSettings.Args) }
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
				TipButtonInfo("签到", Icons.Filled.EventAvailable) { signinSheet.open() },
				TipButtonInfo("好友", Icons.Filled.Group) { },
				TipButtonInfo("主题", Icons.AutoMirrored.Filled.Article) {
					app.config.userProfile?.let { navigate(ScreenUserCard.Args(it.uid)) }
				},
				TipButtonInfo("邮箱", Icons.Filled.Mail) {
					if (app.config.userToken.isNotEmpty()) navigate(ScreenMail.Args)
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
					icon = Icons.Filled.Settings,
					onClick = { navigate(ScreenSettings.Args) }
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
						painter = painterResource(Res.drawable.img_not_login),
						contentDescription = null
					)
					RachelButton(
						text = stringResource(Res.string.login),
						onClick = { navigate(ScreenLogin.Args) }
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
			UserSpaceContainer(modifier = Modifier.fillMaxWidth())
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
	override fun Content() {
		val userProfile = app.config.userProfile
		if (userProfile == null) LoginBox(Modifier.fillMaxSize())
		else {
			if (app.isPortrait) Portrait(userProfile)
			else Landscape(userProfile)
		}
	}

	@Composable
	override fun Floating() {
		signinSheet.Land {
			var data by rememberState { BooleanArray(8) { false } }
			var todayIndex: Int by rememberState { -1 }
			var todaySignin by rememberState { true }
			val today = remember { DateEx.Today }

			Column(
				modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 20.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(20.dp)
			) {
				Text(
					text = "签到记录",
					style = MaterialTheme.typography.titleLarge
				)
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					repeat(2) { row ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(10.dp)
						) {
							repeat(4) { col ->
								val index = row * 4 + col
								Surface(
									modifier = Modifier.weight(1f),
									shape = MaterialTheme.shapes.medium,
									tonalElevation = 3.dp,
									shadowElevation = 1.dp,
									border = if (index != todayIndex) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
								) {
									Column(
										modifier = Modifier.fillMaxWidth().padding(10.dp),
										horizontalAlignment = Alignment.CenterHorizontally,
										verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
									) {
										val date = today.minus(todayIndex - index, DateTimeUnit.DAY)

										MiniIcon(
											icon = if (data[index]) Icons.Outlined.Check else Icons.Outlined.IndeterminateCheckBox,
											color = if (index != todayIndex) LocalContentColor.current else MaterialTheme.colorScheme.primary
										)
										Text(
											text = "${date.monthNumber}月${date.dayOfMonth}日",
											color = if (index != todayIndex) LocalContentColor.current else MaterialTheme.colorScheme.primary
										)
									}
								}
							}
						}
					}
				}
				Text(text = if (todaySignin) "今日已签到" else "签到成功! 银币+1")
			}

			LaunchedEffect(Unit) {
				val result = ClientAPI.request(
					route = API.User.Profile.Signin,
					data = app.config.userToken
				)
				if (result is Data.Success) {
					with(result.data) {
						todaySignin = status
						todayIndex = index
						data = BooleanArray(8) { ((value shr it) and 1) == 1 }
					}
				}
			}
		}
		scanSheet.Land {
			ScannerView(
				modifier = Modifier.fillMaxWidth(),
				codeTypes = listOf(BarcodeFormats.FORMAT_QR_CODE),
				showUi = false,
				result = {
					scanSheet.close()
					if (it is BarcodeResult.OnSuccess) {
						try {
							val uri = Uri.parse(it.barcode.data)!!
							if (uri.scheme == Scheme.Rachel) deeplink(uri)
						}
						catch (_: Throwable) {
							slot.tip.warning("不能识别此信息")
						}
					}
				}
			)
		}
	}
}
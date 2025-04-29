package love.yinlin.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.brush
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.LocalOrientation
import love.yinlin.common.Orientation
import love.yinlin.common.Scheme
import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.data.Failed
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.DateEx
import love.yinlin.extension.rememberState
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_logo
import love.yinlin.resources.img_not_login
import love.yinlin.resources.login
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.component.screen.SheetConfig
import love.yinlin.ui.screen.settings.ScreenSettings
import love.yinlin.ui.screen.world.ScreenActivityLink
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.BarcodeFormats
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Stable
class ScreenPartMe(model: AppModel) : ScreenPart(model) {
	val scanSheet = FloatingSheet(SheetConfig(max = 0.9f, full = true))
	val userCardSheet = FloatingSheet()
	val signinSheet = FloatingSheet()

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
						navigate<ScreenLogin>()
					}
				}
			}
		}
	}

	@Composable
	private fun ToolContainer(
		modifier: Modifier = Modifier,
		shape: Shape = RectangleShape
	) {
		TipButtonContainer(
			modifier = modifier,
			shape = shape,
			title = "功能栏"
		) {
			Item("扫码", Icons.Filled.CropFree) { scanSheet.open() }
			Item("名片", Icons.Filled.AccountBox) {
				if (app.config.userToken.isNotEmpty()) userCardSheet.open()
			}
			Item("设置", Icons.Filled.Settings) { navigate<ScreenSettings>() }
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
			title = "个人空间"
		) {
			Item("签到", Icons.Filled.EventAvailable) { signinSheet.open() }
			Item("好友", Icons.Filled.Group) { }
			Item("主题", Icons.AutoMirrored.Filled.Article) {
				app.config.userProfile?.let { navigate(ScreenUserCard.Args(it.uid)) }
			}
			Item("邮箱", Icons.Filled.Mail) {
				if (app.config.userToken.isNotEmpty()) navigate<ScreenMail>()
			}
			Item("徽章", Icons.Filled.MilitaryTech) { }
		}
	}

	@Composable
	private fun AdminContainer(
		modifier: Modifier = Modifier,
		shape: Shape = RectangleShape
	) {
		TipButtonContainer(
			modifier = modifier,
			shape = shape,
			title = "超管空间"
		) {
			Item("活动", Icons.Filled.Link) {
				if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
					navigate<ScreenActivityLink>() // 250539
				}
			}
		}
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
					onClick = { navigate<ScreenSettings>() }
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
						onClick = { navigate<ScreenLogin>() }
					)
				}
			}
		}
	}

	@Composable
	private fun Portrait(userProfile: UserProfile) {
		Column(
			modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			PortraitUserProfileCard(
				modifier = Modifier.fillMaxWidth(),
				profile = remember(userProfile) { userProfile.publicProfile },
				owner = true
			)
			ToolContainer(modifier = Modifier.fillMaxWidth())
			UserSpaceContainer(modifier = Modifier.fillMaxWidth())
			AdminContainer(modifier = Modifier.fillMaxWidth())
			Space(10.dp)
		}
	}

	@Composable
	private fun Landscape(userProfile: UserProfile) {
		Row(modifier = Modifier.fillMaxSize()) {
			LandscapeUserProfileCard(
				profile = remember(userProfile) { userProfile.publicProfile },
				owner = true,
				modifier = Modifier.weight(1f).padding(20.dp)
			)
			Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
				ToolContainer(
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					shape = MaterialTheme.shapes.large
				)
				UserSpaceContainer(
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					shape = MaterialTheme.shapes.large
				)
				AdminContainer(
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					shape = MaterialTheme.shapes.large
				)
				Space(10.dp)
			}
		}
	}

	@Composable
	override fun Content() {
		val userProfile = app.config.userProfile
		if (userProfile == null) LoginBox(Modifier.fillMaxSize())
		else {
			when (LocalOrientation.current) {
				Orientation.PORTRAIT -> Portrait(userProfile = userProfile)
				Orientation.LANDSCAPE -> Landscape(userProfile = userProfile)
				Orientation.SQUARE -> {}
			}
		}
	}

	@Composable
	override fun Floating() {
		userCardSheet.Land {
			val profile = app.config.userProfile
			if (profile == null) EmptyBox()
			else {
				Column(
					modifier = Modifier.fillMaxWidth().padding(20.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(20.dp)
				) {
					WebImage(
						uri = profile.avatarPath,
						key = app.config.cacheUserWall,
						contentScale = ContentScale.Crop,
						circle = true,
						modifier = Modifier.size(100.dp).shadow(5.dp, CircleShape)
					)
					Text(
						text = profile.name,
						style = MaterialTheme.typography.displayMedium,
						color = MaterialTheme.colorScheme.primary
					)
					Text(
						text = "扫我添加好友",
						style = MaterialTheme.typography.headlineSmall
					)

					val primaryColor = MaterialTheme.colorScheme.primary
					val secondaryColor = MaterialTheme.colorScheme.secondary
					val logoPainter = painterResource(Res.drawable.img_logo)
					val qrcodePainter = rememberQrCodePainter(data = "rachel://yinlin.love/openProfile?uid=${profile.uid}") {
						logo {
							painter = logoPainter
							padding = QrLogoPadding.Natural(1f)
							shape = QrLogoShape.circle()
							size = 0.2f
						}
						shapes {
							ball = QrBallShape.circle()
							darkPixel = QrPixelShape.roundCorners()
							frame = QrFrameShape.roundCorners(0.25f)
						}
						colors {
							dark = QrBrush.brush {
								Brush.linearGradient(
									0f to primaryColor.copy(alpha = 0.9f),
									1f to secondaryColor.copy(0.9f),
									end = Offset(it, it)
								)
							}
							frame = QrBrush.solid(primaryColor)
						}
					}
					Image(
						painter = qrcodePainter,
						contentDescription = null,
						modifier = Modifier.size(180.dp)
					)
					Space(40.dp)
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
	}
}
package love.yinlin.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.IndeterminateCheckBox
import androidx.compose.material.icons.outlined.Paid
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
import androidx.compose.ui.text.style.TextOverflow
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.KVConfig
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.Scheme
import love.yinlin.common.ThemeValue
import love.yinlin.common.Uri
import love.yinlin.common.UriGenerator
import love.yinlin.data.Data
import love.yinlin.data.Failed
import love.yinlin.data.ItemKey
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.DateEx
import love.yinlin.extension.rememberState
import love.yinlin.extension.rememberTrue
import love.yinlin.extension.rememberValueState
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.platform.QrcodeScanner
import love.yinlin.ui.component.screen.FloatingArgsSheet
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.component.screen.SheetConfig
import love.yinlin.ui.screen.settings.ScreenSettings
import love.yinlin.ui.screen.world.ScreenActivityLink
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Composable
private fun LevelItem(
	index: Int,
	item: Pair<Int, Int>,
	modifier: Modifier = Modifier
) {
	val level = index + 1
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(level.toString())
		Box(
			modifier = Modifier.weight(1f),
			contentAlignment = Alignment.Center
		) {
			RachelText(
				text = remember(item) {
					if (item.second != Int.MAX_VALUE) "${item.first} ~ ${item.second}"
					else "> ${item.first}"
				},
				icon = Icons.Outlined.Paid
			)
		}
		Box(
			modifier = Modifier.offset(y = -ThemeValue.Padding.LittleSpace),
			contentAlignment = Alignment.Center
		) {
			UserLabel(
				label = "",
				level = level
			)
		}
	}
}

@Stable
class ScreenPartMe(model: AppModel) : ScreenPart(model) {
	private val scanSheet = FloatingSheet(SheetConfig(max = 0.9f, full = true))
	private val userCardSheet = FloatingArgsSheet<UserProfile>()
	private val signinSheet = FloatingArgsSheet<UserProfile>()
	private val levelSheet = FloatingArgsSheet<UserProfile>()

	@OptIn(ExperimentalAtomicApi::class)
    private val isUpdateToken = AtomicBoolean(false)

	fun cleanUserToken() {
		app.config.userToken = ""
		app.config.userProfile = null
		app.config.cacheUserAvatar = KVConfig.UPDATE
		app.config.cacheUserWall = KVConfig.UPDATE
	}

	@OptIn(ExperimentalAtomicApi::class)
	suspend fun updateUserToken() {
		val token = app.config.userToken
		if (token.isNotEmpty() && isUpdateToken.compareAndSet(expectedValue = false, newValue = true)) {
			val result = ClientAPI.request(
				route = API.User.Account.UpdateToken,
				data = token
			)
			isUpdateToken.store(false)
			when (result) {
				is Data.Success -> app.config.userToken = result.data
				is Data.Error -> {
					if (result.type == Failed.RequestError.Unauthorized) {
						cleanUserToken()
						navigate<ScreenLogin>()
					}
				}
			}
		}
	}

	@OptIn(ExperimentalAtomicApi::class)
	suspend fun updateUserProfile() {
		val token = app.config.userToken
		if (token.isNotEmpty() && !isUpdateToken.load()) {
			val result = ClientAPI.request(
				route = API.User.Profile.GetProfile,
				data = token
			)
			if (result is Data.Success) app.config.userProfile = result.data
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
				app.config.userProfile?.let {
					userCardSheet.open(it)
				} ?: slot.tip.warning("请先登录")
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
			Item("签到", Icons.Filled.EventAvailable) {
				app.config.userProfile?.let {
					signinSheet.open(it)
				} ?: slot.tip.warning("请先登录")
			}
			Item("主题", Icons.AutoMirrored.Filled.Article) {
				app.config.userProfile?.let {
					navigate(ScreenUserCard.Args(it.uid))
				} ?: slot.tip.warning("请先登录")
			}
			Item("邮箱", Icons.Filled.Mail) {
				app.config.userProfile?.let {
					navigate<ScreenMail>()
				} ?: slot.tip.warning("请先登录")
			}
			Item("徽章", Icons.Filled.MilitaryTech) { }
		}
	}

	@Composable
	private fun PromotionContainer(
		modifier: Modifier = Modifier,
		shape: Shape = RectangleShape
	) {
		TipButtonContainer(
			modifier = modifier,
			shape = shape,
			title = "推广"
		) {
			Item("水群", ExtraIcons.QQ) {
				launch {
					if (!OS.Application.startAppIntent(UriGenerator.qqGroup("828049503"))) slot.tip.warning("未安装QQ")
				}
			}
			Item("店铺", ExtraIcons.Taobao) {
				launch {
					if (!OS.Application.startAppIntent(UriGenerator.taobao("280201975"))) slot.tip.warning("未安装淘宝")
				}
			}
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
				navigate<ScreenActivityLink>()
			}
		}
	}

	@Composable
	private fun LoginBox(modifier: Modifier = Modifier) {
		Column(modifier = modifier) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(vertical = ThemeValue.Padding.VerticalSpace),
				horizontalArrangement = Arrangement.End
			) {
				ActionScope.Right.Actions {
					Action(Icons.Filled.Settings) {
						navigate<ScreenSettings>()
					}
				}
			}
			Box(
				modifier = Modifier.fillMaxWidth().weight(1f),
				contentAlignment = Alignment.Center
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
				) {
					MiniIcon(
						res = Res.drawable.img_not_login,
						size = ThemeValue.Size.ExtraLargeImage
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
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
		) {
			UserProfileCard(
				modifier = Modifier.fillMaxWidth(),
				profile = remember(userProfile) { userProfile.publicProfile },
				owner = true,
				onLevelClick = { levelSheet.open(userProfile) },
				onFollowClick = { navigate(ScreenFollows.Args(it)) }
			)
			ToolContainer(modifier = Modifier.fillMaxWidth())
			UserSpaceContainer(modifier = Modifier.fillMaxWidth())
			PromotionContainer(modifier = Modifier.fillMaxWidth())
			if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
				AdminContainer(modifier = Modifier.fillMaxWidth())
			}
			Space()
		}
	}

	@Composable
	private fun Landscape(userProfile: UserProfile) {
		Row(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
			UserProfileCard(
				profile = remember(userProfile) { userProfile.publicProfile },
				owner = true,
				shape = MaterialTheme.shapes.large,
				modifier = Modifier.weight(1f).padding(ThemeValue.Padding.EqualExtraValue),
				onLevelClick = { levelSheet.open(userProfile) },
				onFollowClick = { navigate(ScreenFollows.Args(it)) }
			)
			Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
				ToolContainer(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraValue),
					shape = MaterialTheme.shapes.large
				)
				UserSpaceContainer(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraValue),
					shape = MaterialTheme.shapes.large
				)
				PromotionContainer(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraValue),
					shape = MaterialTheme.shapes.large
				)
				if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
					AdminContainer(
						modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraValue),
						shape = MaterialTheme.shapes.large
					)
				}
				Space()
			}
		}
	}

    @OptIn(ExperimentalAtomicApi::class)
    @Composable
	override fun Content() {
		val userProfile = app.config.userProfile
		if (userProfile == null) LoginBox(Modifier.fillMaxSize().padding(LocalImmersivePadding.current))
		else {
			when (LocalDevice.current.type) {
				Device.Type.PORTRAIT -> Portrait(userProfile = userProfile)
				Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(userProfile = userProfile)
			}
		}

		LaunchedEffect(Unit) {
			updateUserProfile()
		}
	}

	@Composable
	override fun Floating() {
		userCardSheet.Land { profile ->
			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
			) {
				WebImage(
					uri = profile.avatarPath,
					key = app.config.cacheUserWall,
					contentScale = ContentScale.Crop,
					circle = true,
					modifier = Modifier.size(ThemeValue.Size.LargeImage)
						.shadow(ThemeValue.Shadow.Icon, CircleShape)
				)
				Text(
					text = profile.name,
					style = MaterialTheme.typography.displaySmall,
					color = MaterialTheme.colorScheme.primary
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
				MiniImage(
					painter = qrcodePainter,
					modifier = Modifier.size(ThemeValue.Size.ExtraLargeImage)
				)
				Space()
			}
		}

		scanSheet.Land {
			QrcodeScanner(
				modifier = Modifier.fillMaxWidth(),
				onResult = { result ->
					try {
						val uri = Uri.parse(result)!!
						if (uri.scheme == Scheme.Rachel) deeplink(uri)
					}
					catch (_: Throwable) {
						slot.tip.warning("不能识别此信息")
					}
					scanSheet.close()
				}
			)
		}

		signinSheet.Land { profile ->
			var data by rememberState { BooleanArray(8) { false } }
			var todayIndex by rememberValueState(-1)
			var todaySignin by rememberTrue()
			val today = remember { DateEx.Today }

			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
			) {
				Text(
					text = "签到记录",
					style = MaterialTheme.typography.titleLarge
				)
				Column(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
				) {
					repeat(2) { row ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
						) {
							repeat(4) { col ->
								val index = row * 4 + col
								Surface(
									modifier = Modifier.weight(1f),
									shape = MaterialTheme.shapes.medium,
									tonalElevation = ThemeValue.Shadow.Tonal,
									shadowElevation = ThemeValue.Shadow.Item,
									border = if (index != todayIndex) null else BorderStroke(ThemeValue.Border.Small, MaterialTheme.colorScheme.primary)
								) {
									Column(
										modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
										horizontalAlignment = Alignment.CenterHorizontally,
										verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace, Alignment.CenterVertically)
									) {
										val date = today.minus(todayIndex - index, DateTimeUnit.DAY)

										MiniIcon(
											icon = if (data[index]) Icons.Outlined.Check else Icons.Outlined.IndeterminateCheckBox,
											color = if (index != todayIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
										)
										Text(
											text = "${date.monthNumber}月${date.dayOfMonth}日",
											color = if (index != todayIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
											maxLines = 1,
											overflow = TextOverflow.Ellipsis
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

		levelSheet.Land { profile ->
			LazyColumn(modifier = Modifier.fillMaxWidth()) {
				item(key = ItemKey("Profile")) {
					UserProfileInfo(
						profile = remember(profile) { profile.publicProfile },
						owner = true,
						modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue)
					) { onLevelClick ->
						Row(
							horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
							verticalAlignment = Alignment.CenterVertically
						) {
							PortraitValue(
								value = profile.level.toString(),
								title = "等级",
								modifier = Modifier.clickableNoRipple(onClick = onLevelClick)
							)
							PortraitValue(
								value = profile.coin.toString(),
								title = "银币"
							)
						}
					}
				}
				itemsIndexed(
					items = UserLevel.levelTable,
					key = { index, item -> index }
				) { index, item ->
					LevelItem(
						index = index,
						item = item,
						modifier = Modifier.fillMaxWidth().clickable {}.padding(ThemeValue.Padding.Value)
					)
				}
			}
		}
	}
}
package love.yinlin.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.common.Colors
import love.yinlin.common.KVConfig
import love.yinlin.common.ThemeColor
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.itemKey
import love.yinlin.extension.rememberState
import love.yinlin.platform.Coroutines
import love.yinlin.platform.AppContext
import love.yinlin.platform.ImageCompress
import love.yinlin.platform.ImageCrop
import love.yinlin.platform.ImageProcessor
import love.yinlin.platform.ImageQuality
import love.yinlin.platform.OS
import love.yinlin.platform.PicturePicker
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.app_privacy_policy
import love.yinlin.resources.default_name
import love.yinlin.resources.default_signature
import love.yinlin.ui.component.image.DialogCrop
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.image.NoImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.colorfulImageVector
import love.yinlin.ui.component.screen.BottomSheet
import love.yinlin.ui.component.screen.CommonSheetState
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.community.ScreenPartMe
import org.jetbrains.compose.resources.stringResource

@Stable
@Serializable
data object ScreenSettings : Screen<ScreenSettings.Model> {
	class Model(model: AppModel) : Screen.Model(model) {
		val crashLogSheet = CommonSheetState()
		val feedbackSheet = CommonSheetState()
		val privacyPolicySheet = CommonSheetState()
		val aboutSheet = CommonSheetState()

		val cropDialog = DialogCrop()

		val idModifyDialog = DialogInput(
			hint = "修改ID(消耗${UserConstraint.RENAME_COIN_COST}银币)",
			maxLength = UserConstraint.MAX_NAME_LENGTH
		)

		val signatureModifyDialog = DialogInput(
			hint = "修改个性签名",
			maxLength = UserConstraint.MAX_SIGNATURE_LENGTH,
			maxLines = 3,
			clearButton = false
		)

		private suspend fun pickPicture(aspectRatio: Float): Path? {
			return PicturePicker.pick()?.use { source ->
				OS.Storage.createTempFile { sink -> source.transferTo(sink) > 0L }
			}?.let { path ->
				cropDialog.open(url = path.toString(), aspectRatio = aspectRatio)?.let { rect ->
					OS.Storage.createTempFile { sink ->
						SystemFileSystem.source(path).buffered().use { source ->
							ImageProcessor(ImageCrop(rect), ImageCompress, quality = ImageQuality.High).process(source, sink)
						}
					}
				}
			}
		}

		suspend fun modifyUserAvatar() {
			pickPicture(1f)?.let { path ->
				val result = ClientAPI.request(
					route = API.User.Profile.UpdateAvatar,
					data = app.config.userToken,
					files = {
						API.User.Profile.UpdateAvatar.Files(
							avatar = file(SystemFileSystem.source(path))
						)
					}
				)
				when (result) {
					is Data.Success -> app.config.cacheUserAvatar = KVConfig.UPDATE
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}

		suspend fun modifyUserWall() {
			pickPicture(1.77777f)?.let { path ->
				val result = ClientAPI.request(
					route = API.User.Profile.UpdateWall,
					data = app.config.userToken,
					files = {
						API.User.Profile.UpdateWall.Files(
							wall = file(SystemFileSystem.source(path))
						)
					}
				)
				when (result) {
					is Data.Success -> app.config.cacheUserWall = KVConfig.UPDATE
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}

		suspend fun modifyUserId(initText: String) {
			idModifyDialog.open(initText)?.let { text ->
				val profile = app.config.userProfile
				if (profile != null) launch {
					val result = ClientAPI.request(
						route = API.User.Profile.UpdateName,
						data = API.User.Profile.UpdateName.Request(
							token = app.config.userToken,
							name = text
						)
					)
					when (result) {
						is Data.Success -> {
							app.config.userProfile = profile.copy(
								name = text,
								coin = profile.coin - UserConstraint.RENAME_COIN_COST
							)
						}
						is Data.Error -> slot.tip.error(result.message)
					}
				}
			}
		}

		suspend fun modifyUserSignature(initText: String) {
			signatureModifyDialog.open(initText)?.let { text ->
				val profile = app.config.userProfile
				if (profile != null) launch {
					val result = ClientAPI.request(
						route = API.User.Profile.UpdateSignature,
						data = API.User.Profile.UpdateSignature.Request(
							token = app.config.userToken,
							signature = text
						)
					)
					when (result) {
						is Data.Success -> {
							app.config.userProfile = profile.copy(
								signature = text
							)
						}
						is Data.Error -> slot.tip.error(result.message)
					}
				}
			}
		}

		suspend fun logoff() {
			val token = app.config.userToken
			if (token.isNotEmpty()) {
				ClientAPI.request(
					route = API.User.Account.Logoff,
					data = token
				)
				// 不论是否成功均从本地设备退出登录
				part<ScreenPartMe>().logoff()
			}
		}

		suspend fun clearCache(): String = Coroutines.io {
			OS.Storage.clearCache()
			OS.Storage.cacheSize.fileSizeString
		}

		suspend fun sendFeedback(content: String) {
			val result = ClientAPI.request(
				route = API.User.Info.SendFeedback,
				data = API.User.Info.SendFeedback.Request(
					token = app.config.userToken,
					content = content
				)
			)
			when (result) {
				is Data.Success -> feedbackSheet.hide()
				is Data.Error -> slot.tip.error(result.message)
			}
		}

		suspend fun checkUpdate() {
			val result = ClientAPI.request<ServerStatus>(route = ServerRes.Server)
			when (result) {
				is Data.Success -> {
					val data = result.data
					if (data.targetVersion > Local.VERSION) slot.tip.warning("新版本${data.targetVersion}可用")
					else if (data.minVersion > Local.VERSION) slot.tip.error("当前版本不满足服务器最低兼容版本${data.minVersion}")
					else slot.tip.success("当前已是最新版本")
				}
				is Data.Error -> slot.tip.error(result.message)
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
					onClick = {
						if (userProfile != null) launch { modifyUserAvatar() }
					}
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
					onClick = {
						if (userProfile != null) launch { modifyUserId(userProfile.name) }
					}
				)
				ItemText(
					title = "个性签名",
					text = userProfile?.signature ?: stringResource(Res.string.default_signature),
					maxLines = 2,
					onClick = {
						if (userProfile != null) launch { modifyUserSignature(userProfile.signature) }
					}
				)
				Item(
					title = "背景墙",
					onClick = {
						if (userProfile != null) launch { modifyUserWall() }
					}
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
				ItemExpanderSuspend(
					title = "退出登录",
					icon = colorfulImageVector(icon = Icons.AutoMirrored.Outlined.Logout, background = ThemeColor.warning),
					color = ThemeColor.warning,
					hasDivider = false,
					onClick = { logoff() }
				)
			}
		}

		@Composable
		fun CrashLogLayout() {
			val text = remember {
				app.kv.get(AppContext.CRASH_KEY, "无崩溃日志")
			}
			BottomSheet(state = crashLogSheet) {
				Box(
					modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)
						.padding(10.dp)
						.verticalScroll(rememberScrollState())
				) {
					Text(
						text = text,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		@Composable
		fun FeedbackLayout() {
			val state = remember { TextInputState() }

			BottomSheet(state = feedbackSheet) {
				Column(
					modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
						.padding(10.dp),
					horizontalAlignment = Alignment.End,
					verticalArrangement = Arrangement.spacedBy(10.dp),
				) {
					LoadingIcon(
						icon = Icons.Outlined.Check,
						enabled = state.ok,
						onClick = { sendFeedback(state.text) }
					)
					TextInput(
						state = state,
						hint = "反馈与建议",
						maxLength = 512,
						maxLines = 5,
						clearButton = false,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		@Composable
		fun PrivacyPolicyLayout() {
			BottomSheet(state = privacyPolicySheet) {
				Box(
					modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp)
						.padding(10.dp)
						.verticalScroll(rememberScrollState())
				) {
					Text(
						text = stringResource(Res.string.app_privacy_policy),
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		@Composable
		fun AboutLayout() {
			BottomSheet(state = aboutSheet) {
				Box(
					modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
						.padding(10.dp)
				) {
					Text(text = "${Local.NAME} ${Local.VERSION_NAME}")
				}
			}
		}

		@Composable
		private fun CommonSettings(modifier: Modifier = Modifier) {
			SettingsLayout(
				modifier = modifier,
				title = "系统",
				icon = Icons.Outlined.Info
			) {
				var cacheSizeText by rememberState { OS.Storage.cacheSize.fileSizeString }
				ItemExpanderSuspend(
					title = "清理缓存",
					icon = colorfulImageVector(icon = Icons.Outlined.DeleteSweep, background = Colors.Red4),
					text = cacheSizeText,
					onClick = { cacheSizeText = clearCache() }
				)

				ItemExpander(
					title = "崩溃日志",
					icon = colorfulImageVector(icon = Icons.Outlined.Description, background = Colors.Yellow4),
					onClick = { crashLogSheet.open() }
				)

				ItemExpanderSuspend(
					title = "检查更新",
					icon = colorfulImageVector(icon = Icons.Outlined.RocketLaunch, background = Colors.Yellow4),
					text = "3.0.0",
					onClick = { checkUpdate() }
				)

				ItemExpander(
					title = "反馈与建议",
					icon = colorfulImageVector(icon = Icons.Outlined.Draw, background = Colors.Green4),
					onClick = { feedbackSheet.open() }
				)

				ItemExpander(
					title = "隐私政策",
					icon = colorfulImageVector(icon = Icons.Outlined.VerifiedUser, background = Colors.Yellow4),
					onClick = { privacyPolicySheet.open() }
				)

				ItemExpander(
					title = "关于茶舍",
					icon = colorfulImageVector(icon = Icons.Outlined.Face, background = Colors.Green4),
					hasDivider = false,
					onClick = { aboutSheet.open() }
				)
			}
		}

		@Composable
		fun Portrait(userProfile: UserProfile?) {
			Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
				AccountSettings(
					userProfile = userProfile,
					modifier = Modifier.fillMaxWidth().padding(10.dp)
				)
				CommonSettings(
					modifier = Modifier.fillMaxWidth().padding(10.dp)
				)
			}
		}

		@Composable
		fun Landscape(userProfile: UserProfile?) {
			LazyVerticalStaggeredGrid(
				columns = StaggeredGridCells.Fixed(3),
				modifier = Modifier.fillMaxSize(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalItemSpacing = 10.dp
			) {
				item(key = "AccountSettings".itemKey) {
					AccountSettings(
						userProfile = userProfile,
						modifier = Modifier.fillMaxWidth().padding(10.dp)
					)
				}
				item(key = "CommonSettings".itemKey) {
					CommonSettings(
						modifier = Modifier.fillMaxWidth().padding(10.dp)
					)
				}
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

		model.crashLogSheet.withOpen {
			model.CrashLogLayout()
		}

		model.feedbackSheet.withOpen {
			model.FeedbackLayout()
		}

		model.privacyPolicySheet.withOpen {
			model.PrivacyPolicyLayout()
		}

		model.aboutSheet.withOpen {
			model.AboutLayout()
		}

		model.cropDialog.withOpen()
		model.idModifyDialog.withOpen()
		model.signatureModifyDialog.withOpen()
	}
}
package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastMap
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.app
import love.yinlin.ui.component.image.LoadingCircle
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.rememberTextInputState

@Composable
private fun WeiboUserItem(
	user: WeiboUserInfo,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Row(
		modifier = modifier.clickable(onClick = onClick).padding(ThemeValue.Padding.Value),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace)
	) {
		if (user.avatar.isEmpty()) {
			LoadingCircle(size = ThemeValue.Size.MicroImage)
		}
		else {
			WebImage(
				uri = user.avatar,
				key = remember { DateEx.TodayString },
				contentScale = ContentScale.Crop,
				circle = true,
				modifier = Modifier.size(ThemeValue.Size.MicroImage)
			)
		}
		Text(
			text = user.name,
			overflow = Ellipsis,
			modifier = Modifier.weight(1f)
		)
	}
}

@Stable
class ScreenWeiboFollows(model: AppModel) : CommonSubScreen(model) {
	private var isLocal by mutableStateOf(true)
	private var state: BoxState by mutableStateOf(CONTENT)
	private var searchResult by mutableStateOf(emptyList<WeiboUserInfo>())

	private suspend fun refreshLocalUser() {
		val weiboUsers = app.config.weiboUsers
		for ((index, user) in weiboUsers.withIndex()) {
			if (user.avatar.isEmpty()) {
				val data = WeiboAPI.getWeiboUser(user.id)
				if (data is Success) weiboUsers[index] = data.data.info
			}
		}
		isLocal = true
	}

	private suspend fun onSearchWeiboUser() {
		searchDialog.openSuspend()?.let { key ->
			state = LOADING
			val result = WeiboAPI.searchWeiboUser(key)
			if (result is Success) {
				val data = result.data
				searchResult = data
				state = if (data.isEmpty()) EMPTY else CONTENT
			}
			else state = NETWORK_ERROR
			isLocal = false
		}
	}

	override suspend fun initialize() {
		refreshLocalUser()
	}

	override val title: String by derivedStateOf { if (isLocal) "微博关注" else "搜索结果" }

	@Composable
	override fun ActionScope.LeftActions() {
		if (!isLocal) {
			ActionSuspend(Icons.Outlined.Close) {
				refreshLocalUser()
			}
		}
	}

	@Composable
	override fun ActionScope.RightActions() {
		ActionSuspend(Icons.Outlined.Search, "搜索") {
			onSearchWeiboUser()
		}
		if (isLocal) {
			Action(Icons.Outlined.SwapVert, "备份") {
				importSheet.open()
			}
		}
	}

	@Composable
	override fun SubContent(device: Device) {
		StatefulBox(
			state = state,
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
		) {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
				modifier = Modifier.fillMaxSize()
			) {
				items(
					items = if (isLocal) app.config.weiboUsers.items else searchResult,
					key = { it.id }
				) {
					WeiboUserItem(
						user = it,
						modifier = Modifier.fillMaxWidth(),
						onClick = { navigate(ScreenWeiboUser.Args(it.id)) }
					)
				}
			}
		}
	}

	private val importSheet = object : FloatingSheet() {
		@Composable
		override fun Content() {
			val state = rememberTextInputState()

			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				Text(text = "微博关注数据迁移")
				TextInput(
					state = state,
					hint = "关注列表(JSON格式)",
					maxLines = 6,
					clearButton = false,
					modifier = Modifier.fillMaxWidth()
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly
				) {
					LoadingRachelButton(
						text = "导入(叠加)",
						icon = Icons.Outlined.Download,
						enabled = state.ok,
						onClick = {
							try {
								val localUsers = app.config.weiboUsers
								val items = state.text.parseJsonValue<List<WeiboUserInfo>>()!!
								for (item in items) {
									if (!localUsers.contains { it.id == item.id }) localUsers += WeiboUserInfo(item.id, item.name, "")
								}
								slot.tip.success("导入成功")
							}
							catch (_: Throwable) {
								slot.tip.error("导入格式错误")
							}
						}
					)
					LoadingRachelButton(
						text = "导出",
						icon = Icons.Outlined.Upload,
						onClick = {
							try {
								state.text = app.config.weiboUsers.items.fastMap { WeiboUserInfo(it.id, it.name, "") }.toJsonString()
							}
							catch (e: Throwable) {
								slot.tip.error(e.message ?: "导出失败")
							}
						}
					)
				}
			}
		}
	}

	private val searchDialog = FloatingDialogInput(hint = "输入微博用户昵称关键字", maxLength = 16)

	@Composable
	override fun Floating() {
		importSheet.Land()
		searchDialog.Land()
	}
}
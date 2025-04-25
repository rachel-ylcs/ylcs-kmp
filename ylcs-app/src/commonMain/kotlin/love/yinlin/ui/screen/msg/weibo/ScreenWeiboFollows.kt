package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Composable
private fun WeiboUserItem(
	user: WeiboUserInfo,
	contentPadding: PaddingValues,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Row(
		modifier = modifier.clickable(onClick = onClick).padding(contentPadding),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		if (user.avatar.isEmpty()) CircularProgressIndicator(modifier = Modifier.size(28.dp))
		else WebImage(
			uri = user.avatar,
			key = DateEx.TodayString,
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = Modifier.size(32.dp)
		)
		Text(
			text = user.name,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f)
		)
	}
}

@Stable
class ScreenWeiboFollows(model: AppModel) : Screen<ScreenWeiboFollows.Args>(model) {
	@Stable
	@Serializable
	data object Args : Screen.Args

	private var isLocal by mutableStateOf(true)
	private val searchDialog = DialogInput(
		hint = "输入微博用户昵称关键字",
		maxLength = 16
	)
	private var state by mutableStateOf(BoxState.CONTENT)
	private var searchResult by mutableStateOf(emptyList<WeiboUserInfo>())

	private val importSheet = FloatingSheet()

	private suspend fun refreshLocalUser() {
		val weiboUsers = app.config.weiboUsers
		for ((index, user) in weiboUsers.withIndex()) {
			if (user.avatar.isEmpty()) {
				val data = WeiboAPI.getWeiboUser(user.id)
				if (data is Data.Success) weiboUsers[index] = data.data.info
			}
		}
		isLocal = true
	}

	private suspend fun onSearchWeiboUser() {
		searchDialog.open()?.let { key ->
			state = BoxState.LOADING
			val result = WeiboAPI.searchWeiboUser(key)
			if (result is Data.Success) {
				val data = result.data
				searchResult = data
				state = if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
			}
			else state = BoxState.NETWORK_ERROR
			isLocal = false
		}
	}

	@Composable
	private fun ImportLayout() {
		val state = remember { TextInputState() }

		Column(
			modifier = Modifier.fillMaxWidth().padding(10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(10.dp)
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
					text = "导入(保留旧数据)",
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
							state.text = app.config.weiboUsers.items.map { WeiboUserInfo(it.id, it.name, "") }.toJsonString()
						}
						catch (e: Throwable) {
							slot.tip.error(e.message ?: "导出失败")
						}
					}
				)
			}
		}
	}

	override suspend fun initialize() {
		refreshLocalUser()
	}

	@Composable
	override fun Content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = if (isLocal) "微博关注" else "搜索结果",
			onBack = { pop() },
			actions = {
				ActionSuspend(Icons.Outlined.Search) {
					onSearchWeiboUser()
				}
				if (isLocal) {
					Action(Icons.Outlined.SwapVert) {
						importSheet.open()
					}
				}
			},
			leftActions = {
				if (!isLocal) {
					ActionSuspend(Icons.Outlined.Close) {
						refreshLocalUser()
					}
				}
			},
			slot = slot
		) {
			StatefulBox(
				state = state,
				modifier = Modifier.fillMaxSize()
			) {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(300.dp),
					contentPadding = PaddingValues(5.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalArrangement = Arrangement.spacedBy(5.dp),
					modifier = Modifier.fillMaxSize()
				) {
					items(
						items = if (isLocal) app.config.weiboUsers.items else searchResult,
						key = { it.id }
					) {
						WeiboUserItem(
							user = it,
							contentPadding = PaddingValues(5.dp),
							modifier = Modifier.fillMaxWidth(),
							onClick = { navigate(ScreenWeiboUser.Args(it.id)) }
						)
					}
				}
			}
		}

		searchDialog.WithOpen()
	}

	@Composable
	override fun Floating() {
		importSheet.Land { ImportLayout() }
	}
}
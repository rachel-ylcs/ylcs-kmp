package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen

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
		if (user.avatar.isEmpty()) CircularProgressIndicator(modifier = Modifier.size(32.dp))
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

	private suspend fun refreshLocalUser() {
		isLocal = true
		val weiboUsers = app.config.weiboUsers
		for ((index, user) in weiboUsers.withIndex()) {
			if (user.avatar.isEmpty()) {
				val data = WeiboAPI.getWeiboUser(user.id)
				if (data is Data.Success) weiboUsers[index] = data.data.info
			}
		}
	}

	private suspend fun onSearchWeiboUser() {
		searchDialog.open()?.let { key ->
			state = BoxState.LOADING
			val result = WeiboAPI.searchWeiboUser(key)
			isLocal = false
			if (result is Data.Success) {
				val data = result.data
				searchResult = data
				state = if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
			}
			else state = BoxState.NETWORK_ERROR
		}
	}

	override suspend fun initialize() {
		refreshLocalUser()
	}

	@Composable
	override fun content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = if (isLocal) "微博关注" else "搜索结果",
			onBack = { pop() },
			actions = {
				Action(Icons.Outlined.Search) {
					launch { onSearchWeiboUser() }
				}
				Action(Icons.Outlined.Refresh) {
					launch { refreshLocalUser() }
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

		searchDialog.withOpen()
	}
}
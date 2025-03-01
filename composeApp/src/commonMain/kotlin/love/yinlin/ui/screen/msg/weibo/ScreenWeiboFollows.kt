package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.launch
import love.yinlin.platform.config
import love.yinlin.ui.Route
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.DialogState
import love.yinlin.ui.component.screen.SubScreen

private class WeiboFollowsModel(val model: AppModel) : ViewModel() {
	var isLocal by mutableStateOf(true)
	val searchDialog = DialogState()
	var state by mutableStateOf(BoxState.CONTENT)
	var searchResult by mutableStateOf(emptyList<WeiboUserInfo>())

	init {
		refreshLocalUser()
	}

	fun refreshLocalUser() {
		isLocal = true
		launch {
			val weiboUsers = config.weiboUsers
			for ((index, user) in weiboUsers.withIndex()) {
				if (user.avatar.isEmpty()) {
					val data = WeiboAPI.getWeiboUser(user.id)
					if (data is Data.Success) weiboUsers[index] = data.data.info
				}
			}
		}
	}

	fun onUserClick(info: WeiboUserInfo) {
		model.navigate(Route.WeiboUser(info.id))
	}

	fun openSearch() {
		searchDialog.isOpen = true
	}

	fun onSearchWeiboUser(key: String) {
		launch {
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
}

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
			key = DateEx.currentDateString,
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

@Composable
fun ScreenWeiboFollows(model: AppModel) {
	val screenModel = viewModel { WeiboFollowsModel(model) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = if (screenModel.isLocal) "微博关注" else "搜索结果",
		onBack = { model.pop() },
		actions = {
			ClickIcon(
				imageVector = Icons.Filled.Search,
				modifier = Modifier.padding(end = 5.dp),
				onClick = { screenModel.openSearch() }
			)
			ClickIcon(
				imageVector = Icons.Filled.Refresh,
				modifier = Modifier.padding(end = 5.dp),
				onClick = { screenModel.refreshLocalUser() }
			)
		}
	) {
		StatefulBox(
			state = screenModel.state,
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
					items = if (screenModel.isLocal) config.weiboUsers.items else screenModel.searchResult,
					key = { it.id }
				) {
					WeiboUserItem(
						user = it,
						contentPadding = PaddingValues(5.dp),
						modifier = Modifier.fillMaxWidth(),
						onClick = { screenModel.onUserClick(it) }
					)
				}
			}
		}
	}

	if (screenModel.searchDialog.isOpen) {
		DialogInput(
			state = screenModel.searchDialog,
			hint = "输入微博用户昵称关键字",
			maxLength = 16,
			onInput = { screenModel.onSearchWeiboUser(it) }
		)
	}
}
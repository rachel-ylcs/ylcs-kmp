package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.ui.Route
import love.yinlin.ui.component.*

class WeiboFollowsModel(val model: AppModel) : ViewModel() {
	var isLocal by mutableStateOf(true)
	val searchDialogState = DialogState()

	init {
		refreshLocalUser()
	}

	fun refreshLocalUser() {
		model.launch {
			val users = model.mainModel.msgModel.followUsers
			users.forEachIndexed { index, user ->
				if (user.avatar.isEmpty()) {
					val data = WeiboAPI.getWeiboUser(user.id)
					if (data is Data.Success) users[index] = data.data.info
				}
			}
		}
	}

	fun onUserClick(info: WeiboUserInfo) {
		model.navigate(Route.WeiboUser(info.id))
	}

	fun onUserDelete(info: WeiboUserInfo) {

	}

	fun openSearch() {
		searchDialogState.isOpen = true
	}

	fun onRefresh() {

	}
}

@Composable
private fun WeiboUserItem(
	isLocal: Boolean,
	user: WeiboUserInfo,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	onDelete: () -> Unit
) {
	Row(
		modifier = modifier.clickable(onClick = onClick),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		if (user.avatar.isEmpty()) CircularProgressIndicator(modifier = Modifier.size(32.dp))
		else WebImage(
			uri = user.avatar,
			key = DateEx.currentDateString,
			modifier = Modifier.size(32.dp)
		)
		Text(
			text = user.name,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f)
		)
		if (isLocal) ClickIcon(
			imageVector = Icons.Filled.Close,
			onClick = onDelete
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
				onClick = { screenModel.onRefresh() }
			)
		}
	) {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(300.dp),
			contentPadding = PaddingValues(5.dp),
			horizontalArrangement = Arrangement.spacedBy(5.dp),
			verticalArrangement = Arrangement.spacedBy(5.dp),
			modifier = Modifier.fillMaxSize()
		) {
			items(
				items = model.mainModel.msgModel.followUsers,
				key = { it.id }
			) {
				WeiboUserItem(
					isLocal = screenModel.isLocal,
					user = it,
					modifier = Modifier.fillMaxWidth(),
					onClick = { screenModel.onUserClick(it) },
					onDelete = { screenModel.onUserDelete(it) }
				)
			}
		}
	}

	if (screenModel.searchDialogState.isOpen) {
		DialogInfo(
			state = screenModel.searchDialogState,
			title = "选择一个",
			content = "啊啊啊"
		)
	}
}
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
import love.yinlin.ui.component.screen.DialogInputState
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
@Serializable
data object ScreenWeiboFollows : Screen<ScreenWeiboFollows.Model> {
	class Model(model: AppModel) : Screen.Model(model) {
		var isLocal by mutableStateOf(true)
		val searchDialog = object : DialogInputState(
			hint = "输入微博用户昵称关键字",
			maxLength = 16
		) {
			override fun onInput(text: String) = onSearchWeiboUser(text)
		}
		var state by mutableStateOf(BoxState.CONTENT)
		var searchResult by mutableStateOf(emptyList<WeiboUserInfo>())

		fun refreshLocalUser() {
			isLocal = true
			launch {
				val weiboUsers = app.config.weiboUsers
				for ((index, user) in weiboUsers.withIndex()) {
					if (user.avatar.isEmpty()) {
						val data = WeiboAPI.getWeiboUser(user.id)
						if (data is Data.Success) weiboUsers[index] = data.data.info
					}
				}
			}
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

	override fun model(model: AppModel): Model = Model(model).apply {
		refreshLocalUser()
	}

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = if (model.isLocal) "微博关注" else "搜索结果",
			onBack = { model.pop() },
			actions = {
				Action(
					icon = Icons.Outlined.Search,
					onClick = { model.searchDialog.open() }
				)
				Action(
					icon = Icons.Outlined.Refresh,
					onClick = { model.refreshLocalUser() }
				)
			},
			slot = model.slot
		) {
			StatefulBox(
				state = model.state,
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
						items = if (model.isLocal) app.config.weiboUsers.items else model.searchResult,
						key = { it.id }
					) {
						WeiboUserItem(
							user = it,
							contentPadding = PaddingValues(5.dp),
							modifier = Modifier.fillMaxWidth(),
							onClick = { model.navigate(ScreenWeiboUser(it.id)) }
						)
					}
				}
			}
		}

		model.searchDialog.withOpen()
	}
}
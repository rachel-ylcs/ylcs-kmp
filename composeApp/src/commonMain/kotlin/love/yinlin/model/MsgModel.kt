package love.yinlin.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.component.BoxState
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.platform.Coroutines

class MsgModel {
	open class PageState {
		var isFirstLoad: Boolean = true
		var state by mutableStateOf(BoxState.EMPTY)
	}

	inner class WeiboState : PageState() {
		val items = mutableStateListOf<Weibo>()

		suspend fun requestWeibo() {
			if (state != BoxState.LOADING) {
				state = BoxState.LOADING
				val newItems = mutableStateListOf<Weibo>()
				if (localWeiboUsers.isEmpty()) {
					state = BoxState.EMPTY
					isFirstLoad = false
				}
				else withContext(NonCancellable) {
					Coroutines.io {
						for (user in localWeiboUsers) {
							val result = WeiboAPI.getUserWeibo(user.id)
							if (result is Data.Success) newItems += result.data
						}
					}
					newItems.sortDescending()
					items += newItems
					state = if (newItems.isEmpty()) BoxState.NETWORK_ERROR else BoxState.CONTENT
					isFirstLoad = false
				}
			}
		}
	}

	inner class ChaohuaState : PageState() {

	}

	inner class PicturesState : PageState() {

	}

	// 本地微博用户
	val localWeiboUsers = app.config.weiboUsers.toMutableStateList()
	val weiboState = WeiboState()
	val chaohuaState = ChaohuaState()
	val picturesState = PicturesState()
}
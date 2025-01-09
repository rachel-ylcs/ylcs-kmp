package love.yinlin.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.ensureActive
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.component.BoxState
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.replaceAll
import love.yinlin.platform.Coroutines

class MsgModel {
	open class PageState {
		val launchFlag = LaunchFlag()
		var boxState by mutableStateOf(BoxState.EMPTY)
	}

	inner class WeiboState : PageState() {
		val items = mutableStateListOf<Weibo>()

		suspend fun requestWeibo() {
			if (boxState != BoxState.LOADING) {
				boxState = BoxState.LOADING
				boxState = if (localWeiboUsers.isEmpty()) BoxState.EMPTY
				else {
					val newItems = mutableMapOf<String, Weibo>()
					Coroutines.io {
						for (user in localWeiboUsers) {
							val result = WeiboAPI.getUserWeibo(user.id)
							if (result is Data.Success) newItems += result.data.associateBy { it.id }
							else if (result is Data.Error && result.type == RequestError.Canceled) {
								boxState = BoxState.EMPTY
								ensureActive()
							}
						}
					}
					items.replaceAll(newItems.map { it.value }.sortedDescending())
					if (newItems.isEmpty()) BoxState.NETWORK_ERROR else BoxState.CONTENT
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
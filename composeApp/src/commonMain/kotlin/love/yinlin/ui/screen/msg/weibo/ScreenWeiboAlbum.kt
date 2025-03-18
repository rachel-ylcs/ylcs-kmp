package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
@Serializable
data class ScreenWeiboAlbum(val containerId: String, val title: String) : Screen<ScreenWeiboAlbum.Model> {
	companion object {
		const val PIC_LIMIT = 24
		const val PIC_MAX_LIMIT = 1000
	}

	data class AlbumCache(val count: Int, val items: List<Picture>)

	inner class Model(model: AppModel) : Screen.Model(model) {
		var state by mutableStateOf(BoxState.EMPTY)

		val caches = MutableList<AlbumCache?>(PIC_MAX_LIMIT) { null }
		var num by mutableIntStateOf(0)
		var current by mutableIntStateOf(0)
		var maxNum = 0

		fun requestAlbum(page: Int) {
			launch {
				if (caches[page] == null) { // 无缓存
					state = BoxState.LOADING
					val result = WeiboAPI.getWeiboAlbumPics(containerId, page, PIC_LIMIT)
					if (result is Data.Success) {
						val (data, count) = result.data
						caches[page] = AlbumCache(count, data)
					}
					state = BoxState.CONTENT
				}
				val currentAlbum = caches[page]
				if (currentAlbum != null) {
					num = currentAlbum.count
					current = page
				}
				else maxNum = page - 1
			}
		}

		fun onPrevious() {
			if (state != BoxState.LOADING) {
				if (current > 1) requestAlbum(current - 1)
				else launch {
					tip.warning("已经是第一页啦")
				}
			}
		}

		fun onNext() {
			if (state != BoxState.LOADING) {
				if ((maxNum == 0 || current < maxNum) && current < PIC_MAX_LIMIT - 2) requestAlbum(current + 1)
				else launch {
					tip.warning("已经是最后一页啦")
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model).apply {
		launch {
			requestAlbum(1)
		}
	}

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "$title - 共 ${model.num} 张",
			onBack = { model.pop() },
			tip = model.tip
		) {
			Column(
				modifier = Modifier.fillMaxSize().padding(10.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				StatefulBox(
					state = model.state,
					modifier = Modifier.fillMaxWidth().weight(1f)
				) {
					val data = model.caches[model.current]
					if (data != null) LazyVerticalGrid(
						columns = GridCells.Adaptive(if (app.isPortrait) 75.dp else 120.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp),
						modifier = Modifier.fillMaxSize()
					) {
						itemsIndexed(
							items = data.items,
							key = { index, pic -> pic.image }
						){ index, pic ->
							WebImage(
								uri = pic.image,
								modifier = Modifier.fillMaxWidth().aspectRatio(1f),
								onClick = { model.navigate(ScreenImagePreview(data.items, index)) }
							)
						}
					}
				}
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
					verticalAlignment = Alignment.CenterVertically
				) {
					ClickIcon(
						imageVector = Icons.Outlined.FirstPage,
						size = 32.dp,
						onClick = { model.onPrevious() }
					)
					Text(
						text = "第 ${model.current} 页",
						style = MaterialTheme.typography.headlineSmall,
						textAlign = TextAlign.Center
					)
					ClickIcon(
						imageVector = Icons.AutoMirrored.Outlined.LastPage,
						size = 32.dp,
						onClick = { model.onNext() }
					)
				}
			}
		}
	}
}
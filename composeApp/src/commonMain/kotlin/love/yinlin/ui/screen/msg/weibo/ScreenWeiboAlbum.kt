package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.platform.app
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.extension.launchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.launch
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.screen.Tip
import love.yinlin.ui.component.screen.TipState
import love.yinlin.ui.component.image.WebImage

private data class AlbumCache(val count: Int, val items: List<Picture>)

private class WeiboAlbumModel(val album: WeiboAlbum) : ViewModel() {
	companion object {
		const val PIC_LIMIT = 24
		const val PIC_MAX_LIMIT = 1000
	}

	val tip = TipState()

	val flagFirstLoad = launchFlag()
	var state by mutableStateOf(BoxState.EMPTY)

	val caches = MutableList<AlbumCache?>(PIC_MAX_LIMIT) { null }
	var num by mutableIntStateOf(0)
	var current by mutableIntStateOf(0)
	var maxNum = 0

	fun requestAlbum(page: Int) {
		launch {
			if (caches[page] == null) { // 无缓存
				state = BoxState.LOADING
				val result = WeiboAPI.getWeiboAlbumPics(album.containerId, page, PIC_LIMIT)
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

	fun onPictureClick(pic: Picture) {
		TODO()
	}
}

@Composable
fun ScreenWeiboAlbum(model: AppModel, album: WeiboAlbum) {
	val screenModel = viewModel { WeiboAlbumModel(album) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "${album.title} - 共 ${screenModel.num} 张",
		onBack = { model.pop() }
	) {
		Column(
			modifier = Modifier.fillMaxSize().padding(10.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			StatefulBox(
				state = screenModel.state,
				modifier = Modifier.fillMaxWidth().weight(1f)
			) {
				val data = screenModel.caches[screenModel.current]
				if (data != null) LazyVerticalGrid(
					columns = GridCells.Adaptive(if (app.isPortrait) 75.dp else 120.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp),
					modifier = Modifier.fillMaxSize()
				) {
					items(
						items = data.items,
						key = { it.image }
					){
						WebImage(
							uri = it.image,
							modifier = Modifier.fillMaxWidth().aspectRatio(1f),
							onClick = { screenModel.onPictureClick(it) }
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
					imageVector = Icons.Filled.FirstPage,
					size = 32.dp,
					onClick = { screenModel.onPrevious() }
				)
				Text(
					text = "第 ${screenModel.current} 页",
					style = MaterialTheme.typography.headlineSmall,
					textAlign = TextAlign.Center
				)
				ClickIcon(
					imageVector = Icons.AutoMirrored.Filled.LastPage,
					size = 32.dp,
					onClick = { screenModel.onNext() }
				)
			}
		}
	}

	LaunchOnce(screenModel.flagFirstLoad) {
		screenModel.requestAlbum(1)
	}

	Tip(state = screenModel.tip)
}
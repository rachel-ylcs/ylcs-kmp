package love.yinlin.ui.component.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import love.yinlin.api.APIConfig
import love.yinlin.extension.itemKey
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.OS
import love.yinlin.ui.component.image.MiniIcon
import kotlin.math.abs
import kotlin.math.absoluteValue

// 分页数据实现
@Stable
abstract class Pagination<E, out T>(
	private val default: T,
	val pageNum: Int = APIConfig.MIN_PAGE_NUM
) {
	val items = mutableStateListOf<E>()
	var canLoading by mutableStateOf(false)

	private var mOffset: T = default
	abstract fun offset(item: E): T
	val offset: T get() = mOffset

	open fun processArgs(last: E?) {}

	fun newData(newItems: List<E>): Boolean {
		items.replaceAll(newItems)
		val last = newItems.lastOrNull()
		mOffset = last?.let { offset(it) } ?: default
		processArgs(last)
		canLoading = newItems.size == pageNum
		return newItems.isNotEmpty()
	}

	fun moreData(newItems: List<E>): Boolean {
		if (newItems.isEmpty()) {
			mOffset = default
			processArgs(null)
		}
		else {
			items += newItems
			val last = newItems.lastOrNull()
			mOffset = last?.let { offset(it) } ?: default
			processArgs(last)
		}
		canLoading = offset != default && newItems.size == pageNum
		return newItems.isNotEmpty()
	}
}

@Stable
abstract class PaginationArgs<E, out T, out A1>(
	default: T,
	private val default1: A1,
	pageNum: Int = APIConfig.MIN_PAGE_NUM
) : Pagination<E, T>(default, pageNum) {
	private var mArg1: A1 = default1
	abstract fun arg1(item: E): A1
	val arg1: A1 get() = mArg1

	override fun processArgs(last: E?) {
		mArg1 = last?.let { arg1(it) } ?: default1
	}
}

// 分页 UI 实现

private enum class PaginationStatus {
	IDLE, RUNNING, PULL, RELEASE
}

@Stable
private class SwipeState {
	var isReleaseEdge = false
	var refreshStatus by mutableStateOf(PaginationStatus.IDLE)
	var loadingStatus by mutableStateOf(PaginationStatus.IDLE)
	var isAnimateOver by mutableStateOf(true)
	val isRunning: Boolean get() = !isAnimateOver || refreshStatus == PaginationStatus.RUNNING || loadingStatus == PaginationStatus.RUNNING

	private val mutatorMutex = MutatorMutex()
	private val _indicatorOffset = Animatable(0f)
	val indicatorOffset: Float get() = _indicatorOffset.value

	suspend fun animateOffsetTo(offset: Float) = mutatorMutex.mutate {
		_indicatorOffset.animateTo(offset) {
			if (this.value == 0f) isAnimateOver = true
		}
	}

	suspend fun snapOffsetTo(headerHeightPx: Float, footerHeightPx: Float, offset: Float) = mutatorMutex.mutate(MutatePriority.UserInput) {
		_indicatorOffset.snapTo(offset)
		if (indicatorOffset >= headerHeightPx) refreshStatus = PaginationStatus.RELEASE
		else if (indicatorOffset <= -footerHeightPx) loadingStatus = PaginationStatus.RELEASE
		else {
			if (indicatorOffset > 0) refreshStatus = PaginationStatus.PULL
			if (indicatorOffset < 0) loadingStatus = PaginationStatus.PULL
		}
	}
}

@Composable
private fun DefaultSwipePaginationHeader(
	status: PaginationStatus,
	progress: Float
) {
	Row(
		modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = progress)),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
	) {
		if (status == PaginationStatus.RUNNING) LoadingAnimation(
			size = 32.dp,
			color = MaterialTheme.colorScheme.onPrimary
		)
		else MiniIcon(
			icon = Icons.Outlined.ArrowDownward,
			size = 32.dp,
			color = MaterialTheme.colorScheme.onPrimary
		)
		Text(
			text = when (status) {
				PaginationStatus.RUNNING -> "刷新中..."
				PaginationStatus.PULL -> "继续下拉刷新"
				PaginationStatus.RELEASE -> "释放立即刷新"
				else -> ""
			},
			color = MaterialTheme.colorScheme.onPrimary
		)
	}
}

@Composable
private fun DefaultSwipePaginationFooter(
	status: PaginationStatus,
	progress: Float
) {
	Row(
		modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = progress)),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
	) {
		if (status == PaginationStatus.RUNNING) LoadingAnimation(
			size = 32.dp,
			color = MaterialTheme.colorScheme.onPrimary
		)
		else MiniIcon(
			icon = Icons.Outlined.ArrowUpward,
			size = 32.dp,
			color = MaterialTheme.colorScheme.onPrimary
		)
		Text(
			text = when (status) {
				PaginationStatus.RUNNING -> "加载中..."
				PaginationStatus.PULL -> "上拉加载更多"
				PaginationStatus.RELEASE -> "释放立即加载"
				else -> ""
			},
			color = MaterialTheme.colorScheme.onPrimary
		)
	}
}

@Composable
private fun SwipePaginationLayout(
	canRefresh: Boolean = true,
	canLoading: Boolean = false,
	onRefresh: (suspend () -> Unit)? = null,
	onLoading: (suspend () -> Unit)? = null,
	headerHeight: Dp = 70.dp,
	header: @Composable (PaginationStatus, Float) -> Unit = { status, progress -> DefaultSwipePaginationHeader(status, progress) },
	footerHeight: Dp = 50.dp,
	footer: @Composable (PaginationStatus, Float) -> Unit = { status, progress -> DefaultSwipePaginationFooter(status, progress) },
	stickinessLevel: Float = 0.5f,
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	val state = remember { SwipeState() }
	val (headerHeightPx, footerHeightPx) = with(LocalDensity.current) {
		headerHeight.toPx() to footerHeight.toPx()
	}
	val connection by rememberDerivedState { object : NestedScrollConnection {
		private fun scroll(canConsumed: Float): Offset = if (canConsumed.absoluteValue > 0.5f) {
			scope.launch {
				state.snapOffsetTo(headerHeightPx, footerHeightPx, state.indicatorOffset + canConsumed)
			}
			Offset(0f, canConsumed / stickinessLevel)
		} else Offset.Zero

		override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
			state.isRunning -> Offset.Zero
			available.y < 0 && state.indicatorOffset > 0 -> scroll((available.y * stickinessLevel).coerceAtLeast(-state.indicatorOffset))
			available.y > 0 && state.indicatorOffset < 0 -> scroll((available.y * stickinessLevel).coerceAtMost(-state.indicatorOffset))
			else -> Offset.Zero
		}

		override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
			state.isRunning -> Offset.Zero
			available.y > 0 && canRefresh -> scroll(canConsumed =
				if (source == NestedScrollSource.SideEffect) (available.y * stickinessLevel).coerceAtMost(-state.indicatorOffset)
				else (available.y * stickinessLevel).coerceAtMost(headerHeightPx - state.indicatorOffset)
			)
			available.y < 0 && canLoading -> scroll(canConsumed =
				if (source == NestedScrollSource.SideEffect) (available.y * stickinessLevel).coerceAtLeast(-state.indicatorOffset)
				else (available.y * stickinessLevel).coerceAtLeast(-footerHeightPx - state.indicatorOffset)
			)
			else -> Offset.Zero
		}

		override suspend fun onPreFling(available: Velocity): Velocity {
			if (state.isRunning) return Velocity.Zero
			state.isReleaseEdge = state.indicatorOffset != 0f
			return if (state.indicatorOffset >= headerHeightPx && state.isReleaseEdge && state.refreshStatus != PaginationStatus.RUNNING) {
				state.isAnimateOver = false
				state.refreshStatus = PaginationStatus.RUNNING
				state.animateOffsetTo(headerHeightPx)
				onRefresh?.invoke()
				state.refreshStatus = PaginationStatus.IDLE
				state.animateOffsetTo(0f)
				available
			}
			else if (state.indicatorOffset <= -footerHeightPx && state.isReleaseEdge && state.loadingStatus != PaginationStatus.RUNNING) {
				state.isAnimateOver = false
				state.loadingStatus = PaginationStatus.RUNNING
				state.animateOffsetTo(-footerHeightPx)
				onLoading?.invoke()
				state.loadingStatus = PaginationStatus.IDLE
				state.animateOffsetTo(0f)
				available
			}
			else super.onPreFling(available)
		}

		override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
			if (state.isRunning) return Velocity.Zero
			if (state.indicatorOffset > 0 && state.refreshStatus != PaginationStatus.RUNNING) {
				state.refreshStatus = PaginationStatus.IDLE
				state.animateOffsetTo(0f)
			}
			else if (state.indicatorOffset < 0 && state.loadingStatus != PaginationStatus.RUNNING) {
				state.loadingStatus = PaginationStatus.IDLE
				state.animateOffsetTo(0f)
			}
			return super.onPostFling(consumed, available)
		}
	} }

	Box(modifier = modifier.clipToBounds().nestedScroll(connection)) {
		Box(
			modifier = Modifier.fillMaxWidth().height(headerHeight).align(Alignment.TopCenter)
				.graphicsLayer { translationY = -headerHeightPx + state.indicatorOffset }
		) {
			header(state.refreshStatus, abs(state.indicatorOffset) / headerHeightPx)
		}
		Box(
			modifier = Modifier.fillMaxSize().graphicsLayer { translationY = state.indicatorOffset },
			content = content
		)
		Box(
			modifier = Modifier.fillMaxWidth().height(footerHeight).align(Alignment.BottomCenter)
				.graphicsLayer { translationY = footerHeightPx + state.indicatorOffset }
		) {
			footer(state.loadingStatus, -state.indicatorOffset / footerHeightPx)
		}
	}
}

@Composable
private fun DefaultClickPaginationIndicator(
	status: PaginationStatus,
	onLoading: () -> Unit
) {
	Box(
		modifier = Modifier.fillMaxWidth().clickable(onClick = onLoading),
		contentAlignment = Alignment.Center
	) {
		Row(
			modifier = Modifier.padding(10.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
		) {
			if (status == PaginationStatus.RUNNING) LoadingAnimation(size = 32.dp)
			else MiniIcon(icon = Icons.Filled.Update, size = 32.dp)
			Text(text = if (status == PaginationStatus.RUNNING) "加载中..." else "加载更多")
		}
	}
}

@Composable
private fun <T> ClickPaginationColumn(
	items: List<T>,
	key: ((T) -> Any)? = null,
	state: LazyListState = rememberLazyListState(),
	canLoading: Boolean = false,
	onLoading: (suspend () -> Unit)? = null,
	indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	header: (@Composable LazyItemScope.() -> Unit)? = null,
	itemDivider: PaddingValues? = null,
	itemContent: @Composable LazyItemScope.(T) -> Unit
) {
	val scope = rememberCoroutineScope()
	var status by mutableStateOf(PaginationStatus.IDLE)
	LazyColumn(
		state = state,
		contentPadding = contentPadding,
		verticalArrangement = verticalArrangement,
		horizontalAlignment = horizontalAlignment,
		modifier = modifier
	) {
		if (header != null) {
			item(key = "Header".itemKey) {
				header()
			}
		}
		itemsIndexed(items = items, key = key?.let { { index, item -> it(item) } }) {index, item->
			if (itemDivider != null && index != 0) HorizontalDivider(modifier = Modifier.padding(itemDivider))
			itemContent(item)
		}
		if (canLoading) {
			item(key = Unit) {
				indicator(status) {
					if (status != PaginationStatus.RUNNING) {
						scope.launch {
							status = PaginationStatus.RUNNING
							onLoading?.invoke()
							status = PaginationStatus.IDLE
						}
					}
				}
			}
		}
	}
}

@Composable
private fun <T> ClickPaginationGrid(
	items: List<T>,
	key: ((T) -> Any)? = null,
	columns: GridCells,
	state: LazyGridState = rememberLazyGridState(),
	canLoading: Boolean = false,
	onLoading: (suspend () -> Unit)? = null,
	indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
	header: (@Composable LazyGridItemScope.() -> Unit)? = null,
	itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
	val scope = rememberCoroutineScope()
	var status by mutableStateOf(PaginationStatus.IDLE)
	LazyVerticalGrid(
		columns = columns,
		state = state,
		contentPadding = contentPadding,
		verticalArrangement = verticalArrangement,
		horizontalArrangement = horizontalArrangement,
		modifier = modifier
	) {
		if (header != null) {
			item(
				key = "Header".itemKey,
				span = { GridItemSpan(maxLineSpan) }
			) {
				header()
			}
		}
		items(items = items, key = key, itemContent = itemContent)
		if (canLoading) {
			item(
				key = Unit,
				span = { GridItemSpan(maxLineSpan) }
			) {
				indicator(status) {
					if (status != PaginationStatus.RUNNING) {
						scope.launch {
							status = PaginationStatus.RUNNING
							onLoading?.invoke()
							status = PaginationStatus.IDLE
						}
					}
				}
			}
		}
	}
}

@Composable
private fun <T> ClickPaginationStaggeredGrid(
	items: List<T>,
	key: ((T) -> Any)? = null,
	columns: StaggeredGridCells,
	state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
	canLoading: Boolean = false,
	onLoading: (suspend () -> Unit)? = null,
	indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalItemSpacing: Dp = 0.dp,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
	header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
	itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
	val scope = rememberCoroutineScope()
	var status by mutableStateOf(PaginationStatus.IDLE)
	LazyVerticalStaggeredGrid(
		columns = columns,
		state = state,
		contentPadding = contentPadding,
		verticalItemSpacing = verticalItemSpacing,
		horizontalArrangement = horizontalArrangement,
		modifier = modifier
	) {
		if (header != null) {
			item(
				key = "Header".itemKey,
				span = StaggeredGridItemSpan.FullLine
			) {
				header()
			}
		}
		items(items = items, key = key, itemContent = itemContent)
		if (canLoading) {
			item(
				key = Unit,
				span = StaggeredGridItemSpan.FullLine
			) {
				indicator(status) {
					if (status != PaginationStatus.RUNNING) {
						scope.launch {
							status = PaginationStatus.RUNNING
							onLoading?.invoke()
							status = PaginationStatus.IDLE
						}
					}
				}
			}
		}
	}
}

@Composable
fun <T> PaginationColumn(
	items: List<T>,
	key: ((T) -> Any)? = null,
	state: LazyListState = rememberLazyListState(),
	canRefresh: Boolean = true,
	canLoading: Boolean = false,
	onRefresh: (suspend () -> Unit)? = null,
	onLoading: (suspend () -> Unit)? = null,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	header: (@Composable LazyItemScope.() -> Unit)? = null,
	itemDivider: PaddingValues? = null,
	itemContent: @Composable LazyItemScope.(T) -> Unit
) {
	OS.runPhone(notPhone = {
		ClickPaginationColumn(
			items = items,
			key = key,
			state = state,
			canLoading = canLoading,
			onLoading = onLoading,
			modifier = modifier,
			contentPadding = contentPadding,
			verticalArrangement = verticalArrangement,
			horizontalAlignment = horizontalAlignment,
			header = header,
			itemDivider = itemDivider,
			itemContent = itemContent
		)
	}) {
		SwipePaginationLayout(
			canRefresh = canRefresh,
			canLoading = canLoading,
			onRefresh = onRefresh,
			onLoading = onLoading,
			modifier = modifier
		) {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				state = state,
				contentPadding = contentPadding,
				verticalArrangement = verticalArrangement,
				horizontalAlignment = horizontalAlignment,
			) {
				if (header != null) {
					item(key = "Header".itemKey) {
						header()
					}
				}
				itemsIndexed(items = items, key = key?.let { { index, item -> it(item) } }) {index, item->
					if (itemDivider != null && index != 0) HorizontalDivider(modifier = Modifier.padding(itemDivider))
					itemContent(item)
				}
			}
		}
	}
}

@Composable
fun <T> PaginationGrid(
	items: List<T>,
	key: ((T) -> Any)? = null,
	columns: GridCells,
	state: LazyGridState = rememberLazyGridState(),
	canRefresh: Boolean = true,
	canLoading: Boolean = false,
	onRefresh: (suspend () -> Unit)? = null,
	onLoading: (suspend () -> Unit)? = null,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
	header: (@Composable LazyGridItemScope.() -> Unit)? = null,
	itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
	OS.runPhone(
		notPhone = {
			ClickPaginationGrid(
				items = items,
				key = key,
				columns = columns,
				state = state,
				canLoading = canLoading,
				onLoading = onLoading,
				modifier = modifier,
				contentPadding = contentPadding,
				verticalArrangement = verticalArrangement,
				horizontalArrangement = horizontalArrangement,
				header = header,
				itemContent = itemContent
			)
		}
	) {
		SwipePaginationLayout(
			canRefresh = canRefresh,
			canLoading = canLoading,
			onRefresh = onRefresh,
			onLoading = onLoading,
			modifier = modifier
		) {
			LazyVerticalGrid(
				columns = columns,
				modifier = Modifier.fillMaxSize(),
				state = state,
				contentPadding = contentPadding,
				verticalArrangement = verticalArrangement,
				horizontalArrangement = horizontalArrangement,
			) {
				if (header != null) {
					item(
						key = "Header".itemKey,
						span = { GridItemSpan(maxLineSpan) }
					) {
						header()
					}
				}
				items(items = items, key = key, itemContent = itemContent)
			}
		}
	}
}

@Composable
fun <T> PaginationStaggeredGrid(
	items: List<T>,
	key: ((T) -> Any)? = null,
	columns: StaggeredGridCells,
	state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
	canRefresh: Boolean = true,
	canLoading: Boolean = false,
	onRefresh: (suspend () -> Unit)? = null,
	onLoading: (suspend () -> Unit)? = null,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	verticalItemSpacing: Dp = 0.dp,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
	header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
	itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
	OS.runPhone(
		notPhone = {
			ClickPaginationStaggeredGrid(
				items = items,
				key = key,
				columns = columns,
				state = state,
				canLoading = canLoading,
				onLoading = onLoading,
				modifier = modifier,
				contentPadding = contentPadding,
				verticalItemSpacing = verticalItemSpacing,
				horizontalArrangement = horizontalArrangement,
				header = header,
				itemContent = itemContent
			)
		}
	) {
		SwipePaginationLayout(
			canRefresh = canRefresh,
			canLoading = canLoading,
			onRefresh = onRefresh,
			onLoading = onLoading,
			modifier = modifier
		) {
			LazyVerticalStaggeredGrid(
				columns = columns,
				modifier = Modifier.fillMaxSize(),
				state = state,
				contentPadding = contentPadding,
				verticalItemSpacing = verticalItemSpacing,
				horizontalArrangement = horizontalArrangement
			) {
				if (header != null) {
					item(
						key = "Header".itemKey,
						span = StaggeredGridItemSpan.FullLine
					) {
						header()
					}
				}
				items(items = items, key = key, itemContent = itemContent)
			}
		}
	}
}
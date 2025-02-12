package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
actual fun <T> PaginationColumn(
	items: List<T>,
	key: ((T) -> Any)?,
	state: LazyListState,
	canRefresh: Boolean,
	canLoading: Boolean,
	onRefresh: (suspend () -> Unit)?,
	onLoading: (suspend () -> Unit)?,
	modifier: Modifier,
	contentPadding: PaddingValues,
	verticalArrangement: Arrangement.Vertical,
	horizontalAlignment: Alignment.Horizontal,
	itemContent: @Composable LazyItemScope.(T) -> Unit
) {
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
			items(items = items, key = key, itemContent = itemContent)
		}
	}
}

@Composable
actual fun <T> PaginationGrid(
	items: List<T>,
	key: ((T) -> Any)?,
	columns: GridCells,
	state: LazyGridState,
	canRefresh: Boolean,
	canLoading: Boolean,
	onRefresh: (suspend () -> Unit)?,
	onLoading: (suspend () -> Unit)?,
	modifier: Modifier,
	contentPadding: PaddingValues,
	verticalArrangement: Arrangement.Vertical,
	horizontalArrangement: Arrangement.Horizontal,
	itemContent: @Composable LazyGridItemScope.(T) -> Unit
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
			items(items = items, key = key, itemContent = itemContent)
		}
	}
}

@Composable
actual fun <T> PaginationStaggeredGrid(
	items: List<T>,
	key: ((T) -> Any)?,
	columns: StaggeredGridCells,
	state: LazyStaggeredGridState,
	canRefresh: Boolean,
	canLoading: Boolean,
	onRefresh: (suspend () -> Unit)?,
	onLoading: (suspend () -> Unit)?,
	modifier: Modifier,
	contentPadding: PaddingValues,
	verticalItemSpacing: Dp,
	horizontalArrangement: Arrangement.Horizontal,
	itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
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
			items(items = items, key = key, itemContent = itemContent)
		}
	}
}
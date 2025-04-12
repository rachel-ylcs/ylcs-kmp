package love.yinlin.ui.component.image

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.extension.rememberDerivedState

@Composable
private fun BannerIndicator(
	num: Int,
	current: Int,
	width: Dp,
) {
	val offsetX by animateDpAsState(targetValue = width * current)
	val height = width / 4f

	Row(
		modifier = Modifier.width(width * num).height(height)
			.clip(MaterialTheme.shapes.extraSmall)
			.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
	) {
		Box(modifier = Modifier.width(width).fillMaxHeight()
			.offset(x = offsetX).clip(MaterialTheme.shapes.extraSmall)
			.background(MaterialTheme.colorScheme.onBackground))
	}
}

@Composable
fun <T> Banner(
	pics: List<T>,
	spacing: Dp = 0.dp,
	gap: Dp = 10.dp,
	interval: Long = 0L,
	state: PagerState = rememberPagerState { pics.size },
	modifier: Modifier = Modifier,
	content: @Composable (pic: T, index: Int, scale: Float) -> Unit
) {
	val autoplay by rememberDerivedState(interval, pics) { interval > 0L && pics.size > 1 }

	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(gap)
	) {
		HorizontalPager(
			state = state,
			contentPadding = PaddingValues(horizontal = spacing),
			modifier = Modifier.fillMaxWidth()
		) {
			val scale by animateFloatAsState(targetValue = if (it == state.currentPage || spacing == 0.dp) 1f else 0.85f)
			content(pics[it], it, scale)
		}
		BannerIndicator(
			num = pics.size,
			current = state.currentPage,
			width = 20.dp
		)
	}

	if (autoplay) {
		LaunchedEffect(state.settledPage) {
			delay(interval)
			state.animateScrollToPage((state.currentPage + 1) % state.pageCount)
		}
	}
}
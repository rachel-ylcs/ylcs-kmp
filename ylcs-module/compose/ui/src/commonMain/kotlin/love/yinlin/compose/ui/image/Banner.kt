package love.yinlin.compose.ui.image

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import love.yinlin.collection.StableList
import love.yinlin.compose.*

@Composable
private fun BannerIndicator(
	num: Int,
	current: Int,
	totalWidth: Dp,
) {
	val width = totalWidth / num
	val offsetX by animateDpAsState(targetValue = width * current)

	Row(
		modifier = Modifier.width(totalWidth).height(width / 8f)
			.clip(MaterialTheme.shapes.extraLarge)
			.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
	) {
		Box(
			modifier = Modifier.width(width).fillMaxHeight()
				.offset(x = offsetX).clip(MaterialTheme.shapes.extraLarge)
				.background(MaterialTheme.colorScheme.onBackground)
		)
	}
}

@Composable
fun <T> Banner(
	pics: StableList<T>,
	interval: Long = 0L,
	gap: Float = 0.2f,
	state: PagerState = rememberPagerState { pics.size },
	modifier: Modifier = Modifier,
	content: @Composable (pic: T, index: Int, scale: Float) -> Unit
) {
	BoxWithConstraints(modifier = modifier) {
		val spacingGap = maxWidth * gap
		val totalWidth = maxWidth * 0.3f
		Column(
			modifier = Modifier.fillMaxWidth(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
		) {
			HorizontalPager(
				state = state,
				beyondViewportPageCount = 1,
				contentPadding = PaddingValues(horizontal = spacingGap),
				modifier = Modifier.fillMaxWidth()
			) {
				if (it in pics.indices) {
					val scale by animateFloatAsState(targetValue = if (it == state.currentPage) 1f else 0.85f)
					content(pics[it], it, scale)
				}
			}
			if (pics.isNotEmpty()) {
				BannerIndicator(
					num = pics.size,
					current = state.currentPage,
					totalWidth = totalWidth
				)
			}
		}
	}

	val autoplay by rememberDerivedState(interval, pics) { interval > 0L && pics.size > 1 }

	if (autoplay && rememberOffScreenState()) {
		LaunchedEffect(state.settledPage) {
			delay(interval)
			if (pics.isNotEmpty() && state.pageCount != 0) {
				state.animateScrollToPage((state.currentPage + 1) % state.pageCount)
			}
		}
	}
}
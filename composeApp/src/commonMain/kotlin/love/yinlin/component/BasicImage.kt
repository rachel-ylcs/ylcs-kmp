package love.yinlin.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.PainterState
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import love.yinlin.Colors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.placeholder_pic

@Composable
fun MiniIcon(
	imageVector: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = 32.dp,
	modifier: Modifier = Modifier
) {
	if (imageVector != null) {
		Icon(
			modifier = modifier.size(size),
			imageVector = imageVector,
			contentDescription = null,
			tint = color,
		)
	}
	else {
		Spacer(modifier = modifier.size(size))
	}
}

@Composable
fun ClickIcon(
	imageVector: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = 32.dp,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	Icon(
		modifier = modifier.size(size).clickable(onClick = onClick),
		imageVector = imageVector,
		contentDescription = null,
		tint = color,
	)
}

@Composable
fun ClickIconRow(
	items: List<Pair<ImageVector, () -> Unit>>,
	space: Dp = 10.dp,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = 32.dp,
	modifier: Modifier = Modifier,
) {
	Row(
		modifier = modifier.padding(horizontal = 10.dp),
		horizontalArrangement = Arrangement.spacedBy(space)
	) {
		for (item in items) {
			Icon(
				modifier = Modifier.size(size).clickable(onClick = item.second),
				imageVector = item.first,
				contentDescription = null,
				tint = color,
			)
		}
	}
}

@Composable
fun MiniImage(
	res: DrawableResource,
	size: Dp = 32.dp,
	modifier: Modifier = Modifier,
) {
	Image(
		modifier = modifier.size(size),
		painter = painterResource(res),
		contentDescription = null
	)
}

enum class WebImageQuality {
	Low, Medium, High
}

@Composable
private fun Modifier.shimmerLoading(
	isActive: Boolean = true,
	durationMillis: Int = 3000
): Modifier = composed {
	if (isActive) {
		val transition = rememberInfiniteTransition()
		val offsetX = transition.animateFloat(
			initialValue = 0f,
			targetValue = durationMillis + 500f,
			animationSpec = infiniteRepeatable(
				animation = tween(durationMillis = durationMillis),
				repeatMode = RepeatMode.Restart
			)
		)
		background(brush = Brush.linearGradient(
			colors = listOf(Colors.Gray2, Colors.Gray3, Colors.Gray2),
			start = Offset(x = offsetX.value - 500, y = 0f),
			end = Offset(x = offsetX.value, y = 0f)
		))
	}
	else background(Colors.White)
}

@Composable
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember(key) {
	if (key == null) uri
	else if (uri.contains("?")) "$uri&_cacheKey=$key"
	else "$uri?_cacheKey=$key"
}

@Composable
fun WebImage(
	uri: String,
	key: Any? = null,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	quality: WebImageQuality = WebImageQuality.Medium,
	contentScale: ContentScale = ContentScale.FillWidth,
	placeholder: DrawableResource = Res.drawable.placeholder_pic
) {
	val state = rememberAsyncImageState(ComposableImageOptions {
		downloadCachePolicy(CachePolicy.ENABLED)
		memoryCachePolicy(CachePolicy.ENABLED)
		sizeMultiplier(when (quality) {
			WebImageQuality.Low -> 1f
			WebImageQuality.Medium -> 2f
			WebImageQuality.High -> 4f
		})
		placeholder(rememberIconPainterStateImage(placeholder))
		crossfade()
	})
	AsyncImage(
		uri = rememberWebImageKeyUrl(uri, key),
		contentDescription = null,
		state = state,
		contentScale = contentScale,
		modifier = modifier.let { if (circle) it.clip(CircleShape) else it }
			.shimmerLoading(state.painterState is PainterState.Loading),
		filterQuality = when (quality) {
			WebImageQuality.Low -> FilterQuality.Low
			WebImageQuality.Medium -> FilterQuality.Medium
			WebImageQuality.High -> FilterQuality.High
		}
	)
}
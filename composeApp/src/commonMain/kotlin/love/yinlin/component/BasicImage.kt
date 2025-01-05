package love.yinlin.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.request.LoadState
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import com.github.panpf.sketch.state.rememberPainterStateImage
import kotlinx.io.files.Path
import love.yinlin.Colors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.img_logo

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

@Composable
private fun Modifier.shimmerLoading(
	isActive: Boolean = true,
	durationMillis: Int = 1500
): Modifier = if (!isActive) this else composed {
	val transition = rememberInfiniteTransition()
	val offsetX = transition.animateFloat(
		initialValue = 0f,
		targetValue = durationMillis + 500f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = durationMillis),
			repeatMode = RepeatMode.Restart
		)
	)
	background(
		brush = Brush.linearGradient(
			colors = listOf(Colors.Gray2, Colors.Gray3, Colors.Gray2),
			start = Offset(x = offsetX.value - 500, y = 0f),
			end = Offset(x = offsetX.value, y = 0f)
		)
	)
}

@Composable
fun WebImage(
	uri: String,
	key: Any? = null,
	modifier: Modifier = Modifier,
) {
	val state = rememberAsyncImageState(ComposableImageOptions {
		downloadCachePolicy(CachePolicy.ENABLED)
		memoryCachePolicy(CachePolicy.ENABLED)
		placeholder(rememberIconPainterStateImage(Res.drawable.img_logo))
		crossfade()
		resizeOnDraw()
	})
	AsyncImage(
		modifier = modifier.shimmerLoading(state.loadState !is LoadState.Success),
		uri = uri + if (key != null) "?cacheKey=${key}" else "",
		state = state,
		contentScale = ContentScale.FillWidth,
		contentDescription = null
	)
}

@Composable
fun WebImage(
	path: Path
) {
	AsyncImage(
		uri = newFileUri(path.toString()),
		state = rememberAsyncImageState(ComposableImageOptions {
			downloadCachePolicy(CachePolicy.DISABLED)
		}),
		contentDescription = null
	)
}
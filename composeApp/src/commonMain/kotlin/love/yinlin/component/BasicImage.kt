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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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

val DEFAULT_ICON_SIZE = 28.dp

@Composable
fun MiniIcon(
	imageVector: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = DEFAULT_ICON_SIZE,
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
	size: Dp = DEFAULT_ICON_SIZE,
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
fun MiniImage(
	res: DrawableResource,
	size: Dp = DEFAULT_ICON_SIZE,
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
private fun Modifier.circleImage(circle: Boolean): Modifier = composed {
	if (circle) clip(CircleShape) else this
}

@Composable
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = rememberSaveable(key) {
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
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
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
		alignment = alignment,
		contentScale = contentScale,
		modifier = modifier.circleImage(circle).background(MaterialTheme.colorScheme.surface),
		filterQuality = when (quality) {
			WebImageQuality.Low -> FilterQuality.Low
			WebImageQuality.Medium -> FilterQuality.Medium
			WebImageQuality.High -> FilterQuality.High
		}
	)
}
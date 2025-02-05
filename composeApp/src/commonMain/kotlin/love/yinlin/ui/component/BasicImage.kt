package love.yinlin.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import love.yinlin.extension.condition
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
	indication: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	Icon(
		modifier = modifier.size(size).condition(value = indication,
			ifTrue = { clickable(onClick = onClick) },
			ifFalse = {
				clickable(
					onClick = onClick,
					indication = null,
					interactionSource = remember { MutableInteractionSource() }
				)
			}
		),
		imageVector = imageVector,
		contentDescription = null,
		tint = color,
	)
}

@Composable
fun ClickIcon(
	imageVector: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	indication: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	Icon(
		modifier = modifier.condition(value = indication,
			ifTrue = { clickable(onClick = onClick) },
			ifFalse = {
				clickable(
					onClick = onClick,
					indication = null,
					interactionSource = remember { MutableInteractionSource() }
				)
			}
		),
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
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember (key) {
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
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic,
	onClick: (() -> Unit)? = null,
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
		filterQuality = when (quality) {
			WebImageQuality.Low -> FilterQuality.Low
			WebImageQuality.Medium -> FilterQuality.Medium
			WebImageQuality.High -> FilterQuality.High
		},
		alpha = alpha,
		modifier = modifier.condition(circle, { clip(CircleShape) })
			.condition(onClick != null, { clickable(onClick = onClick ?: {}) })
	)
}
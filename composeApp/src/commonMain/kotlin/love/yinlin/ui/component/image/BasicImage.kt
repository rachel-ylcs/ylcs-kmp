package love.yinlin.ui.component.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.SketchZoomState
import com.github.panpf.zoomimage.rememberSketchZoomState
import love.yinlin.Colors
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
fun NoImage(
	width: Dp = DEFAULT_ICON_SIZE,
	height: Dp = DEFAULT_ICON_SIZE,
	color: Color = MaterialTheme.colorScheme.onSurface
) {
	if (width == height) {
		Box(
			modifier = Modifier.shadow(2.dp, CircleShape),
			contentAlignment = Alignment.Center
		) {
			Icon(
				modifier = Modifier.size(width),
				imageVector = Icons.AutoMirrored.Filled.Help,
				contentDescription = null,
				tint = color,
			)
		}
	}
	else {
		Box(
			modifier = Modifier.width(width).height(height).shadow(2.dp),
			contentAlignment = Alignment.Center
		) {
			Icon(
				modifier = Modifier.size(min(width, height)).padding(5.dp),
				imageVector = Icons.AutoMirrored.Filled.Help,
				contentDescription = null,
				tint = color,
			)
		}
	}
}

@Stable
data class ColorfulImageVector(
	val icon: ImageVector,
	val color: Color,
	val background: Color
)

@Stable
@Composable
fun colorfulImageVector(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	background: Color = Colors.Transparent
) = ColorfulImageVector(icon, color, background)

@Composable
fun ColorfulIcon(
	imageVector: ColorfulImageVector,
	size: Dp = DEFAULT_ICON_SIZE
) {
	Box(
		modifier = Modifier.size(size).clip(CircleShape).background(imageVector.background).padding(4.dp),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.fillMaxSize(),
			imageVector = imageVector.icon,
			contentDescription = null,
			tint = imageVector.color,
		)
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
		modifier = modifier.size(size).clip(CircleShape).condition(value = indication,
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
	Icon(
		modifier = modifier.clip(CircleShape).condition(value = indication,
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
	Low, Medium, High;

	val sizeMultiplier: Float get() = when (this) {
		Low -> 1f
		Medium -> 2f
		High -> 4f
	}

	val filterQuality: FilterQuality get() = when (this) {
		Low -> FilterQuality.Low
		Medium -> FilterQuality.Medium
		High -> FilterQuality.High
	}
}

@Composable
private fun rememberWebImageKeyUrl(
	uri: String,
	key: Any? = null
): String = rememberSaveable(uri, key) {
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
	onClick: (() -> Unit)? = null
) {
	val state = rememberAsyncImageState(ComposableImageOptions {
		downloadCachePolicy(CachePolicy.ENABLED)
		memoryCachePolicy(CachePolicy.ENABLED)
		sizeMultiplier(quality.sizeMultiplier)
		placeholder(rememberIconPainterStateImage(placeholder))
		crossfade()
	})
	AsyncImage(
		uri = rememberWebImageKeyUrl(uri, key),
		contentDescription = null,
		state = state,
		alignment = alignment,
		contentScale = contentScale,
		filterQuality = quality.filterQuality,
		alpha = alpha,
		modifier = modifier.condition(circle) { clip(CircleShape) }
			.condition(onClick != null) { clickable(onClick = onClick ?: {}) }
	)
}

@Composable
fun ZoomWebImage(
	uri: String,
	key: Any? = null,
	zoomState: SketchZoomState = rememberSketchZoomState(),
	modifier: Modifier = Modifier,
	quality: WebImageQuality = WebImageQuality.Medium,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic
) {
	val state = rememberAsyncImageState(ComposableImageOptions {
		downloadCachePolicy(CachePolicy.ENABLED)
		memoryCachePolicy(CachePolicy.ENABLED)
		sizeMultiplier(quality.sizeMultiplier)
		placeholder(rememberIconPainterStateImage(placeholder))
	})
	SketchZoomAsyncImage(
		uri = rememberWebImageKeyUrl(uri, key),
		contentDescription = null,
		state = state,
		zoomState = zoomState,
		alignment = alignment,
		contentScale = contentScale,
		filterQuality = quality.filterQuality,
		alpha = alpha,
		scrollBar = null,
		modifier = modifier
	)
}
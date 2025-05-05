package love.yinlin.ui.component.image

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.github.panpf.sketch.*
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.SketchZoomState
import com.github.panpf.zoomimage.rememberSketchZoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.extension.condition
import love.yinlin.extension.rememberState
import love.yinlin.platform.ImageQuality
import love.yinlin.resources.Res
import love.yinlin.resources.placeholder_pic
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MiniIcon(
	icon: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		if (icon != null) {
			Icon(
				modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
				imageVector = icon,
				contentDescription = null,
				tint = color,
			)
		}
	}
}

@Composable
fun NoImage(
	width: Dp = ThemeValue.Size.Icon,
	height: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface
) = MiniIcon(
	icon = Icons.AutoMirrored.Filled.Help,
	color = color,
	size = min(width, height),
	modifier = Modifier.size(width = width, height = height)
)

@Stable
data class ColorfulImageVector(
	val icon: ImageVector,
	val color: Color,
	val background: Color
)

@Composable
fun colorfulImageVector(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	background: Color = Colors.Transparent
) = ColorfulImageVector(icon, color, background)

@Composable
fun ColorfulIcon(
	icon: ColorfulImageVector,
	size: Dp = ThemeValue.Size.Icon
) = MiniIcon(
	icon = icon.icon,
	color = icon.color.copy(alpha = 0.8f),
	size = size,
	modifier = Modifier.clip(CircleShape).background(icon.background.copy(alpha = 0.6f))
)

@Composable
fun ClickIcon(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = ThemeValue.Size.Icon,
	indication: Boolean = true,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniIcon(
	icon = icon,
	color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
	size = size,
	modifier = modifier
		.clip(MaterialTheme.shapes.extraSmall)
		.clickable(
			onClick = onClick,
			indication = if (indication) LocalIndication.current else null,
			interactionSource = remember { MutableInteractionSource() },
			enabled = enabled
		)
)

@Composable
fun LoadingCircle(
	modifier: Modifier = Modifier,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
) {
	Box(modifier = modifier) {
		CircularProgressIndicator(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
			color = color
		)
	}
}

@Composable
fun StaticLoadingIcon(
	isLoading: Boolean,
	icon: ImageVector,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	if (isLoading) {
		LoadingCircle(
			size = size,
			color = color,
			modifier = modifier
		)
	}
	else {
		MiniIcon(
			icon = icon,
			color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
			size = size,
			modifier = modifier
		)
	}
}

@Composable
fun LoadingIcon(
	icon: ImageVector,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberState { false }

	if (isLoading) {
		LoadingCircle(
			size = size,
			color = color,
			modifier = modifier
		)
	}
	else {
		ClickIcon(
			icon = icon,
			color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
			size = size,
			enabled = enabled && !isLoading,
			onClick = {
				scope.launch {
					isLoading = true
					onClick()
					isLoading = false
				}
			},
			modifier = modifier
		)
	}
}

@Composable
fun MiniIcon(
	res: DrawableResource,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
			painter = painterResource(res),
			contentDescription = null
		)
	}
}

@Composable
fun ClickIcon(
	res: DrawableResource,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniIcon(
	res = res,
	size = size,
	modifier = modifier
		.clip(MaterialTheme.shapes.extraSmall)
		.clickable(onClick = onClick)
)

@Composable
fun MiniImage(
	res: DrawableResource,
	contentScale: ContentScale = ContentScale.Fit,
	alpha: Float = 1f,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			modifier = modifier,
			contentScale = contentScale,
			alpha = alpha,
			painter = painterResource(res),
			contentDescription = null
		)
	}
}

@Composable
fun ClickImage(
	res: DrawableResource,
	contentScale: ContentScale = ContentScale.Fit,
	alpha: Float = 1f,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniImage(
	res = res,
	contentScale = contentScale,
	alpha = alpha,
	modifier = modifier.clickable(onClick = onClick)
)

@Composable
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember(uri, key) {
	if (key == null) uri
	else if (uri.contains("?")) "$uri&_cacheKey=$key"
	else "$uri?_cacheKey=$key"
}

@Composable
private fun rememberLocalFileImageKeyUrl(path: Path): String = remember(path) {
	"$path?_cacheKey=${SystemFileSystem.metadataOrNull(path)?.size}"
}

@Composable
fun rememberWebImageState(
	quality: ImageQuality,
	placeholder: DrawableResource? = null,
	isCrossfade: Boolean = true
): AsyncImageState {
	val context = LocalPlatformContext.current
	val holder = placeholder?.let { rememberIconPainterStateImage(it) }
	val options = remember(quality, holder, isCrossfade) {
		ImageOptions.Builder().apply {
			sizeMultiplier(quality.sizeMultiplier)
			placeholder(holder)
			if (isCrossfade) crossfade()
		}.merge(SingletonSketch.get(context).globalImageOptions).build()
	}
	return rememberAsyncImageState(options)
}

@Composable
fun WebImage(
	uri: String,
	key: Any? = null,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	quality: ImageQuality = ImageQuality.Medium,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic,
	state: AsyncImageState = rememberWebImageState(quality, placeholder, true),
	onClick: (() -> Unit)? = null
) {
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
fun LocalFileImage(
	path: Path,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	quality: ImageQuality = ImageQuality.Medium,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	state: AsyncImageState = rememberWebImageState(quality, null, true),
	onClick: (() -> Unit)? = null
) {
	AsyncImage(
		uri = rememberLocalFileImageKeyUrl(path),
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
	quality: ImageQuality = ImageQuality.High,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic,
	state: AsyncImageState = rememberWebImageState(quality, placeholder, false)
) {
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